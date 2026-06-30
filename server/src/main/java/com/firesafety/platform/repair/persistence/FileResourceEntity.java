package com.firesafety.platform.repair.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "file_resource")
class FileResourceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "enterprise_id") Long enterpriseId;
    @Column(name = "business_type", nullable = false, length = 64) String businessType;
    @Column(name = "business_id") Long businessId;
    @Column(name = "storage_key", nullable = false, length = 500) String storageKey;
    @Column(name = "original_name", nullable = false, length = 255) String originalName;
    @Column(name = "content_type", nullable = false, length = 128) String contentType;
    @Column(name = "file_size", nullable = false) long fileSize;
    @Column(name = "uploader_id", nullable = false) Long uploaderId;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected FileResourceEntity() {}
}
