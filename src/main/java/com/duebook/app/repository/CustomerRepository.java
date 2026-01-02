package com.duebook.app.repository;

import com.duebook.app.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find all customers for a specific shop
     */
    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId ORDER BY c.createdAt DESC")
    List<Customer> findByShopId(@Param("shopId") Long shopId);

    /**
     * Find all active customers for a specific shop
     */
    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Customer> findActiveByShopId(@Param("shopId") Long shopId);

    /**
     * Find all customers for shops owned by a user
     */
    @Query("SELECT c FROM Customer c " +
           "INNER JOIN Shop s ON c.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY c.createdAt DESC")
    List<Customer> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find customer by ID and verify user has access
     */
    @Query("SELECT c FROM Customer c " +
           "INNER JOIN Shop s ON c.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE c.id = :customerId AND su.user.id = :userId AND su.status = 'ACTIVE'")
    Optional<Customer> findByIdAndUserId(@Param("customerId") Long customerId, @Param("userId") Long userId);

    /**
     * Check if customer exists for a shop
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.shop.id = :shopId AND c.phone = :phone")
    boolean existsByShopIdAndPhone(@Param("shopId") Long shopId, @Param("phone") String phone);

    /**
     * Find customer by shop and phone
     */
    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId AND c.phone = :phone")
    Optional<Customer> findByShopIdAndPhone(@Param("shopId") Long shopId, @Param("phone") String phone);
}

