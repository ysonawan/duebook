package com.duebook.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_users", schema = "duebook_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ShopUserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShopUserStatus status = ShopUserStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public enum ShopUserRole {
        OWNER, STAFF, VIEWER
    }

    public enum ShopUserStatus {
        ACTIVE, INVITED, INACTIVE
    }
}

