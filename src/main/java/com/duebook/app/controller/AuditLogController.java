package com.duebook.app.controller;

import com.duebook.app.dto.AuditLogDTO;
import com.duebook.app.exception.ApplicationException;
import com.duebook.app.model.AuditLog;
import com.duebook.app.model.ShopUser;
import com.duebook.app.model.User;
import com.duebook.app.repository.AuditLogRepository;
import com.duebook.app.repository.ShopUserRepository;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for retrieving audit logs
 * All endpoints require authentication
 * Users can only view audit logs for shops they have access to
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class AuditLogController {

    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ShopUserRepository shopUserRepository;

    /**
     * Get paginated audit logs for a shop with optional filters and date range
     * Supports filtering by action, entityType, and date range
     */
    @GetMapping("/shop/{shopId}/paginated")
    public ResponseEntity<?> getAuditLogsByShopPaginatedWithDates(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        verifyUserAccessToShop(shopId, userId);

        log.debug("Fetching paginated audit logs for shop ID: {} (page: {}, size: {}, action: {}, entityType: {}, startDate: {}, endDate: {}) by user ID: {}",
                shopId, page, size, action, entityType, startDate, endDate, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs;

        // Parse date range if provided
        java.time.LocalDateTime startDateTime = null;
        java.time.LocalDateTime endDateTime = null;

        if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_DATE;
                java.time.LocalDate start = java.time.LocalDate.parse(startDate, formatter);
                java.time.LocalDate end = java.time.LocalDate.parse(endDate, formatter);

                startDateTime = start.atStartOfDay();
                endDateTime = end.atTime(23, 59, 59);
            } catch (Exception e) {
                log.error("Error parsing date range: start={}, end={}", startDate, endDate, e);
            }
        }

        // Apply filters based on provided parameters
        if (startDateTime != null && endDateTime != null) {
            // With date range
            if (action != null && !action.trim().isEmpty() && entityType != null && !entityType.trim().isEmpty()) {
                // Filter by action, entity type, and date range
                auditLogs = auditLogRepository.findByShopIdAndActionAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
                        shopId, action, entityType, startDateTime, endDateTime, pageable);
            } else if (action != null && !action.trim().isEmpty()) {
                // Filter by action and date range
                auditLogs = auditLogRepository.findByShopIdAndActionAndDateRangeOrderByPerformedAtDesc(
                        shopId, action, startDateTime, endDateTime, pageable);
            } else if (entityType != null && !entityType.trim().isEmpty()) {
                // Filter by entity type and date range
                auditLogs = auditLogRepository.findByShopIdAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
                        shopId, entityType, startDateTime, endDateTime, pageable);
            } else {
                // Filter by date range only
                auditLogs = auditLogRepository.findByShopIdAndDateRangeOrderByPerformedAtDesc(
                        shopId, startDateTime, endDateTime, pageable);
            }
        } else {
            // Without date range
            if (action != null && !action.trim().isEmpty() && entityType != null && !entityType.trim().isEmpty()) {
                // Filter by both action and entity type
                auditLogs = auditLogRepository.findByShopIdAndActionAndEntityTypeOrderByPerformedAtDesc(shopId, action, entityType, pageable);
            } else if (action != null && !action.trim().isEmpty()) {
                // Filter by action only
                auditLogs = auditLogRepository.findByShopIdAndActionOrderByPerformedAtDesc(shopId, action, pageable);
            } else if (entityType != null && !entityType.trim().isEmpty()) {
                // Filter by entity type only
                auditLogs = auditLogRepository.findByShopIdAndEntityTypeOrderByPerformedAtDesc(shopId, entityType, pageable);
            } else {
                // No filters, get all audit logs for the shop
                auditLogs = auditLogRepository.findByShopIdOrderByPerformedAtDesc(shopId, pageable);
            }
        }

        Page<AuditLogDTO> dtos = auditLogs.map(this::convertToDTO);

        log.info("Retrieved page {} with {} audit logs for shop ID: {} (action: {}, entityType: {}, startDate: {}, endDate: {})",
                page, dtos.getContent().size(), shopId, action, entityType, startDate, endDate);

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/shop/{shopId}/actions")
    public ResponseEntity<?> getDistinctActions(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        verifyUserAccessToShop(shopId, userId);

        log.debug("Fetching distinct actions for shop ID: {} by user ID: {}", shopId, userId);

        var actions = auditLogRepository.findDistinctActionsByShopId(shopId);
        log.info("Retrieved {} distinct actions for shop ID: {}", actions.size(), shopId);

        return ResponseEntity.ok(actions);
    }

    @GetMapping("/shop/{shopId}/entity-types")
    public ResponseEntity<?> getDistinctEntityTypes(
            @PathVariable Long shopId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        verifyUserAccessToShop(shopId, userId);

        log.debug("Fetching distinct entity types for shop ID: {} by user ID: {}", shopId, userId);

        var entityTypes = auditLogRepository.findDistinctEntityTypesByShopId(shopId);
        log.info("Retrieved {} distinct entity types for shop ID: {}", entityTypes.size(), shopId);

        return ResponseEntity.ok(entityTypes);
    }

    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setShopId(auditLog.getShop().getId());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setAction(auditLog.getAction());
        dto.setPerformedById(auditLog.getPerformedBy().getId());
        dto.setPerformedByName(auditLog.getPerformedBy().getName());
        dto.setOldValue(auditLog.getOldValue());
        dto.setNewValue(auditLog.getNewValue());
        dto.setPerformedAt(auditLog.getPerformedAt());
        return dto;
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized audit log access attempt");
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
                    log.warn("User ID: {} attempted to access audit logs for shop ID: {} without access", userId, shopId);
                    return new ApplicationException("You don't have access to this shop", "FORBIDDEN");
                });

        // Verify user is still active in the shop
        if (shopUser.getStatus() != ShopUser.ShopUserStatus.ACTIVE) {
            log.warn("Inactive user ID: {} attempted to access audit logs for shop ID: {}", userId, shopId);
            throw new ApplicationException("Your access to this shop has been revoked", "FORBIDDEN");
        }

        log.debug("Access verified for user ID: {} to shop ID: {}", userId, shopId);
    }
}
