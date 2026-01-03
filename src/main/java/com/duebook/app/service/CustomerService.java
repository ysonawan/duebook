package com.duebook.app.service;

import com.duebook.app.dto.CustomerDTO;
import com.duebook.app.dto.CustomerLedgerDTO;
import com.duebook.app.dto.UserDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.*;
import com.duebook.app.repository.CustomerRepository;
import com.duebook.app.repository.CustomerLedgerRepository;
import com.duebook.app.repository.ShopRepository;
import com.duebook.app.repository.ShopUserRepository;
import com.duebook.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ShopRepository shopRepository;
    private final ShopUserRepository shopUserRepository;
    private final UserRepository userRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Get all customers for the authenticated user
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomersForUser(Long userId) {
        return customerRepository.findAllByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific customer for the authenticated user
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long customerId, Long userId) {
        Customer customer = customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new ApplicationException("Customer not found or you don't have access to it", "CUSTOMER_NOT_FOUND"));
        return convertToDTO(customer);
    }

    /**
     * Create a new customer
     */
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO, Long userId) {
        Shop shop = shopRepository.findById(customerDTO.getShopId())
                .orElseThrow(() -> new ApplicationException("Shop not found", "SHOP_NOT_FOUND"));
        if (!isOwnerOrStaff(shop.getId(), userId)) {
            throw new ApplicationException("Only OWNER or STAFF can create customers", "FORBIDDEN");
        }

        // Validate phone uniqueness within shop
        if (customerRepository.existsByShopIdAndPhone(shop.getId(), customerDTO.getPhone())) {
            throw new ApplicationException("A customer with this phone number already exists in this shop", "PHONE_ALREADY_EXISTS");
        }

        // Create the customer
        Customer customer = new Customer();
        customer.setShop(shop);
        customer.setName(customerDTO.getName().trim());
        customer.setEntityName(customerDTO.getEntityName().trim());
        customer.setPhone(customerDTO.getPhone().trim());
        customer.setOpeningBalance(customerDTO.getOpeningBalance());
        customer.setCurrentBalance(customerDTO.getOpeningBalance());
        customer.setIsActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);

        // If opening balance is greater than 0, create a ledger entry
        if (customerDTO.getOpeningBalance() != null && customerDTO.getOpeningBalance() > 0) {
            createOpeningBalanceLedgerEntry(savedCustomer, userId);
        }

        // Audit log: Customer created
        try {
            String newValue = objectMapper.writeValueAsString(convertToDTO(savedCustomer));
            auditService.logAuditLongId(shop.getId(), AuditAction.CUSTOMER.name(), savedCustomer.getId(), AuditAction.CUSTOMER_CREATED, userId, null, newValue);
        } catch (Exception e) {
            log.error("Error logging audit for customer creation", e);
        }

        return convertToDTO(savedCustomer);
    }

    /**
     * Update an existing customer
     */
    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerDTO customerDTO, Long userId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ApplicationException("Customer not found", "CUSTOMER_NOT_FOUND"));
        if (!isOwnerOrStaff(customer.getShop().getId(), userId)) {
            throw new ApplicationException("Only OWNER or STAFF can update customers", "FORBIDDEN");
        }

        // Verify shop exists and user has access to it
        Shop shop = shopRepository.findByIdAndUserId(customerDTO.getShopId(), userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it", "SHOP_NOT_FOUND"));

        // Validate phone uniqueness (allow if it's the same phone for this customer)
        if (!customer.getPhone().equals(customerDTO.getPhone()) &&
            customerRepository.existsByShopIdAndPhone(shop.getId(), customerDTO.getPhone())) {
            throw new ApplicationException("A customer with this phone number already exists in this shop", "PHONE_ALREADY_EXISTS");
        }

        // Store old value for audit
        String oldValue = null;
        try {
            oldValue = objectMapper.writeValueAsString(convertToDTO(customer));
        } catch (Exception e) {
            log.error("Error serializing old customer value for audit", e);
        }

        // Update the customer
        customer.setName(customerDTO.getName().trim());
        customer.setEntityName(customerDTO.getEntityName().trim());
        customer.setPhone(customerDTO.getPhone().trim());
        customer.setCurrentBalance(customerDTO.getCurrentBalance());

        if (customerDTO.getIsActive() != null) {
            customer.setIsActive(customerDTO.getIsActive());
        }
        customer.setShop(shop);
        customer.setUpdatedAt(LocalDateTime.now());

        Customer updatedCustomer = customerRepository.save(customer);

        // Audit log: Customer updated
        try {
            String newValue = objectMapper.writeValueAsString(convertToDTO(updatedCustomer));
            auditService.logAuditLongId(shop.getId(), AuditAction.CUSTOMER.name(), updatedCustomer.getId(), AuditAction.CUSTOMER_UPDATED, userId, oldValue, newValue);
        } catch (Exception e) {
            log.error("Error logging audit for customer update", e);
        }

        return convertToDTO(updatedCustomer);
    }

    /**
     * Get customers for a specific shop
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> getCustomersByShop(Long shopId, Long userId) {
        // Verify shop exists and user has access
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it", "SHOP_NOT_FOUND"));

        return customerRepository.findByShopId(shopId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active customers for a specific shop
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> getActiveCustomersByShop(Long shopId, Long userId) {
        // Verify shop exists and user has access
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it", "SHOP_NOT_FOUND"));

        return customerRepository.findActiveByShopId(shopId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
     * Helper: Create opening balance ledger entry for a new customer
     * Creates a BAKI (Debit) entry to track the opening balance
     */
    private void createOpeningBalanceLedgerEntry(Customer customer, Long userId) {
        try {
            // Get the user who created the customer
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

            // Create the ledger entry for opening balance
            CustomerLedger ledgerEntry = new CustomerLedger();
            ledgerEntry.setCustomer(customer);
            ledgerEntry.setShop(customer.getShop());
            ledgerEntry.setCreatedByUser(user);
            ledgerEntry.setEntryType(CustomerLedger.LedgerEntryType.BAKI);
            ledgerEntry.setAmount(customer.getOpeningBalance());
            ledgerEntry.setBalanceAfter(customer.getCurrentBalance());
            ledgerEntry.setNotes("Opening balance for new customer");
            ledgerEntry.setEntryDate(LocalDate.now());
            ledgerEntry.setCreatedAt(LocalDateTime.now());

            customerLedgerRepository.save(ledgerEntry);
            try {
                String newValue = objectMapper.writeValueAsString(convertToDTO(ledgerEntry));
                auditService.logAuditLongId(customer.getShop().getId(), AuditAction.LEDGER.name(), ledgerEntry.getId(), AuditAction.LEDGER_ENTRY_CREATED, userId, null, newValue);
            } catch (Exception e) {
                log.error("Error logging audit for ledger entry creation", e);
            }
        } catch (Exception e) {
            // Log the exception but don't fail the customer creation
            // The customer is already created, we just couldn't create the ledger entry
            throw new ApplicationException("Failed to create opening balance ledger entry: " + e.getMessage(), "LEDGER_CREATION_FAILED");
        }
    }

    /**
     * Convert Customer entity to CustomerDTO
     */
    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEntityName(customer.getEntityName());
        dto.setPhone(customer.getPhone());
        dto.setOpeningBalance(customer.getOpeningBalance());
        dto.setCurrentBalance(customer.getCurrentBalance());
        dto.setShopId(customer.getShop() != null ? customer.getShop().getId() : null);
        dto.setIsActive(customer.getIsActive());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }

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
}
