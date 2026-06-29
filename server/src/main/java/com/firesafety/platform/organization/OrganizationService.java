package com.firesafety.platform.organization;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OrganizationService {
    private final EnterpriseRepository enterprises;
    private final UserAccountRepository users;

    public OrganizationService(EnterpriseRepository enterprises, UserAccountRepository users) {
        this.enterprises = enterprises;
        this.users = users;
    }

    public EnterpriseCreationResult createEnterprise(
            SessionPrincipal operator,
            Long parentId,
            String name,
            String contactName,
            String contactPhone,
            String administratorName,
            String administratorPhone) {
        requirePlatformRole(operator);
        if (users.findByPhone(administratorPhone).isPresent()) {
            throw new BusinessException("PHONE_ALREADY_PROVISIONED", "管理员手机号已经开通权限");
        }
        Enterprise enterprise;
        if (parentId == null) {
            enterprise = Enterprise.headquarters(name, contactName, contactPhone);
        } else {
            enterprises.findById(parentId)
                    .orElseThrow(() -> new BusinessException("PARENT_ENTERPRISE_NOT_FOUND", "上级企业不存在"));
            enterprise = Enterprise.branch(parentId, name, contactName, contactPhone);
        }
        var savedEnterprise = enterprises.save(enterprise);
        var administrator = users.save(UserAccount.employee(
                savedEnterprise.id(), administratorName, administratorPhone, UserRole.ENTERPRISE_ADMIN));
        return new EnterpriseCreationResult(savedEnterprise, administrator);
    }

    @Transactional(readOnly = true)
    public List<Enterprise> listEnterprises(SessionPrincipal operator) {
        requirePlatformRole(operator);
        return enterprises.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listEnterpriseUsers(SessionPrincipal operator, Long enterpriseId) {
        requirePlatformRole(operator);
        enterprises.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException("ENTERPRISE_NOT_FOUND", "企业不存在", HttpStatus.NOT_FOUND));
        return users.findByEnterpriseId(enterpriseId);
    }

    public UserAccount disableUser(SessionPrincipal operator, Long userId) {
        requirePlatformRole(operator);
        var user = users.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
        if (user.id().equals(operator.userId())) {
            throw new BusinessException("SELF_DISABLE_NOT_ALLOWED", "不能停用当前登录账号");
        }
        if ((user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.PLATFORM_OPERATOR)
                && operator.role() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(
                    "PLATFORM_ACCOUNT_REQUIRES_SUPER_ADMIN", "只有超级管理员可以停用平台账号", HttpStatus.FORBIDDEN);
        }
        user.disable();
        return users.save(user);
    }

    private void requirePlatformRole(SessionPrincipal operator) {
        if (operator == null || (operator.role() != UserRole.SUPER_ADMIN
                && operator.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有企业管理权限", HttpStatus.FORBIDDEN);
        }
    }
}
