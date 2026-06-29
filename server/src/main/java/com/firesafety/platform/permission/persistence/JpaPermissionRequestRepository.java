package com.firesafety.platform.permission.persistence;

import com.firesafety.platform.permission.PermissionRequest;
import com.firesafety.platform.permission.PermissionRequestRepository;
import com.firesafety.platform.permission.PermissionRequestStatus;
import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import com.firesafety.platform.common.BusinessException;

@Repository
public class JpaPermissionRequestRepository implements PermissionRequestRepository {
    private final PermissionRequestJpaRepository jpa;

    public JpaPermissionRequestRepository(PermissionRequestJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PermissionRequest save(PermissionRequest request) {
        var entity = request.id() == null
                ? new PermissionRequestEntity()
                : jpa.findById(request.id()).orElseThrow();
        entity.enterpriseId = request.enterpriseId();
        entity.applicantUserId = request.applicantUserId();
        entity.requestedName = request.requestedName();
        entity.requestedPhone = request.requestedPhone();
        entity.pendingPhone = request.status() == PermissionRequestStatus.PENDING
                ? request.requestedPhone() : null;
        entity.status = request.status().name();
        entity.reviewerUserId = request.reviewerUserId();
        entity.reviewRemark = request.reviewRemark();
        entity.createdAt = request.createdAt();
        entity.reviewedAt = request.reviewedAt();
        try {
            return toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (request.status() == PermissionRequestStatus.PENDING) {
                throw new BusinessException("DUPLICATE_PENDING_REQUEST", "该手机号已有待审核申请");
            }
            throw exception;
        }
    }

    @Override
    public Optional<PermissionRequest> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PermissionRequest> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override public List<PermissionRequest> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override public List<PermissionRequest> findByEnterpriseId(Long enterpriseId) {
        return jpa.findAllByEnterpriseIdOrderByCreatedAtDesc(enterpriseId).stream().map(this::toDomain).toList();
    }

    @Override public boolean existsPendingByPhone(String phone) {
        return jpa.existsByRequestedPhoneAndStatus(phone, PermissionRequestStatus.PENDING.name());
    }

    private PermissionRequest toDomain(PermissionRequestEntity entity) {
        return PermissionRequest.restore(
                entity.id,
                entity.enterpriseId,
                entity.applicantUserId,
                entity.requestedName,
                entity.requestedPhone,
                entity.createdAt,
                PermissionRequestStatus.valueOf(entity.status),
                entity.reviewerUserId,
                entity.reviewRemark,
                entity.reviewedAt);
    }
}
