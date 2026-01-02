package com.duebook.app.repository;

import com.duebook.app.model.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    /**
     * Find all ledger entries for a specific customer
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer.id = :customerId ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Find all ledger entries for a specific shop
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.shop.id = :shopId ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findByShopId(@Param("shopId") Long shopId);

    /**
     * Find ledger entries within a date range
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.entryDate BETWEEN :startDate AND :endDate ORDER BY cl.entryDate DESC")
    List<CustomerLedger> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all ledger entries for shops owned by a user
     */
    @Query("SELECT cl FROM CustomerLedger cl " +
           "INNER JOIN Shop s ON cl.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY cl.entryDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find ledger entry by ID and verify user has access
     */
    @Query("SELECT cl FROM CustomerLedger cl " +
           "INNER JOIN Shop s ON cl.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE cl.id = :ledgerId AND su.user.id = :userId AND su.status = 'ACTIVE'")
    Optional<CustomerLedger> findByIdAndUserId(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * Find last ledger entry for a customer
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer.id = :customerId ORDER BY cl.entryDate DESC, cl.createdAt DESC LIMIT 1")
    Optional<CustomerLedger> findLastEntryForCustomer(@Param("customerId") Long customerId);
}

