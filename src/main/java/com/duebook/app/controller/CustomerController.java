package com.duebook.app.controller;

import com.duebook.app.dto.CustomerDTO;
import com.duebook.app.dto.CustomerSummaryDTO;
import com.duebook.app.service.CustomerService;
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
import com.duebook.app.model.Customer;
import com.duebook.app.model.ShopUser;
import com.duebook.app.repository.CustomerRepository;
import com.duebook.app.repository.ShopUserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ShopUserRepository shopUserRepository;

    /**
     * Get all customers for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching all customers for user ID: {}", userId);
        List<CustomerDTO> customers = customerService.getAllCustomersForUser(userId);
        log.info("Retrieved {} customers for user ID: {}", customers.size(), userId);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get a specific customer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching customer ID: {} for user ID: {}", id, userId);
        CustomerDTO customer = customerService.getCustomerById(id, userId);
        log.info("Retrieved customer ID: {} for user ID: {}", id, userId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get paginated customers for a shop with optional filters
     * Supports filtering by status and search term
     */
    @GetMapping("/shop/{shopId}/paginated")
    public ResponseEntity<Page<CustomerDTO>> getCustomersByShopPaginated(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm,
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

        log.debug("Fetching paginated customers for shop ID: {} (accessible shops: {}, page: {}, size: {}, status: {}, searchTerm: {}) by user ID: {}",
                shopId, accessibleShopIds, page, size, status, searchTerm, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customers = getFilteredCustomers(accessibleShopIds, status, searchTerm, pageable);

        Page<CustomerDTO> dtos = customerService.getCustomerDTOs(customers);

        log.info("Retrieved page {} with {} customers for accessible shops: {} (status: {}, searchTerm: {})",
                page, dtos.getContent().size(), accessibleShopIds, status, searchTerm);

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get customer summary for a shop with applied filters (no pagination)
     * Used for summary cards that need complete data across all pages
     */
    @GetMapping("/shop/{shopId}/summary")
    public ResponseEntity<CustomerSummaryDTO> getCustomerSummary(
            @PathVariable Long shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Long actualShopId = (shopId == 0) ? null : shopId;

        if (actualShopId != null) {
            verifyUserAccessToShop(actualShopId, userId);
        }

        log.debug("Fetching customer summary for shop ID: {} (status: {}, searchTerm: {}) by user ID: {}",
                shopId, status, searchTerm, userId);

        CustomerSummaryDTO summary = customerService.getCustomerSummary(userId, actualShopId, status, searchTerm);

        log.info("Retrieved customer summary for shop ID: {} (totalCustomers: {}, activeCustomers: {}, totalCurrentBalance: {})",
                shopId, summary.getTotalCustomers(), summary.getActiveCustomers(), summary.getTotalCurrentBalance());

        return ResponseEntity.ok(summary);
    }

    private Page<Customer> getFilteredCustomers(List<Long> shopIds, String status, String searchTerm, Pageable pageable) {
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        boolean isActive = hasStatus && status.equals("ACTIVE");

        if (hasStatus && hasSearch) {
            return customerRepository.findByShopIdsInStatusAndSearchTermPaginated(shopIds, isActive, searchTerm, pageable);
        } else if (hasStatus) {
            return customerRepository.findByShopIdsInAndStatusPaginated(shopIds, isActive, pageable);
        } else if (hasSearch) {
            return customerRepository.findByShopIdsInAndSearchTermPaginated(shopIds, searchTerm, pageable);
        }
        return customerRepository.findByShopIdsInPaginated(shopIds, pageable);
    }

    /**
     * Create a new customer
     */
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(
            @Valid @RequestBody CustomerDTO customerDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Creating new customer: {} for shop ID: {} by user ID: {}", customerDTO.getName(), customerDTO.getShopId(), userId);
        CustomerDTO createdCustomer = customerService.createCustomer(customerDTO, userId);
        log.info("Customer created successfully with ID: {} by user ID: {}", createdCustomer.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
    }

    /**
     * Update an existing customer
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDTO customerDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Updating customer ID: {} by user ID: {}", id, userId);
        CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO, userId);
        log.info("Customer ID: {} updated successfully by user ID: {}", id, userId);
        return ResponseEntity.ok(updatedCustomer);
    }


    /**
     * Get customers for a specific shop
     */
    @GetMapping("/shops/{shopId}")
    public ResponseEntity<List<CustomerDTO>> getCustomersByShop(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching customers for shop ID: {} by user ID: {}", shopId, userId);
        List<CustomerDTO> customers = customerService.getCustomersByShop(shopId, userId);
        log.info("Retrieved {} customers for shop ID: {}", customers.size(), shopId);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get active customers for a specific shop
     */
    @GetMapping("/shops/{shopId}/active")
    public ResponseEntity<List<CustomerDTO>> getActiveCustomersByShop(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching active customers for shop ID: {} by user ID: {}", shopId, userId);
        List<CustomerDTO> customers = customerService.getActiveCustomersByShop(shopId, userId);
        log.info("Retrieved {} active customers for shop ID: {}", customers.size(), shopId);
        return ResponseEntity.ok(customers);
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt");
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
                    log.warn("User ID: {} attempted to access customers for shop ID: {} without access", userId, shopId);
                    return new ApplicationException("You don't have access to this shop", "FORBIDDEN");
                });

        // Verify user is still active in the shop
        if (shopUser.getStatus() != ShopUser.ShopUserStatus.ACTIVE) {
            log.warn("Inactive user ID: {} attempted to access customers for shop ID: {}", userId, shopId);
            throw new ApplicationException("Your access to this shop has been revoked", "FORBIDDEN");
        }

        log.debug("Access verified for user ID: {} to shop ID: {}", userId, shopId);
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
}

