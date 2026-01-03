package com.duebook.app.controller;

import com.duebook.app.dto.ShopUserDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.ShopUser;
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
@RequestMapping("/api/shops/{shopId}/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class ShopUserController {

    private final ShopService shopService;
    private final UserRepository userRepository;

    /**
     * Get all users in a shop
     */
    @GetMapping
    public ResponseEntity<List<ShopUserDTO>> getShopUsers(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.debug("Fetching all users for shop ID: {} by user ID: {}", shopId, userId);
        List<ShopUserDTO> users = shopService.getShopUsers(shopId, userId);
        log.info("Retrieved {} users for shop ID: {}", users.size(), shopId);
        return ResponseEntity.ok(users);
    }

    /**
     * Add a user to a shop
     */
    @PostMapping
    public ResponseEntity<ShopUserDTO> addUserToShop(
            @PathVariable Long shopId,
            @Valid @RequestBody ShopUserDTO shopUserDTO,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        shopUserDTO.setShopId(shopId);
        log.info("Adding user with phone {} to shop ID: {} by user ID: {}", shopUserDTO.getUserPhone(), shopId, userId);
        ShopUserDTO addedUser = shopService.addUserToShop(shopId, shopUserDTO, userId);
        log.info("User successfully added to shop ID: {}", shopId);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedUser);
    }

    /**
     * Update user role in a shop
     */
    @PutMapping("/{shopUserId}/role")
    public ResponseEntity<ShopUserDTO> updateUserRole(
            @PathVariable Long shopId,
            @PathVariable Long shopUserId,
            @RequestBody RoleUpdateRequest roleUpdateRequest,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Updating role for shop user ID: {} in shop ID: {} by user ID: {}", shopUserId, shopId, userId);
        ShopUserDTO updatedUser = shopService.updateUserRoleInShop(shopId, shopUserId, roleUpdateRequest.getRole(), userId);
        log.info("Role updated successfully for shop user ID: {}", shopUserId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Remove user from shop
     */
    @DeleteMapping("/{shopUserId}")
    public ResponseEntity<Void> removeUserFromShop(
            @PathVariable Long shopId,
            @PathVariable Long shopUserId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Removing shop user ID: {} from shop ID: {} by user ID: {}", shopUserId, shopId, userId);
        shopService.removeUserFromShop(shopId, shopUserId, userId);
        log.info("User successfully removed from shop ID: {}", shopId);
        return ResponseEntity.noContent().build();
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

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RoleUpdateRequest {
        private ShopUser.ShopUserRole role;
    }
}

