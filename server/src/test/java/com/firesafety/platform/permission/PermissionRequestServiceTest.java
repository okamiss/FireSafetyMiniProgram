package com.firesafety.platform.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PermissionRequestServiceTest {

    @Test
    void enterpriseAdminRequestsEmployeeAndOperatorApprovalCreatesAccount() {
        var enterprises = new MemoryEnterprises();
        var users = new MemoryUsers();
        var requests = new MemoryRequests();
        var notices = new RecordingPermissionNotifications();
        var headquarters = enterprises.save(Enterprise.headquarters("示例总部", "联系人", "13800000000"));
        var service = new PermissionRequestService(requests, enterprises, users, notices);
        var applicant = new SessionPrincipal(11L, UserRole.ENTERPRISE_ADMIN, headquarters.id(), "企业管理员");
        var operator = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

        var request = service.requestEmployee(applicant, "李四", "13900000000");
        var approved = service.approve(operator, request.id(), "资料已核验");

        assertThat(approved.status()).isEqualTo(PermissionRequestStatus.APPROVED);
        assertThat(users.findByPhone("13900000000")).get()
                .extracting(UserAccount::enterpriseId, UserAccount::role)
                .containsExactly(headquarters.id(), UserRole.EMPLOYEE);
        assertThat(notices.events).containsExactly("APPROVED:" + request.id());

        assertThatThrownBy(() -> service.approve(operator, request.id(), "重复审批"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("INVALID_REQUEST_STATUS"));
    }

    @Test
    void completedRequestCannotBeReviewedAgain() {
        var enterprises = new MemoryEnterprises();
        var requests = new MemoryRequests();
        var headquarters = enterprises.save(Enterprise.headquarters("示例总部", "联系人", "13800000000"));
        var service = new PermissionRequestService(
                requests, enterprises, new MemoryUsers(), new RecordingPermissionNotifications());
        var applicant = new SessionPrincipal(11L, UserRole.ENTERPRISE_ADMIN, headquarters.id(), "企业管理员");
        var operator = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");
        var request = service.requestEmployee(applicant, "李四", "13900000000");
        service.reject(operator, request.id(), "手机号有误");

        assertThatThrownBy(() -> service.approve(operator, request.id(), "再次审批"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("INVALID_REQUEST_STATUS"));
    }

    @Test
    void duplicatePendingRequestForSamePhoneIsRejected() {
        var enterprises = new MemoryEnterprises();
        var requests = new MemoryRequests();
        var headquarters = enterprises.save(Enterprise.headquarters("示例总部", "联系人", "13800000000"));
        var service = new PermissionRequestService(
                requests, enterprises, new MemoryUsers(), new RecordingPermissionNotifications());
        var applicant = new SessionPrincipal(11L, UserRole.ENTERPRISE_ADMIN, headquarters.id(), "企业管理员");
        service.requestEmployee(applicant, "李四", "13900000000");

        assertThatThrownBy(() -> service.requestEmployee(applicant, "李四", "13900000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("DUPLICATE_PENDING_REQUEST"));
    }

    private static final class RecordingPermissionNotifications implements PermissionNotificationPort {
        private final List<String> events = new ArrayList<>();
        @Override public void approved(PermissionRequest request) { events.add("APPROVED:" + request.id()); }
        @Override public void rejected(PermissionRequest request) { events.add("REJECTED:" + request.id()); }
    }

    private static final class MemoryRequests implements PermissionRequestRepository {
        private final Map<Long, PermissionRequest> values = new HashMap<>();
        private long sequence = 1;
        @Override public PermissionRequest save(PermissionRequest request) {
            if (request.id() == null) request.assignId(sequence++);
            values.put(request.id(), request); return request;
        }
        @Override public Optional<PermissionRequest> findById(Long id) { return Optional.ofNullable(values.get(id)); }
        @Override public boolean existsPendingByPhone(String phone) {
            return values.values().stream().anyMatch(request ->
                    phone.equals(request.requestedPhone()) && request.status() == PermissionRequestStatus.PENDING);
        }
    }

    private static final class MemoryEnterprises implements EnterpriseRepository {
        private final Map<Long, Enterprise> values = new HashMap<>();
        private long sequence = 1;
        @Override public Enterprise save(Enterprise enterprise) {
            if (enterprise.id() == null) enterprise.assignId(sequence++);
            values.put(enterprise.id(), enterprise); return enterprise;
        }
        @Override public Optional<Enterprise> findById(Long id) { return Optional.ofNullable(values.get(id)); }
        @Override public List<Enterprise> findAll() { return List.copyOf(values.values()); }
    }

    private static final class MemoryUsers implements UserAccountRepository {
        private final Map<Long, UserAccount> values = new HashMap<>();
        private long sequence = 1;
        @Override public UserAccount save(UserAccount user) {
            if (user.id() == null) user.assignId(sequence++);
            values.put(user.id(), user); return user;
        }
        @Override public Optional<UserAccount> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<UserAccount> findByPhone(String phone) {
            return values.values().stream().filter(user -> phone.equals(user.phone())).findFirst();
        }
        @Override public Optional<UserAccount> findByOpenid(String openid) { return Optional.empty(); }
        @Override public Optional<UserAccount> findById(Long id) { return Optional.ofNullable(values.get(id)); }
    }
}
