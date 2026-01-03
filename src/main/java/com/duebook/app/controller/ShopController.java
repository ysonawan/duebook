package com.duebook.app.controller;

import com.duebook.app.dto.ShopDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class ShopController {

    private final ShopService shopService;
    private final UserRepository userRepository;

    /**
     * Get all shops for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ShopDTO>> getAllShops(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching all shops for user ID: {}", userId);
        List<ShopDTO> shops = shopService.getAllShopsForUser(userId);
        log.info("Retrieved {} shops for user ID: {}", shops.size(), userId);
        return ResponseEntity.ok(shops);
    }

    /**
     * Get a specific shop by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShopById(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching shop ID: {} for user ID: {}", id, userId);
        ShopDTO shop = shopService.getShopById(id, userId);
        log.info("Retrieved shop ID: {} for user ID: {}", id, userId);
        return ResponseEntity.ok(shop);
    }

    /**
     * Create a new shop
     */
    @PostMapping
    public ResponseEntity<ShopDTO> createShop(
            @Valid @RequestBody ShopDTO shopDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Creating new shop: {} by user ID: {}", shopDTO.getName(), userId);
        ShopDTO createdShop = shopService.createShop(shopDTO, userId);
        log.info("Shop created successfully with ID: {} by user ID: {}", createdShop.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdShop);
    }

    /**
     * Update an existing shop
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShopDTO> updateShop(
            @PathVariable Long id,
            @Valid @RequestBody ShopDTO shopDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Updating shop ID: {} by user ID: {}", id, userId);
        ShopDTO updatedShop = shopService.updateShop(id, shopDTO, userId);
        log.info("Shop ID: {} updated successfully by user ID: {}", id, userId);
        return ResponseEntity.ok(updatedShop);
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String phone = userDetails.getUsername();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApplicationException("User not found"));
        return user.getId();
    }
}

