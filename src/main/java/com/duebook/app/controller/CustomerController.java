package com.duebook.app.controller;

import com.duebook.app.dto.CustomerDTO;
import com.duebook.app.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.exception.ApplicationException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

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
}

