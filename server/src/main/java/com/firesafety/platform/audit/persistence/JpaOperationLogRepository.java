package com.firesafety.platform.audit.persistence;

import com.firesafety.platform.audit.AuditAction;
import com.firesafety.platform.audit.AuditModule;
import com.firesafety.platform.audit.AuditResult;
import com.firesafety.platform.audit.OperationLog;
import com.firesafety.platform.audit.OperationLogRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOperationLogRepository implements OperationLogRepository {
    private final OperationLogJpaRepository jpa;

    public JpaOperationLogRepository(OperationLogJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public OperationLog save(OperationLog value) {
        var entity = new OperationLogEntity();
        entity.enterpriseId = value.enterpriseId();
        entity.operatorId = value.operatorId();
        entity.module = value.module().name();
        entity.action = value.action().name();
        entity.businessId = value.businessId();
        entity.result = value.result().name();
        entity.detail = value.detail();
        entity.ipAddress = value.ipAddress();
        entity.createdAt = value.createdAt();
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<OperationLog> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    private OperationLog toDomain(OperationLogEntity entity) {
        return new OperationLog(entity.id, entity.enterpriseId, entity.operatorId,
                AuditModule.valueOf(entity.module), AuditAction.valueOf(entity.action), entity.businessId,
                AuditResult.valueOf(entity.result), entity.detail, entity.ipAddress, entity.createdAt);
    }
}
