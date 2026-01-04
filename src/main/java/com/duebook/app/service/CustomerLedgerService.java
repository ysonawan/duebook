package com.duebook.app.service;

import com.duebook.app.dto.CustomerLedgerDTO;
import com.duebook.app.dto.CustomerDTO;
import com.duebook.app.dto.LedgerSummaryDTO;
import com.duebook.app.dto.UserDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.*;
import com.duebook.app.repository.CustomerLedgerRepository;
import com.duebook.app.repository.CustomerRepository;
import com.duebook.app.repository.ShopRepository;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.repository.ShopUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerLedgerService {

    private final CustomerLedgerRepository ledgerRepository;
    private final CustomerRepository customerRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopUserRepository shopUserRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Get all ledger entries for the authenticated user
     */
    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getAllLedgerEntriesForUser(Long userId) {
        return ledgerRepository.findAllByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific ledger entry for the authenticated user
     */
    @Transactional(readOnly = true)
    public CustomerLedgerDTO getLedgerEntryById(Long ledgerId, Long userId) {
        CustomerLedger entry = ledgerRepository.findByIdAndUserId(ledgerId, userId)
                .orElseThrow(() -> new ApplicationException("Ledger entry not found or you don't have access to it", "LEDGER_NOT_FOUND"));
        return convertToDTO(entry);
    }

    /**
     * Create a new ledger entry
     */
    @Transactional
    public CustomerLedgerDTO createLedgerEntry(CustomerLedgerDTO ledgerDTO, Long userId) {
        // Verify customer exists and get it
        Customer customer = customerRepository.findById(ledgerDTO.getCustomerId())
                .orElseThrow(() -> new ApplicationException("Customer not found", "CUSTOMER_NOT_FOUND"));

        Shop shop = customer.getShop();
        if (!isOwnerOrStaff(shop.getId(), userId)) {
            throw new ApplicationException("You don't have permission to create ledger entries", "FORBIDDEN");
        }

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

        // Create the ledger entry
        CustomerLedger ledger = new CustomerLedger();
        ledger.setCustomer(customer);
        ledger.setShop(shop);
        ledger.setCreatedByUser(user);
        ledger.setEntryType(ledgerDTO.getEntryType());
        ledger.setAmount(ledgerDTO.getAmount());
        ledger.setBalanceAfter(ledgerDTO.getBalanceAfter());
        ledger.setNotes(ledgerDTO.getNotes());
        ledger.setEntryDate(ledgerDTO.getEntryDate() != null ? ledgerDTO.getEntryDate() : LocalDate.now());
        ledger.setCreatedAt(LocalDateTime.now());

        // Update customer's current balance
        updateCustomerBalance(customer, ledger);

        CustomerLedger savedLedger = ledgerRepository.save(ledger);

        // Audit log: Ledger entry created
        logAudit(shop.getId(), AuditAction.LEDGER.name(), savedLedger.getId(), AuditAction.LEDGER_ENTRY_CREATED, userId, null, convertToDTO(savedLedger));

        return convertToDTO(savedLedger);
    }

    /**
     * Reverse a ledger entry
     */
    @Transactional
    public CustomerLedgerDTO reverseLedgerEntry(Long ledgerId, Long userId, String notes) {
        // Verify entry exists and user has access
        CustomerLedger originalEntry = ledgerRepository.findByIdAndUserId(ledgerId, userId)
                .orElseThrow(() -> new ApplicationException("Ledger entry not found or you don't have access to it", "LEDGER_NOT_FOUND"));

        // Verify user is OWNER or STAFF
        Shop shop = originalEntry.getShop();
        if (!isOwnerOrStaff(shop.getId(), userId)) {
            throw new ApplicationException("You don't have permission to reverse ledger entries", "FORBIDDEN");
        }

        // Cannot reverse a reversal entry
        if (originalEntry.getEntryType() == CustomerLedger.LedgerEntryType.REVERSAL) {
            throw new ApplicationException("Cannot reverse a reversal entry", "INVALID_REVERSAL");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

        // Create reversal entry
        CustomerLedger reversalEntry = new CustomerLedger();
        reversalEntry.setCustomer(originalEntry.getCustomer());
        reversalEntry.setShop(originalEntry.getShop());
        reversalEntry.setCreatedByUser(user);
        reversalEntry.setEntryType(CustomerLedger.LedgerEntryType.REVERSAL);
        reversalEntry.setAmount(originalEntry.getAmount());
        reversalEntry.setReferenceEntry(originalEntry);
        reversalEntry.setNotes(notes != null ? notes : "Reversal of entry #" + originalEntry.getId());
        reversalEntry.setEntryDate(LocalDate.now());
        reversalEntry.setCreatedAt(LocalDateTime.now());

        // Calculate new balance (reverse the original entry)
        Customer customer = originalEntry.getCustomer();
        Double newBalance = customer.getCurrentBalance();

        if (originalEntry.getEntryType() == CustomerLedger.LedgerEntryType.BAKI) {
            // Reverse BAKI (decrease balance)
            newBalance = newBalance - originalEntry.getAmount();
        } else if (originalEntry.getEntryType() == CustomerLedger.LedgerEntryType.PAID) {
            // Reverse PAID (increase balance)
            newBalance = newBalance + originalEntry.getAmount();
        }

        reversalEntry.setBalanceAfter(newBalance);
        customer.setCurrentBalance(newBalance);
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
        CustomerLedger savedReversal = ledgerRepository.save(reversalEntry);

        // Audit log: Ledger reversal
        logAudit(originalEntry.getShop().getId(), AuditAction.LEDGER.name(), savedReversal.getId(), AuditAction.LEDGER_REVERSAL, userId, convertToDTO(originalEntry), convertToDTO(savedReversal));

        return convertToDTO(savedReversal);
    }

    /**
     * Get ledger entries for a specific customer
     */
    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getLedgerByCustomer(Long customerId, Long userId) {
        // Verify customer exists and user has access
        customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new ApplicationException("Customer not found or you don't have access to it", "CUSTOMER_NOT_FOUND"));

        return ledgerRepository.findByCustomerId(customerId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get ledger entries for a specific shop
     */
    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getLedgerByShop(Long shopId, Long userId) {
        // Verify shop exists and user has access
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it", "SHOP_NOT_FOUND"));

        return ledgerRepository.findByShopId(shopId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get ledger entries within a date range
     */
    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getLedgerByDateRange(LocalDate startDate, LocalDate endDate) {
        return ledgerRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get ledger summary with all filters applied (without pagination)
     * Used for summary cards that need complete data across all pages
     */
    @Transactional(readOnly = true)
    public LedgerSummaryDTO getLedgerSummary(Long userId, Long shopId, Long customerId, String entryType,
                                            LocalDate startDate, LocalDate endDate) {
        List<CustomerLedger> filteredEntries;

        if (shopId != null && shopId > 0) {
            // Get ledger entries for specific shop
            filteredEntries = ledgerRepository.findByShopId(shopId);
        } else {
            // Get all ledger entries for user
            filteredEntries = ledgerRepository.findAllByUserId(userId);
        }

        // Apply customer filter
        if (customerId != null && customerId > 0) {
            Long finalCustomerId = customerId;
            filteredEntries = filteredEntries.stream()
                    .filter(e -> e.getCustomer().getId().equals(finalCustomerId))
                    .collect(Collectors.toList());
        }

        // Apply entry type filter
        if (entryType != null && !entryType.isEmpty()) {
            try {
                CustomerLedger.LedgerEntryType typeEnum = CustomerLedger.LedgerEntryType.valueOf(entryType.toUpperCase());
                filteredEntries = filteredEntries.stream()
                        .filter(e -> e.getEntryType() == typeEnum)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid entry type filter: {}", entryType);
            }
        }

        // Apply date range filter
        if (startDate != null && endDate != null) {
            filteredEntries = filteredEntries.stream()
                    .filter(e -> e.getEntryDate() != null &&
                            !e.getEntryDate().isBefore(startDate) &&
                            !e.getEntryDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        // Get all entry IDs that have been reversed
        java.util.Set<Long> reversedEntryIds = filteredEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.REVERSAL)
                .map(e -> e.getReferenceEntry() != null ? e.getReferenceEntry().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // Filter out reversed entries and reversals themselves
        List<CustomerLedger> effectiveEntries = filteredEntries.stream()
                .filter(e -> !reversedEntryIds.contains(e.getId()) && e.getEntryType() != CustomerLedger.LedgerEntryType.REVERSAL)
                .collect(Collectors.toList());

        // Calculate summary
        double totalDebit = effectiveEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.BAKI)
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double totalCredit = effectiveEntries.stream()
                .filter(e -> e.getEntryType() == CustomerLedger.LedgerEntryType.PAID)
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double netBalance = totalDebit - totalCredit;
        long totalEntries = effectiveEntries.size();

        return LedgerSummaryDTO.builder()
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .netBalance(netBalance)
                .totalEntries(totalEntries)
                .build();
    }


    /**
     * Update customer's current balance based on ledger entry
     * Tracks balance adjustments for audit purposes
     */
    private void updateCustomerBalance(Customer customer, CustomerLedger ledger) {
        Double oldBalance = customer.getCurrentBalance();
        Double currentBalance = oldBalance;

        if (ledger.getEntryType() == CustomerLedger.LedgerEntryType.BAKI) {
            // BAKI increases balance (customer owes more)
            currentBalance = currentBalance + ledger.getAmount();
        } else if (ledger.getEntryType() == CustomerLedger.LedgerEntryType.PAID) {
            // PAID decreases balance (customer pays back)
            currentBalance = currentBalance - ledger.getAmount();
        }

        ledger.setBalanceAfter(currentBalance);
        customer.setCurrentBalance(currentBalance);
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);

        // Audit log: Customer balance adjusted
        Map<String, Object> oldBalanceMap = new HashMap<>();
        oldBalanceMap.put("balance", oldBalance);
        Map<String, Object> newBalanceMap = new HashMap<>();
        newBalanceMap.put("balance", currentBalance);
        newBalanceMap.put("amount", ledger.getAmount());
        newBalanceMap.put("type", ledger.getEntryType().name());
        logAudit(customer.getShop().getId(), AuditAction.CUSTOMER.name(), customer.getId(), AuditAction.LEDGER_BALANCE_ADJUSTED, ledger.getCreatedByUser().getId(), oldBalanceMap, newBalanceMap);
    }

    public Page<CustomerLedgerDTO> getCustomerLedgerDTOs(Page<CustomerLedger> ledgerEntries) {
       return ledgerEntries.map(this::convertToDTO);
    }

    /**
     * Convert CustomerLedger entity to CustomerLedgerDTO
     */
    private CustomerLedgerDTO convertToDTO(CustomerLedger ledger) {
        CustomerLedgerDTO dto = new CustomerLedgerDTO();
        dto.setId(ledger.getId());
        dto.setCustomerId(ledger.getCustomer().getId());
        dto.setShopId(ledger.getShop().getId());
        dto.setEntryType(ledger.getEntryType());
        dto.setAmount(ledger.getAmount());
        dto.setBalanceAfter(ledger.getBalanceAfter());
        dto.setReferenceEntryId(ledger.getReferenceEntry() != null ? ledger.getReferenceEntry().getId() : null);
        dto.setNotes(ledger.getNotes());
        dto.setEntryDate(ledger.getEntryDate());
        dto.setCreatedAt(ledger.getCreatedAt());

        // Set customer info if available
        if (ledger.getCustomer() != null) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setId(ledger.getCustomer().getId());
            customerDTO.setName(ledger.getCustomer().getName());
            customerDTO.setPhone(ledger.getCustomer().getPhone());
            dto.setCustomer(customerDTO);
        }

        // Set created by user info if available
        if (ledger.getCreatedByUser() != null) {
            UserDTO userDTO = new UserDTO(
                ledger.getCreatedByUser().getId(),
                ledger.getCreatedByUser().getName(),
                ledger.getCreatedByUser().getEmail()
            );
            dto.setCreatedByUser(userDTO);
        }

        return dto;
    }

    /**
     * Helper: Check if user is OWNER or STAFF for the shop
     */
    private boolean isOwnerOrStaff(Long shopId, Long userId) {
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
            .map(su -> su.getRole() == ShopUser.ShopUserRole.OWNER || su.getRole() == ShopUser.ShopUserRole.STAFF)
            .orElse(false);
    }

    /**
     * Log audit for ledger and customer operations
     */
    private void logAudit(Long shopId, String entityType, Long entityId, AuditAction action, Long userId, Object oldValue, Object newValue) {
        try {
            String oldVal = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newVal = newValue != null ? objectMapper.writeValueAsString(newValue) : null;
            auditService.logAuditLongId(shopId, entityType, entityId, action, userId, oldVal, newVal);
        } catch (Exception e) {
            log.error("Error logging audit for " + entityType + " operation: " + action.name(), e);
        }
    }
}
