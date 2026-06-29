package com.firesafety.platform.security;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import java.util.Set;

public class DataScopeService {
    private final EnterpriseScopeResolver resolver;

    public DataScopeService(EnterpriseScopeResolver resolver) {
        this.resolver = resolver;
    }

    public Set<Long> visibleEnterpriseIds(SessionPrincipal principal) {
        if (principal.role() == UserRole.SUPER_ADMIN || principal.role() == UserRole.PLATFORM_OPERATOR) {
            return resolver.allEnterpriseIds();
        }
        if (principal.enterpriseId() == null) {
            return Set.of();
        }
        if (principal.role() == UserRole.ENTERPRISE_ADMIN) {
            return resolver.descendantsIncludingSelf(principal.enterpriseId());
        }
        return Set.of(principal.enterpriseId());
    }
}
