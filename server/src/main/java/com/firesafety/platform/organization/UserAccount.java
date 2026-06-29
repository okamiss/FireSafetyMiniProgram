package com.firesafety.platform.organization;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;

public class UserAccount {
    private Long id;
    private final Long enterpriseId;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String phone;
    private String openid;
    private final UserRole role;
    private boolean enabled;

    private UserAccount(
            Long enterpriseId,
            String username,
            String passwordHash,
            String displayName,
            String phone,
            UserRole role) {
        this.enterpriseId = enterpriseId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phone = phone;
        this.role = role;
        this.enabled = true;
    }

    public static UserAccount admin(String username, String passwordHash, String displayName, UserRole role) {
        if (role != UserRole.SUPER_ADMIN && role != UserRole.PLATFORM_OPERATOR) {
            throw new IllegalArgumentException("后台账号只能使用平台角色");
        }
        return new UserAccount(null, username, passwordHash, displayName, null, role);
    }

    public static UserAccount employee(Long enterpriseId, String displayName, String phone, UserRole role) {
        if (enterpriseId == null || (role != UserRole.ENTERPRISE_ADMIN && role != UserRole.EMPLOYEE)) {
            throw new IllegalArgumentException("小程序账号必须绑定企业和企业角色");
        }
        return new UserAccount(enterpriseId, null, null, displayName, phone, role);
    }

    public static UserAccount restore(
            Long id,
            Long enterpriseId,
            String username,
            String passwordHash,
            String displayName,
            String phone,
            String openid,
            UserRole role,
            boolean enabled) {
        var user = new UserAccount(enterpriseId, username, passwordHash, displayName, phone, role);
        user.id = id;
        user.openid = openid;
        user.enabled = enabled;
        return user;
    }

    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("账号编号已经分配");
        }
        this.id = id;
    }

    public void bindOpenid(String openid) {
        if (this.openid != null && !this.openid.equals(openid)) {
            throw new BusinessException("PHONE_ALREADY_BOUND", "该手机号已绑定其他微信账号");
        }
        this.openid = openid;
    }

    public SessionPrincipal toPrincipal() {
        return new SessionPrincipal(id, role, enterpriseId, displayName);
    }

    public Long id() { return id; }
    public Long enterpriseId() { return enterpriseId; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public String displayName() { return displayName; }
    public String phone() { return phone; }
    public String openid() { return openid; }
    public UserRole role() { return role; }
    public boolean enabled() { return enabled; }
    public void disable() { enabled = false; }
}
