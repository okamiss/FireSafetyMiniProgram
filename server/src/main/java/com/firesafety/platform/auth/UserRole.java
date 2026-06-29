package com.firesafety.platform.auth;

public enum UserRole {
    SUPER_ADMIN,
    PLATFORM_OPERATOR,
    ENTERPRISE_ADMIN,
    EMPLOYEE;

    public String authority() {
        return "ROLE_" + name();
    }
}
