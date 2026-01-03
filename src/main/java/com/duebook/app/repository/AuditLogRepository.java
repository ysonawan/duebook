package com.duebook.app.repository;

import com.duebook.app.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdOrderByPerformedAtDesc(@Param("shopIds") List<Long> shopIds, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.entityType = :entityType ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndEntityTypeOrderByPerformedAtDesc(@Param("shopIds") List<Long> shopIds, @Param("entityType") String entityType, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.action = :action ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionOrderByPerformedAtDesc(@Param("shopIds") List<Long> shopIds, @Param("action") String action, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.action = :action AND al.entityType = :entityType ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndEntityTypeOrderByPerformedAtDesc(
            @Param("shopIds") List<Long> shopIds,
            @Param("action") String action,
            @Param("entityType") String entityType,
            Pageable pageable);

    @Query("SELECT DISTINCT al.action FROM AuditLog al WHERE al.shop.id = :shopId ORDER BY al.action ASC")
    List<String> findDistinctActionsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT DISTINCT al.entityType FROM AuditLog al WHERE al.shop.id = :shopId ORDER BY al.entityType ASC")
    List<String> findDistinctEntityTypesByShopId(@Param("shopId") Long shopId);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndDateRangeOrderByPerformedAtDesc(
            @Param("shopIds") List<Long> shopIds,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.action = :action AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndDateRangeOrderByPerformedAtDesc(
            @Param("shopIds") List<Long> shopIds,
            @Param("action") String action,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.entityType = :entityType AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
            @Param("shopIds") List<Long> shopIds,
            @Param("entityType") String entityType,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.shop.id IN :shopIds AND al.action = :action AND al.entityType = :entityType AND al.performedAt >= :startDate AND al.performedAt <= :endDate ORDER BY al.performedAt DESC")
    Page<AuditLog> findByShopIdAndActionAndEntityTypeAndDateRangeOrderByPerformedAtDesc(
            @Param("shopIds") List<Long> shopIds,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);
}
