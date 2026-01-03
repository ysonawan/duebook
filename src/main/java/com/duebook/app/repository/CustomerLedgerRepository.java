package com.duebook.app.repository;

import com.duebook.app.model.CustomerLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer.id = :customerId ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id = :shopId ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findByShopId(@Param("shopId") Long shopId);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.entryDate BETWEEN :startDate AND :endDate ORDER BY cl.entryDate DESC")
    List<CustomerLedger> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT cl FROM CustomerLedger cl " +
           "INNER JOIN Shop s ON cl.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT cl FROM CustomerLedger cl " +
           "INNER JOIN Shop s ON cl.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE cl.id = :ledgerId AND su.user.id = :userId AND su.status = 'ACTIVE'")
    Optional<CustomerLedger> findByIdAndUserId(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInPaginated(@Param("shopIds") List<Long> shopIds, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND cl.customer.id = :customerId ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInAndCustomerIdPaginated(@Param("shopIds") List<Long> shopIds, @Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND CAST(cl.entryType AS string) = :entryType ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInAndEntryTypePaginated(@Param("shopIds") List<Long> shopIds, @Param("entryType") String entryType, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND CAST(cl.entryDate AS date) >= :startDate AND CAST(cl.entryDate AS date) <= :endDate ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInAndDateRangePaginated(@Param("shopIds") List<Long> shopIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND cl.customer.id = :customerId AND CAST(cl.entryType AS string) = :entryType ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInCustomerIdAndEntryTypePaginated(@Param("shopIds") List<Long> shopIds, @Param("customerId") Long customerId, @Param("entryType") String entryType, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND cl.customer.id = :customerId AND CAST(cl.entryDate AS date) >= :startDate AND CAST(cl.entryDate AS date) <= :endDate ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInCustomerIdAndDateRangePaginated(@Param("shopIds") List<Long> shopIds, @Param("customerId") Long customerId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND CAST(cl.entryType AS string) = :entryType AND CAST(cl.entryDate AS date) >= :startDate AND CAST(cl.entryDate AS date) <= :endDate ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInEntryTypeAndDateRangePaginated(@Param("shopIds") List<Long> shopIds, @Param("entryType") String entryType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id IN :shopIds AND cl.customer.id = :customerId AND CAST(cl.entryType AS string) = :entryType AND CAST(cl.entryDate AS date) >= :startDate AND CAST(cl.entryDate AS date) <= :endDate ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    Page<CustomerLedger> findByShopIdsInCustomerIdEntryTypeAndDateRangePaginated(@Param("shopIds") List<Long> shopIds, @Param("customerId") Long customerId, @Param("entryType") String entryType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
}

