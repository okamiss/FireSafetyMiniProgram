package com.firesafety.platform.organization.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "sys_user")
class UserAccountEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "enterprise_id") Long enterpriseId;
    @Column(length = 100) String username;
    @Column(name = "password_hash") String passwordHash;
    @Column(length = 128) String openid;
    @Column(name = "name", nullable = false, length = 100) String displayName;
    @Column(length = 32) String phone;
    @Column(nullable = false, length = 32) String role;
    @Column(nullable = false, length = 32) String status;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Version long version;

    protected UserAccountEntity() {}
}
