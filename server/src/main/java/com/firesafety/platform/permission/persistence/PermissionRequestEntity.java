package com.firesafety.platform.permission.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "permission_request")
class PermissionRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "enterprise_id", nullable = false) Long enterpriseId;
    @Column(name = "applicant_user_id", nullable = false) Long applicantUserId;
    @Column(name = "requested_name", nullable = false, length = 100) String requestedName;
    @Column(name = "requested_phone", nullable = false, length = 32) String requestedPhone;
    @Column(name = "pending_phone", length = 32) String pendingPhone;
    @Column(nullable = false, length = 32) String status;
    @Column(name = "reviewer_user_id") Long reviewerUserId;
    @Column(name = "review_remark", length = 500) String reviewRemark;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "reviewed_at") Instant reviewedAt;
    @Version long version;

    protected PermissionRequestEntity() {}
}
