package com.duebook.app.controller;

import com.duebook.app.dto.CustomerDTO;
import com.duebook.app.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    /**
     * Get all customers for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<CustomerDTO> customers = customerService.getAllCustomersForUser(userId);
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
        CustomerDTO customer = customerService.getCustomerById(id, userId);
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
        CustomerDTO createdCustomer = customerService.createCustomer(customerDTO, userId);
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
        CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO, userId);
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
        List<CustomerDTO> customers = customerService.getCustomersByShop(shopId, userId);
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
        List<CustomerDTO> customers = customerService.getActiveCustomersByShop(shopId, userId);
        return ResponseEntity.ok(customers);
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException("User not authenticated", "UNAUTHORIZED");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

        return user.getId();
    }
}

