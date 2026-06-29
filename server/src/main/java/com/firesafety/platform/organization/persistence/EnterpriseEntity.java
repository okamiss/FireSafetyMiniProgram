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
@Table(name = "enterprise")
class EnterpriseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "parent_id") Long parentId;
    @Column(nullable = false, length = 32) String type;
    @Column(nullable = false, length = 200) String name;
    @Column(name = "contact_name", length = 100) String contactName;
    @Column(name = "contact_phone", length = 32) String contactPhone;
    @Column(nullable = false, length = 32) String status;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Version long version;

    protected EnterpriseEntity() {}
}
