package com.duebook.app.repository;

import com.duebook.app.model.ShopUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopUserRepository extends JpaRepository<ShopUser, Long> {

    @Query("SELECT su FROM ShopUser su WHERE su.shop.id = :shopId AND su.user.id = :userId")
    Optional<ShopUser> findByShopIdAndUserId(@Param("shopId") Long shopId, @Param("userId") Long userId);

    @Query("SELECT su FROM ShopUser su WHERE su.user.id = :userId AND su.status = 'ACTIVE'")
    List<ShopUser> findAllActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT su FROM ShopUser su WHERE su.shop.id = :shopId AND su.status = 'ACTIVE'")
    List<ShopUser> findAllActiveByShopId(@Param("shopId") Long shopId);

    @Query("SELECT su FROM ShopUser su WHERE su.shop.id = :shopId AND su.user.id = :userId AND su.role = 'OWNER'")
    Optional<ShopUser> findOwnerByShopIdAndUserId(@Param("shopId") Long shopId, @Param("userId") Long userId);

    @Query("SELECT COUNT(su) > 0 FROM ShopUser su WHERE su.shop.id = :shopId AND su.user.id = :userId")
    boolean existsByShopIdAndUserId(@Param("shopId") Long shopId, @Param("userId") Long userId);
}

