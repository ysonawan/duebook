package com.duebook.app.repository;

import com.duebook.app.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByShopIdOrderByPerformedAtDesc(Long shopId, Pageable pageable);

    Page<AuditLog> findByShopIdAndEntityTypeOrderByPerformedAtDesc(Long shopId, String entityType, Pageable pageable);

    Page<AuditLog> findByShopIdAndActionOrderByPerformedAtDesc(Long shopId, String action, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.action = :action AND al.entityType = :entityType ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndEntityTypeOrderByPerformedAtDesc(
            @Param("shopId") Long shopId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.performedAt BETWEEN :startDate AND :endDate ORDER BY al.performedAt DESC")
    List<AuditLog> findByShopIdAndDateRange(@Param("shopId") Long shopId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.entityType = :entityType ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopAndEntityType(@Param("shopId") Long shopId,
                                          @Param("entityType") String entityType,
                                          Pageable pageable);

    @Query("SELECT DISTINCT al.action FROM AuditLog al WHERE al.shop.id = :shopId ORDER BY al.action ASC")
    List<String> findDistinctActionsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT DISTINCT al.entityType FROM AuditLog al WHERE al.shop.id = :shopId ORDER BY al.entityType ASC")
    List<String> findDistinctEntityTypesByShopId(@Param("shopId") Long shopId);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndDateRangeOrderByPerformedAtDesc(
            @Param("shopId") Long shopId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.action = :action AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndDateRangeOrderByPerformedAtDesc(
            @Param("shopId") Long shopId,
            @Param("action") String action,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.entityType = :entityType AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
            @Param("shopId") Long shopId,
            @Param("entityType") String entityType,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id = :shopId AND al.action = :action AND al.entityType = :entityType AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
            @Param("shopId") Long shopId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);
}
