package com.firesafety.platform.permission;

import com.firesafety.platform.audit.AuditAction;
import com.firesafety.platform.audit.AuditLogPort;
import com.firesafety.platform.audit.AuditModule;
import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Transactional
public class PermissionRequestService {
    private final PermissionRequestRepository requests;
    private final EnterpriseRepository enterprises;
    private final UserAccountRepository users;
    private final PermissionNotificationPort notifications;
    private final AuditLogPort audit;

    public PermissionRequestService(
            PermissionRequestRepository requests,
            EnterpriseRepository enterprises,
            UserAccountRepository users,
            PermissionNotificationPort notifications,
            AuditLogPort audit) {
        this.requests = requests;
        this.enterprises = enterprises;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
    }

    public PermissionRequest requestEmployee(SessionPrincipal applicant, String name, String phone) {
        if (applicant.role() != UserRole.ENTERPRISE_ADMIN || applicant.enterpriseId() == null) {
            throw new BusinessException("FORBIDDEN", "只有企业管理员可以提交员工权限申请", HttpStatus.FORBIDDEN);
        }
        enterprises.findById(applicant.enterpriseId())
                .orElseThrow(() -> new BusinessException("ENTERPRISE_NOT_FOUND", "所属企业不存在"));
        if (users.findByPhone(phone).isPresent()) {
            throw new BusinessException("PHONE_ALREADY_PROVISIONED", "该手机号已经开通权限");
        }
        if (requests.existsPendingByPhone(phone)) {
            throw new BusinessException("DUPLICATE_PENDING_REQUEST", "该手机号已有待审核申请");
        }
        return requests.save(PermissionRequest.employee(
                applicant.enterpriseId(), applicant.userId(), name, phone));
    }

    public PermissionRequest approve(SessionPrincipal operator, Long requestId, String remark) {
        return approve(operator, requestId, remark, null);
    }

    public PermissionRequest approve(
            SessionPrincipal operator, Long requestId, String remark, String ipAddress) {
        requirePlatformOperator(operator);
        var request = findForReview(requestId);
        ensurePending(request);
        if (users.findByPhone(request.requestedPhone()).isPresent()) {
            throw new BusinessException("PHONE_ALREADY_PROVISIONED", "该手机号已经开通权限");
        }
        request.approve(operator.userId(), remark);
        users.save(UserAccount.employee(
                request.enterpriseId(), request.requestedName(), request.requestedPhone(), UserRole.EMPLOYEE));
        var saved = requests.save(request);
        notifications.approved(saved);
        audit.record(operator, AuditModule.PERMISSION, AuditAction.APPROVE,
                saved.id(), "权限申请已通过", ipAddress);
        return saved;
    }

    public PermissionRequest reject(SessionPrincipal operator, Long requestId, String reason) {
        return reject(operator, requestId, reason, null);
    }

    public PermissionRequest reject(
            SessionPrincipal operator, Long requestId, String reason, String ipAddress) {
        requirePlatformOperator(operator);
        var request = findForReview(requestId);
        ensurePending(request);
        request.reject(operator.userId(), reason);
        var saved = requests.save(request);
        notifications.rejected(saved);
        audit.record(operator, AuditModule.PERMISSION, AuditAction.REJECT,
                saved.id(), "权限申请已驳回", ipAddress);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PermissionRequest> list(SessionPrincipal principal) {
        if (principal.role() == UserRole.SUPER_ADMIN || principal.role() == UserRole.PLATFORM_OPERATOR) {
            return requests.findAll();
        }
        if (principal.role() == UserRole.ENTERPRISE_ADMIN && principal.enterpriseId() != null) {
            return requests.findByEnterpriseId(principal.enterpriseId());
        }
        throw new BusinessException("FORBIDDEN", "没有权限查看申请", HttpStatus.FORBIDDEN);
    }

    private PermissionRequest find(Long requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new BusinessException("REQUEST_NOT_FOUND", "权限申请不存在", HttpStatus.NOT_FOUND));
    }

    private PermissionRequest findForReview(Long requestId) {
        return requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException("REQUEST_NOT_FOUND", "权限申请不存在", HttpStatus.NOT_FOUND));
    }

    private void ensurePending(PermissionRequest request) {
        if (request.status() != PermissionRequestStatus.PENDING) {
            throw new BusinessException("INVALID_REQUEST_STATUS", "该权限申请已完成审核");
        }
    }

    private void requirePlatformOperator(SessionPrincipal operator) {
        if (operator.role() != UserRole.SUPER_ADMIN && operator.role() != UserRole.PLATFORM_OPERATOR) {
            throw new BusinessException("FORBIDDEN", "没有权限审核该申请", HttpStatus.FORBIDDEN);
        }
    }
}
