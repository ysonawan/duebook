package com.duebook.app.service;

import com.duebook.app.dto.ShopDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.*;
import com.duebook.app.repository.ShopRepository;
import com.duebook.app.repository.ShopUserRepository;
import com.duebook.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopUserRepository shopUserRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Get all shops for the authenticated user
     */
    @Transactional(readOnly = true)
    public List<ShopDTO> getAllShopsForUser(Long userId) {
        return shopRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific shop for the authenticated user
     */
    @Transactional(readOnly = true)
    public ShopDTO getShopById(Long shopId, Long userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it"));
        return convertToDTO(shop);
    }

    /**
     * Create a new shop
     */
    @Transactional
    public ShopDTO createShop(ShopDTO shopDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("User not found"));

        // Create the shop
        Shop shop = new Shop();
        shop.setName(shopDTO.getName().trim());
        shop.setAddress(shopDTO.getAddress() != null ? shopDTO.getAddress().trim() : null);
        shop.setIsActive(true);
        shop.setCreatedAt(LocalDateTime.now());
        shop.setUpdatedAt(LocalDateTime.now());

        Shop savedShop = shopRepository.save(shop);

        // Create ShopUser relationship with OWNER role
        ShopUser shopUser = new ShopUser();
        shopUser.setShop(savedShop);
        shopUser.setUser(user);
        shopUser.setRole(ShopUser.ShopUserRole.OWNER);
        shopUser.setStatus(ShopUser.ShopUserStatus.ACTIVE);
        shopUser.setJoinedAt(LocalDateTime.now());

        shopUserRepository.save(shopUser);

        // Audit log: Shop created
        try {
            String newValue = objectMapper.writeValueAsString(convertToDTO(savedShop));
            auditService.logAuditLongId(savedShop.getId(), AuditAction.SHOP.name(), savedShop.getId(), AuditAction.SHOP_CREATED, userId, null, newValue);
        } catch (Exception e) {
            log.error("Error logging audit for shop creation", e);
        }

        return convertToDTO(savedShop);
    }

    /**
     * Update an existing shop
     */
    @Transactional
    public ShopDTO updateShop(Long shopId, ShopDTO shopDTO, Long userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Shop not found or you don't have access to it"));

        // Validate that user is OWNER or has update permissions
        ShopUser shopUser = shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ApplicationException("Access denied"));

        if (shopUser.getRole() != ShopUser.ShopUserRole.OWNER) {
            throw new ApplicationException("You don't have permission to update this shop");
        }

        // Store old value for audit
        String oldValue = null;
        try {
            oldValue = objectMapper.writeValueAsString(convertToDTO(shop));
        } catch (Exception e) {
            log.error("Error serializing old shop value for audit", e);
        }

        shop.setName(shopDTO.getName().trim());
        shop.setAddress(shopDTO.getAddress() != null ? shopDTO.getAddress().trim() : null);
        if (shopDTO.getIsActive() != null) {
            shop.setIsActive(shopDTO.getIsActive());
        }
        shop.setUpdatedAt(LocalDateTime.now());

        Shop updatedShop = shopRepository.save(shop);

        // Audit log: Shop updated
        try {
            String newValue = objectMapper.writeValueAsString(convertToDTO(updatedShop));
            auditService.logAuditLongId(shopId, AuditAction.SHOP.name(), shopId, AuditAction.SHOP_UPDATED, userId, oldValue, newValue);
        } catch (Exception e) {
            log.error("Error logging audit for shop update", e);
        }

        return convertToDTO(updatedShop);
    }

    /**
     * Convert Shop entity to DTO
     */
    private ShopDTO convertToDTO(Shop shop) {
        ShopDTO dto = new ShopDTO();
        dto.setId(shop.getId());
        dto.setName(shop.getName());
        dto.setAddress(shop.getAddress());
        dto.setIsActive(shop.getIsActive());
        dto.setCreatedAt(shop.getCreatedAt());
        dto.setUpdatedAt(shop.getUpdatedAt());
        return dto;
    }
}

