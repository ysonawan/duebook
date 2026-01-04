package com.duebook.app.service;

import com.duebook.app.dto.ShopDTO;
import com.duebook.app.dto.ShopUserDTO;
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
                .toList();
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
        logShopAudit(savedShop.getId(), AuditAction.SHOP_CREATED, userId, null, convertToDTO(savedShop));

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
        ShopDTO oldShopDTO = convertToDTO(shop);

        shop.setName(shopDTO.getName().trim());
        shop.setAddress(shopDTO.getAddress() != null ? shopDTO.getAddress().trim() : null);
        if (shopDTO.getIsActive() != null) {
            shop.setIsActive(shopDTO.getIsActive());
        }
        shop.setUpdatedAt(LocalDateTime.now());

        Shop updatedShop = shopRepository.save(shop);

        // Audit log: Shop updated
        logShopAudit(shopId, AuditAction.SHOP_UPDATED, userId, oldShopDTO, convertToDTO(updatedShop));

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

    /**
     * Add a user to a shop with a specific role
     */
    @Transactional
    public ShopUserDTO addUserToShop(Long shopId, ShopUserDTO shopUserDTO, Long currentUserId) {
        // Verify shop exists and current user is owner
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ApplicationException("Shop not found"));

        shopUserRepository.findOwnerByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ApplicationException("You must be the shop owner to add users"));

        // Find user by phone number
        User userToAdd = userRepository.findByPhone(shopUserDTO.getUserPhone())
                .orElseThrow(() -> new ApplicationException("User with phone " + shopUserDTO.getUserPhone() + " not found in the system"));

        // Check if user already exists in shop
        if (shopUserRepository.existsByShopIdAndUserId(shopId, userToAdd.getId())) {
            throw new ApplicationException("This user is already a member of this shop");
        }

        // Create ShopUser relationship
        ShopUser shopUser = new ShopUser();
        shopUser.setShop(shop);
        shopUser.setUser(userToAdd);
        shopUser.setRole(shopUserDTO.getRole());
        shopUser.setStatus(ShopUser.ShopUserStatus.ACTIVE);
        shopUser.setJoinedAt(LocalDateTime.now());

        ShopUser savedShopUser = shopUserRepository.save(shopUser);
        ShopUserDTO resultDTO = convertShopUserToDTO(savedShopUser, userToAdd);
        // Audit log
        logShopAudit(shopId, AuditAction.SHOP_UPDATED, currentUserId, null, resultDTO);

        return resultDTO;
    }

    /**
     * Get all users in a shop
     */
    @Transactional(readOnly = true)
    public List<ShopUserDTO> getShopUsers(Long shopId, Long currentUserId) {
        // Verify shop exists and current user has access
        shopRepository.findById(shopId)
                .orElseThrow(() -> new ApplicationException("Shop not found"));

        shopUserRepository.findByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ApplicationException("You don't have access to this shop"));

        List<ShopUser> shopUsers = shopUserRepository.findAllActiveByShopId(shopId);
        // Convert to DTOs immediately to avoid lazy loading issues after transaction ends
        return shopUsers.stream()
                .map(shopUser -> {
                    User user = shopUser.getUser();
                    return convertShopUserToDTO(shopUser, user);
                })
                .toList();
    }

    /**
     * Update user role in a shop
     */
    @Transactional
    public ShopUserDTO updateUserRoleInShop(Long shopId, Long shopUserId, ShopUser.ShopUserRole newRole, Long currentUserId) {
        // Verify shop exists and current user is owner
        shopUserRepository.findOwnerByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ApplicationException("You must be the shop owner to update roles"));

        ShopUser shopUser = shopUserRepository.findById(shopUserId)
                .orElseThrow(() -> new ApplicationException("Shop user not found"));

        // Verify shop user belongs to this shop
        if (!shopUser.getShop().getId().equals(shopId)) {
            throw new ApplicationException("User does not belong to this shop");
        }
        // Cannot downgrade the only owner
        if (shopUser.getRole() == ShopUser.ShopUserRole.OWNER && newRole != ShopUser.ShopUserRole.OWNER) {
            List<ShopUser> owners = shopUserRepository.findAllActiveByShopId(shopId).stream()
                    .filter(su -> su.getRole() == ShopUser.ShopUserRole.OWNER)
                    .toList();
            if (owners.size() == 1) {
                throw new ApplicationException("Cannot remove the only owner from the shop");
            }
        }
        // Eagerly load user data before returning
        User user = shopUser.getUser();
        // Store old value for audit
        ShopUserDTO oldShopUserDTO = convertShopUserToDTO(shopUser, user);

        shopUser.setRole(newRole);
        ShopUser updatedShopUser = shopUserRepository.save(shopUser);
        ShopUserDTO resultDTO = convertShopUserToDTO(updatedShopUser, user);
        // Audit log
        logShopAudit(shopId, AuditAction.SHOP_UPDATED, currentUserId, oldShopUserDTO, resultDTO);

        return resultDTO;
    }

    /**
     * Remove user from shop
     */
    @Transactional
    public void removeUserFromShop(Long shopId, Long shopUserId, Long currentUserId) {
        // Verify shop exists and current user is owner
        shopUserRepository.findOwnerByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ApplicationException("You must be the shop owner to remove users"));

        ShopUser shopUser = shopUserRepository.findById(shopUserId)
                .orElseThrow(() -> new ApplicationException("Shop user not found"));

        // Verify shop user belongs to this shop
        if (!shopUser.getShop().getId().equals(shopId)) {
            throw new ApplicationException("User does not belong to this shop");
        }

        // Cannot remove the only owner
        if (shopUser.getRole() == ShopUser.ShopUserRole.OWNER) {
            List<ShopUser> owners = shopUserRepository.findAllActiveByShopId(shopId).stream()
                    .filter(su -> su.getRole() == ShopUser.ShopUserRole.OWNER)
                    .toList();
            if (owners.size() == 1) {
                throw new ApplicationException("Cannot remove the only owner from the shop");
            }
        }
        // Eagerly load user data before returning
        User user = shopUser.getUser();
        // Store old value for audit
        ShopUserDTO oldShopUserDTO = convertShopUserToDTO(shopUser, user);
        shopUser.setStatus(ShopUser.ShopUserStatus.INACTIVE);
        ShopUser updatedShopUser = shopUserRepository.save(shopUser);

        // Audit log
        logShopAudit(shopId, AuditAction.SHOP_UPDATED, currentUserId, oldShopUserDTO, convertShopUserToDTO(updatedShopUser, user));
    }

    /**
     * Convert ShopUser entity to DTO with explicit user object (avoids lazy loading)
     */
    private ShopUserDTO convertShopUserToDTO(ShopUser shopUser, User user) {
        ShopUserDTO dto = new ShopUserDTO();
        dto.setId(shopUser.getId());
        dto.setShopId(shopUser.getShop().getId());
        dto.setUserId(user.getId());
        dto.setUserName(user.getName());
        dto.setUserEmail(user.getEmail());
        dto.setUserPhone(user.getPhone());
        dto.setRole(shopUser.getRole());
        dto.setStatus(shopUser.getStatus());
        dto.setJoinedAt(shopUser.getJoinedAt());
        return dto;
    }

    /**
     * Log audit for shop actions
     */
    private void logShopAudit(Long shopId, AuditAction action, Long userId, Object oldValue, Object newValue) {
        try {
            String oldVal = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newVal = newValue != null ? objectMapper.writeValueAsString(newValue) : null;
            auditService.logAuditLongId(shopId, AuditAction.SHOP.name(), shopId, action, userId, oldVal, newVal);
        } catch (Exception e) {
            log.error("Error logging audit for shop operation: " + action.name(), e);
        }
    }
}
