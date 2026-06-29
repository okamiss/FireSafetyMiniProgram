package com.firesafety.platform.permission;

import com.firesafety.platform.common.BusinessException;
import java.time.Instant;

public class PermissionRequest {
    private Long id;
    private final Long enterpriseId;
    private final Long applicantUserId;
    private final String requestedName;
    private final String requestedPhone;
    private final Instant createdAt;
    private PermissionRequestStatus status;
    private Long reviewerUserId;
    private String reviewRemark;
    private Instant reviewedAt;

    private PermissionRequest(
            Long enterpriseId, Long applicantUserId, String requestedName, String requestedPhone, Instant createdAt) {
        this.enterpriseId = enterpriseId;
        this.applicantUserId = applicantUserId;
        this.requestedName = requestedName;
        this.requestedPhone = requestedPhone;
        this.createdAt = createdAt;
        this.status = PermissionRequestStatus.PENDING;
    }

    public static PermissionRequest employee(
            Long enterpriseId, Long applicantUserId, String requestedName, String requestedPhone) {
        return new PermissionRequest(enterpriseId, applicantUserId, requestedName, requestedPhone, Instant.now());
    }

    public static PermissionRequest restore(
            Long id,
            Long enterpriseId,
            Long applicantUserId,
            String requestedName,
            String requestedPhone,
            Instant createdAt,
            PermissionRequestStatus status,
            Long reviewerUserId,
            String reviewRemark,
            Instant reviewedAt) {
        var request = new PermissionRequest(
                enterpriseId, applicantUserId, requestedName, requestedPhone, createdAt);
        request.id = id;
        request.status = status;
        request.reviewerUserId = reviewerUserId;
        request.reviewRemark = reviewRemark;
        request.reviewedAt = reviewedAt;
        return request;
    }

    public void approve(Long reviewerUserId, String remark) {
        ensurePending();
        status = PermissionRequestStatus.APPROVED;
        this.reviewerUserId = reviewerUserId;
        this.reviewRemark = remark;
        this.reviewedAt = Instant.now();
    }

    public void reject(Long reviewerUserId, String remark) {
        ensurePending();
        status = PermissionRequestStatus.REJECTED;
        this.reviewerUserId = reviewerUserId;
        this.reviewRemark = remark;
        this.reviewedAt = Instant.now();
    }

    private void ensurePending() {
        if (status != PermissionRequestStatus.PENDING) {
            throw new BusinessException("INVALID_REQUEST_STATUS", "该权限申请已完成审核");
        }
    }

    public void assignId(long id) {
        if (this.id != null) throw new IllegalStateException("申请编号已经分配");
        this.id = id;
    }

    public Long id() { return id; }
    public Long enterpriseId() { return enterpriseId; }
    public Long applicantUserId() { return applicantUserId; }
    public String requestedName() { return requestedName; }
    public String requestedPhone() { return requestedPhone; }
    public Instant createdAt() { return createdAt; }
    public PermissionRequestStatus status() { return status; }
    public Long reviewerUserId() { return reviewerUserId; }
    public String reviewRemark() { return reviewRemark; }
    public Instant reviewedAt() { return reviewedAt; }
}
