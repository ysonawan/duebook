package com.duebook.app.controller;

import com.duebook.app.dto.CustomerLedgerDTO;
import com.duebook.app.service.CustomerLedgerService;
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
     * Get a specific ledger entry by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerLedgerDTO> getLedgerEntryById(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching ledger entry ID: {} for user ID: {}", id, userId);
        CustomerLedgerDTO entry = ledgerService.getLedgerEntryById(id, userId);
        log.info("Retrieved ledger entry ID: {} for user ID: {}", id, userId);
        return ResponseEntity.ok(entry);
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
     * Get ledger entries for a specific customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CustomerLedgerDTO>> getLedgerByCustomer(
            @PathVariable Long customerId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching ledger entries for customer ID: {} by user ID: {}", customerId, userId);
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByCustomer(customerId, userId);
        log.info("Retrieved {} ledger entries for customer ID: {}", entries.size(), customerId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get ledger entries for a specific shop
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<CustomerLedgerDTO>> getLedgerByShop(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching ledger entries for shop ID: {} by user ID: {}", shopId, userId);
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByShop(shopId, userId);
        log.info("Retrieved {} ledger entries for shop ID: {}", entries.size(), shopId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get ledger entries within a date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<CustomerLedgerDTO>> getLedgerByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.debug("Fetching ledger entries between {} and {}", startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByDateRange(start, end);
        log.info("Retrieved {} ledger entries for date range {} to {}", entries.size(), startDate, endDate);
        return ResponseEntity.ok(entries);
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
}

