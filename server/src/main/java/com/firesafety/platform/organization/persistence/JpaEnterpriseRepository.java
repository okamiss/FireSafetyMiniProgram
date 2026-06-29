package com.firesafety.platform.organization.persistence;

import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEnterpriseRepository implements EnterpriseRepository {
    private final EnterpriseJpaRepository jpa;

    public JpaEnterpriseRepository(EnterpriseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Enterprise save(Enterprise enterprise) {
        var entity = enterprise.id() == null
                ? new EnterpriseEntity()
                : jpa.findById(enterprise.id()).orElseThrow();
        var now = Instant.now();
        if (entity.createdAt == null) entity.createdAt = now;
        entity.updatedAt = now;
        entity.parentId = enterprise.parentId();
        entity.type = enterprise.parentId() == null ? "HEADQUARTERS" : "BRANCH";
        entity.name = enterprise.name();
        entity.contactName = enterprise.contactName();
        entity.contactPhone = enterprise.contactPhone();
        entity.status = enterprise.enabled() ? "ENABLED" : "DISABLED";
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Enterprise> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Enterprise> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private Enterprise toDomain(EnterpriseEntity entity) {
        return Enterprise.restore(
                entity.id,
                entity.parentId,
                entity.name,
                entity.contactName,
                entity.contactPhone,
                "ENABLED".equals(entity.status));
    }
}
