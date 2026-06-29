package com.firesafety.platform.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrganizationServiceTest {
    private final MemoryEnterprises enterprises = new MemoryEnterprises();
    private final MemoryUsers users = new MemoryUsers();
    private final OrganizationService service = new OrganizationService(enterprises, users);
    private final SessionPrincipal operator =
            new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

    @Test
    void createsEnterpriseAndItsFirstMiniappAdministratorTogether() {
        var result = service.createEnterprise(
                operator, null, "示例总部", "联系人", "13800000000", "企业管理员", "13800000001");

        assertThat(result.enterprise().name()).isEqualTo("示例总部");
        assertThat(result.administrator())
                .extracting(UserAccount::enterpriseId, UserAccount::phone, UserAccount::role)
                .containsExactly(result.enterprise().id(), "13800000001", UserRole.ENTERPRISE_ADMIN);
        assertThat(result.administrator().username()).isNull();
    }

    @Test
    void rejectsDuplicateProvisionedPhone() {
        users.save(UserAccount.employee(88L, "已有员工", "13800000001", UserRole.EMPLOYEE));

        assertThatThrownBy(() -> service.createEnterprise(
                        operator, null, "示例总部", "联系人", "13800000000", "企业管理员", "13800000001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("PHONE_ALREADY_PROVISIONED"));
    }

    @Test
    void platformOperatorCannotDisablePlatformAdministrator() {
        var administrator = users.save(UserAccount.admin(
                "root", "unused", "超级管理员", UserRole.SUPER_ADMIN));
        var anotherOperator = new SessionPrincipal(99L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

        assertThatThrownBy(() -> service.disableUser(anotherOperator, administrator.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("PLATFORM_ACCOUNT_REQUIRES_SUPER_ADMIN"));
    }

    private static final class MemoryEnterprises implements EnterpriseRepository {
        private final Map<Long, Enterprise> values = new HashMap<>();
        private long sequence = 1;
        @Override public Enterprise save(Enterprise value) {
            if (value.id() == null) value.assignId(sequence++);
            values.put(value.id(), value); return value;
        }
        @Override public Optional<Enterprise> findById(Long id) { return Optional.ofNullable(values.get(id)); }
        @Override public List<Enterprise> findAll() { return List.copyOf(values.values()); }
    }

    private static final class MemoryUsers implements UserAccountRepository {
        private final List<UserAccount> values = new ArrayList<>();
        private long sequence = 1;
        @Override public UserAccount save(UserAccount value) {
            if (value.id() == null) value.assignId(sequence++);
            values.removeIf(existing -> existing.id().equals(value.id()));
            values.add(value); return value;
        }
        @Override public Optional<UserAccount> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<UserAccount> findByPhone(String phone) {
            return values.stream().filter(value -> phone.equals(value.phone())).findFirst();
        }
        @Override public Optional<UserAccount> findByOpenid(String openid) { return Optional.empty(); }
        @Override public Optional<UserAccount> findById(Long id) {
            return values.stream().filter(value -> value.id().equals(id)).findFirst();
        }
    }
}
