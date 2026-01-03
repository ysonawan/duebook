package com.duebook.app.repository;

import com.duebook.app.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query("SELECT s FROM Shop s " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY s.createdAt DESC")
    List<Shop> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT s FROM Shop s " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND s.id = :shopId AND su.status = 'ACTIVE'")
    Optional<Shop> findByIdAndUserId(@Param("shopId") Long shopId, @Param("userId") Long userId);

    @Query("SELECT s FROM Shop s " +
           "INNER JOIN ShopUser su ON s.id = su.shop.id " +
           "WHERE su.user.id = :userId AND su.status = 'ACTIVE' " +
           "ORDER BY s.createdAt DESC")
    List<Shop> findByUserId(@Param("userId") Long userId);
}

