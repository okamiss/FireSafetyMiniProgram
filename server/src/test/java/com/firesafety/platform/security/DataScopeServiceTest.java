package com.firesafety.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DataScopeServiceTest {

    private final EnterpriseScopeResolver resolver = new EnterpriseScopeResolver() {
        @Override
        public Set<Long> allEnterpriseIds() {
            return Set.of(10L, 11L, 12L);
        }

        @Override
        public Set<Long> descendantsIncludingSelf(long enterpriseId) {
            return enterpriseId == 10L ? Set.of(10L, 11L) : Set.of(enterpriseId);
        }
    };

    private final DataScopeService service = new DataScopeService(resolver);

    @Test
    void platformRolesCanSeeAllEnterprises() {
        var principal = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "operator");

        assertThat(service.visibleEnterpriseIds(principal)).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void enterpriseAdministratorCanSeeOwnOrganizationTree() {
        var principal = new SessionPrincipal(2L, UserRole.ENTERPRISE_ADMIN, 10L, "manager");

        assertThat(service.visibleEnterpriseIds(principal)).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void employeeCanOnlySeeOwnEnterprise() {
        var principal = new SessionPrincipal(3L, UserRole.EMPLOYEE, 11L, "employee");

        assertThat(service.visibleEnterpriseIds(principal)).containsExactly(11L);
    }
}
