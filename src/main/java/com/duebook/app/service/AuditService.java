package com.duebook.app.service;

import com.duebook.app.model.AuditLog;
import com.duebook.app.model.AuditAction;
import com.duebook.app.model.Shop;
import com.duebook.app.model.User;
import com.duebook.app.repository.AuditLogRepository;
import com.duebook.app.repository.ShopRepository;
import com.duebook.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    public void logAudit(Long shopId, String entityType, UUID entityId, AuditAction action, Long performedBy, String oldValue, String newValue) {
        try {
            Optional<User> user = userRepository.findById(performedBy);
            Optional<Shop> shop = shopRepository.findById(shopId);

            if (user.isEmpty() || shop.isEmpty()) {
                log.warn("Could not find user {} or shop {} for audit logging", performedBy, shopId);
                return;
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setShop(shop.get());
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action.name());
            auditLog.setPerformedBy(user.get());
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setPerformedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: {} - {} on {} with ID {}", action.name(), entityType, entityType, entityId);
        } catch (Exception e) {
            log.error("Error logging audit for action: {}", action.name(), e);
            // Don't throw exception as audit logging should not break the main transaction
        }
    }

    public void logAuditLongId(Long shopId, String entityType, Long entityId, AuditAction action, Long performedBy, String oldValue, String newValue) {
        logAudit(shopId, entityType, convertLongToUUID(entityId), action, performedBy, oldValue, newValue);
    }

    private UUID convertLongToUUID(Long id) {
        if (id == null) {
            return null;
        }
        // Create a UUID from the long ID
        return new UUID(0, id);
    }

}
