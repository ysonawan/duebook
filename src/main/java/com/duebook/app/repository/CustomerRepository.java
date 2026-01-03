package com.duebook.app.repository;

import com.duebook.app.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId ORDER BY c.createdAt DESC")
    List<Customer> findByShopId(@Param("shopId") Long shopId);

    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Customer> findActiveByShopId(@Param("shopId") Long shopId);

    @Query("SELECT c FROM Customer c " +
           "INNER JOIN Shop s ON c.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY c.createdAt DESC")
    List<Customer> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Customer c " +
           "INNER JOIN Shop s ON c.shop.id = s.id " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE c.id = :customerId AND su.user.id = :userId AND su.status = 'ACTIVE'")
    Optional<Customer> findByIdAndUserId(@Param("customerId") Long customerId, @Param("userId") Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.shop.id = :shopId AND c.phone = :phone")
    boolean existsByShopIdAndPhone(@Param("shopId") Long shopId, @Param("phone") String phone);

    @Query("SELECT c FROM Customer c WHERE c.shop.id IN :shopIds ORDER BY c.createdAt DESC")
    Page<Customer> findByShopIdsInPaginated(@Param("shopIds") List<Long> shopIds, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.shop.id IN :shopIds AND c.isActive = :isActive ORDER BY c.createdAt DESC")
    Page<Customer> findByShopIdsInAndStatusPaginated(@Param("shopIds") List<Long> shopIds, @Param("isActive") boolean isActive, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.shop.id IN :shopIds AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR c.phone LIKE CONCAT('%', :searchTerm, '%') OR LOWER(c.entityName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY c.createdAt DESC")
    Page<Customer> findByShopIdsInAndSearchTermPaginated(@Param("shopIds") List<Long> shopIds, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.shop.id IN :shopIds AND c.isActive = :isActive AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR c.phone LIKE CONCAT('%', :searchTerm, '%') OR LOWER(c.entityName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY c.createdAt DESC")
    Page<Customer> findByShopIdsInStatusAndSearchTermPaginated(@Param("shopIds") List<Long> shopIds, @Param("isActive") boolean isActive, @Param("searchTerm") String searchTerm, Pageable pageable);
}

