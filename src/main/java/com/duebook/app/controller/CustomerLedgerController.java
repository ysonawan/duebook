package com.duebook.app.controller;

import com.duebook.app.dto.CustomerLedgerDTO;
import com.duebook.app.dto.LedgerSummaryDTO;
import com.duebook.app.service.CustomerLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.CustomerLedger;
import com.duebook.app.model.ShopUser;
import com.duebook.app.repository.CustomerLedgerRepository;
import com.duebook.app.repository.ShopUserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class CustomerLedgerController {

    private final CustomerLedgerService ledgerService;
    private final UserRepository userRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final ShopUserRepository shopUserRepository;
    private final CustomerLedgerService customerLedgerService;

    /**
     * Get all ledger entries for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CustomerLedgerDTO>> getAllLedgerEntries(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching all ledger entries for user ID: {}", userId);
        List<CustomerLedgerDTO> entries = ledgerService.getAllLedgerEntriesForUser(userId);
        log.info("Retrieved {} ledger entries for user ID: {}", entries.size(), userId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get paginated ledger entries for a shop with optional filters and date range
     * Supports filtering by customer, entryType, and date range
     */
    @GetMapping("/shop/{shopId}/paginated")
    public ResponseEntity<Page<CustomerLedgerDTO>> getLedgerEntriesByShopPaginatedWithFilters(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {

        Long userId = extractUserId(authentication);

        // Determine which shops the user can access
        List<Long> accessibleShopIds;
        if (shopId == 0) {
            // All Shops - fetch list of shops user has access to
            accessibleShopIds = getAccessibleShopIdsForUser(userId);
            if (accessibleShopIds.isEmpty()) {
                // User has no accessible shops
                return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(new java.util.ArrayList<>()));
            }
        } else {
            // Specific shop - verify access
            verifyUserAccessToShop(shopId, userId);
            accessibleShopIds = java.util.List.of(shopId);
        }

        log.debug("Fetching paginated ledger entries for shop ID: {} (accessible shops: {}, page: {}, size: {}, customerId: {}, entryType: {}, startDate: {}, endDate: {}) by user ID: {}",
                shopId, accessibleShopIds, page, size, customerId, entryType, startDate, endDate, userId);

        Pageable pageable = PageRequest.of(page, size);

        LocalDate startDateTime = parseDate(startDate);
        LocalDate endDateTime = parseDate(endDate);

        Page<CustomerLedger> ledgerEntries = getFilteredLedgerEntries(accessibleShopIds, customerId, entryType, startDateTime, endDateTime, pageable);
        Page<CustomerLedgerDTO> dtos = customerLedgerService.getCustomerLedgerDTOs(ledgerEntries);

        log.info("Retrieved page {} with {} ledger entries for accessible shops: {} (customerId: {}, entryType: {}, startDate: {}, endDate: {})",
                page, dtos.getContent().size(), accessibleShopIds, customerId, entryType, startDate, endDate);

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get ledger summary for a shop with applied filters (no pagination)
     * Used for summary cards that need complete data across all pages
     */
    @GetMapping("/shop/{shopId}/summary")
    public ResponseEntity<LedgerSummaryDTO> getLedgerSummary(
            @PathVariable Long shopId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Long actualShopId = (shopId == 0) ? null : shopId;

        if (actualShopId != null) {
            verifyUserAccessToShop(actualShopId, userId);
        }

        LocalDate startDateTime = parseDate(startDate);
        LocalDate endDateTime = parseDate(endDate);

        log.debug("Fetching ledger summary for shop ID: {} (customerId: {}, entryType: {}, startDate: {}, endDate: {}) by user ID: {}",
                shopId, customerId, entryType, startDate, endDate, userId);

        LedgerSummaryDTO summary = ledgerService.getLedgerSummary(userId, actualShopId, customerId, entryType, startDateTime, endDateTime);

        log.info("Retrieved ledger summary for shop ID: {} (totalDebit: {}, totalCredit: {}, netBalance: {}, totalEntries: {})",
                shopId, summary.getTotalDebit(), summary.getTotalCredit(), summary.getNetBalance(), summary.getTotalEntries());

        return ResponseEntity.ok(summary);
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            return LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            log.error("Error parsing date: {}", dateString, e);
            return null;
        }
    }

    private Page<CustomerLedger> getFilteredLedgerEntries(List<Long> shopIds, Long customerId, String entryType,
                                                           LocalDate startDate, LocalDate endDate, Pageable pageable) {
        boolean hasCustomer = customerId != null && customerId != 0;
        boolean hasEntryType = entryType != null && !entryType.trim().isEmpty();
        boolean hasDateRange = startDate != null && endDate != null;

        if (!hasCustomer && !hasEntryType && !hasDateRange) {
            return customerLedgerRepository.findByShopIdsInPaginated(shopIds, pageable);
        }

        if (hasDateRange) {
            return getFilteredLedgerEntriesWithDateRange(shopIds, customerId, entryType, startDate, endDate, pageable);
        } else {
            return getFilteredLedgerEntriesWithoutDateRange(shopIds, customerId, entryType, pageable);
        }
    }

    private Page<CustomerLedger> getFilteredLedgerEntriesWithDateRange(List<Long> shopIds, Long customerId, String entryType,
                                                                        LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (customerId != null && customerId > 0 && entryType != null && !entryType.trim().isEmpty()) {
            return customerLedgerRepository.findByShopIdsInCustomerIdEntryTypeAndDateRangePaginated(
                    shopIds, customerId, entryType, startDate, endDate, pageable);
        } else if (customerId != null && customerId > 0) {
            return customerLedgerRepository.findByShopIdsInCustomerIdAndDateRangePaginated(
                    shopIds, customerId, startDate, endDate, pageable);
        } else if (entryType != null && !entryType.trim().isEmpty()) {
            return customerLedgerRepository.findByShopIdsInEntryTypeAndDateRangePaginated(
                    shopIds, entryType, startDate, endDate, pageable);
        } else {
            return customerLedgerRepository.findByShopIdsInAndDateRangePaginated(
                    shopIds, startDate, endDate, pageable);
        }
    }

    private Page<CustomerLedger> getFilteredLedgerEntriesWithoutDateRange(List<Long> shopIds, Long customerId, String entryType, Pageable pageable) {
        if (customerId != null && entryType != null && !entryType.trim().isEmpty()) {
            return customerLedgerRepository.findByShopIdsInCustomerIdAndEntryTypePaginated(
                    shopIds, customerId, entryType, pageable);
        } else if (customerId != null) {
            return customerLedgerRepository.findByShopIdsInAndCustomerIdPaginated(
                    shopIds, customerId, pageable);
        } else {
            return customerLedgerRepository.findByShopIdsInAndEntryTypePaginated(
                    shopIds, entryType, pageable);
        }
    }

    private List<Long> getAccessibleShopIdsForUser(Long userId) {
        log.debug("Fetching accessible shop IDs for user ID: {}", userId);
        // Get all shops where user is an ACTIVE member
        List<ShopUser> shopUsers = shopUserRepository.findAllActiveByUserId(userId);
        List<Long> shopIds = shopUsers.stream()
                .map(su -> su.getShop().getId())
                .toList();
        log.debug("User ID: {} has access to {} shops: {}", userId, shopIds.size(), shopIds);
        return shopIds;
    }

    /**
     * Create a new ledger entry
     */
    @PostMapping
    public ResponseEntity<CustomerLedgerDTO> createLedgerEntry(
            @Valid @RequestBody CustomerLedgerDTO ledgerDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Creating new ledger entry for customer ID: {} with amount: {} by user ID: {}", ledgerDTO.getCustomerId(), ledgerDTO.getAmount(), userId);
        CustomerLedgerDTO createdEntry = ledgerService.createLedgerEntry(ledgerDTO, userId);
        log.info("Ledger entry created successfully with ID: {} by user ID: {}", createdEntry.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
    }

    /**
     * Reverse a ledger entry
     */
    @PostMapping("/{id}/reverse")
    public ResponseEntity<CustomerLedgerDTO> reverseLedgerEntry(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        String notes = body != null ? body.get("notes") : null;
        log.info("Reversing ledger entry ID: {} by user ID: {}", id, userId);
        CustomerLedgerDTO reversedEntry = ledgerService.reverseLedgerEntry(id, userId, notes);
        log.info("Ledger entry ID: {} reversed successfully by user ID: {}", id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reversedEntry);
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized ledger access attempt");
            throw new ApplicationException("User not authenticated", "UNAUTHORIZED");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

        return user.getId();
    }

    private void verifyUserAccessToShop(Long shopId, Long userId) {
        ShopUser shopUser = shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> {
                    log.warn("User ID: {} attempted to access ledger for shop ID: {} without access", userId, shopId);
                    return new ApplicationException("You don't have access to this shop", "FORBIDDEN");
                });

        // Verify user is still active in the shop
        if (shopUser.getStatus() != ShopUser.ShopUserStatus.ACTIVE) {
            log.warn("Inactive user ID: {} attempted to access ledger for shop ID: {}", userId, shopId);
            throw new ApplicationException("Your access to this shop has been revoked", "FORBIDDEN");
        }

        log.debug("Access verified for user ID: {} to shop ID: {}", userId, shopId);
    }
}

