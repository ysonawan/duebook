package com.duebook.app.controller;

import com.duebook.app.dto.CustomerLedgerDTO;
import com.duebook.app.service.CustomerLedgerService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CustomerLedgerController {

    private final CustomerLedgerService ledgerService;
    private final UserRepository userRepository;

    /**
     * Get all ledger entries for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CustomerLedgerDTO>> getAllLedgerEntries(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<CustomerLedgerDTO> entries = ledgerService.getAllLedgerEntriesForUser(userId);
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
        CustomerLedgerDTO entry = ledgerService.getLedgerEntryById(id, userId);
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
        CustomerLedgerDTO createdEntry = ledgerService.createLedgerEntry(ledgerDTO, userId);
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
        CustomerLedgerDTO reversedEntry = ledgerService.reverseLedgerEntry(id, userId, notes);
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
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByCustomer(customerId, userId);
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
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByShop(shopId, userId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get ledger entries within a date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<CustomerLedgerDTO>> getLedgerByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        List<CustomerLedgerDTO> entries = ledgerService.getLedgerByDateRange(start, end);
        return ResponseEntity.ok(entries);
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

