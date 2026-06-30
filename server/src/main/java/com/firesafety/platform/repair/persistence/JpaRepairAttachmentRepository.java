package com.firesafety.platform.repair.persistence;

import com.firesafety.platform.repair.RepairAttachment;
import com.firesafety.platform.repair.RepairAttachmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRepairAttachmentRepository implements RepairAttachmentRepository {
    private static final String BUSINESS_TYPE = "REPAIR";
    private final FileResourceJpaRepository jpa;

    public JpaRepairAttachmentRepository(FileResourceJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public RepairAttachment save(RepairAttachment value) {
        var entity = new FileResourceEntity();
        entity.enterpriseId = value.enterpriseId();
        entity.businessType = BUSINESS_TYPE;
        entity.businessId = value.repairId();
        entity.storageKey = value.storageKey();
        entity.originalName = value.originalName();
        entity.contentType = value.contentType();
        entity.fileSize = value.fileSize();
        entity.uploaderId = value.uploaderUserId();
        entity.createdAt = value.createdAt();
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<RepairAttachment> findByRepairId(Long repairId) {
        return jpa.findAllByBusinessTypeAndBusinessIdOrderByCreatedAtAsc(BUSINESS_TYPE, repairId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<RepairAttachment> findById(Long id) {
        return jpa.findByIdAndBusinessType(id, BUSINESS_TYPE).map(this::toDomain);
    }

    private RepairAttachment toDomain(FileResourceEntity entity) {
        return new RepairAttachment(entity.id, entity.enterpriseId, entity.businessId, entity.uploaderId,
                entity.storageKey, entity.originalName, entity.contentType, entity.fileSize, entity.createdAt);
    }
}
