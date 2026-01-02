package com.duebook.app.controller;

import com.duebook.app.dto.ShopDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class ShopController {

    private final ShopService shopService;
    private final UserRepository userRepository;

    /**
     * Get all shops for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ShopDTO>> getAllShops(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<ShopDTO> shops = shopService.getAllShopsForUser(userId);
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
        ShopDTO shop = shopService.getShopById(id, userId);
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
        ShopDTO createdShop = shopService.createShop(shopDTO, userId);
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
        ShopDTO updatedShop = shopService.updateShop(id, shopDTO, userId);
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

