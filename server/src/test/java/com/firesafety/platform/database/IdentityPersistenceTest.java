package com.firesafety.platform.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.permission.PermissionRequest;
import com.firesafety.platform.permission.PermissionRequestRepository;
import com.firesafety.platform.permission.PermissionRequestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityPersistenceTest {
    @Autowired private EnterpriseRepository enterprises;
    @Autowired private UserAccountRepository users;
    @Autowired private PermissionRequestRepository requests;

    @Test
    void persistsEnterpriseUserAndReviewedPermissionRequest() {
        var enterprise = enterprises.save(Enterprise.headquarters("示例总部", "联系人", "13800000000"));
        var admin = users.save(UserAccount.employee(
                enterprise.id(), "企业管理员", "13800000001", UserRole.ENTERPRISE_ADMIN));
        var operator = users.save(UserAccount.admin(
                "operator", "unused", "平台运营", UserRole.PLATFORM_OPERATOR));
        var request = requests.save(PermissionRequest.employee(
                enterprise.id(), admin.id(), "李四", "13900000000"));

        request.approve(operator.id(), "资料已核验");
        requests.save(request);

        assertThat(enterprises.findById(enterprise.id())).get()
                .extracting(Enterprise::name, Enterprise::contactPhone)
                .containsExactly("示例总部", "13800000000");
        assertThat(users.findByPhone("13800000001")).get()
                .extracting(UserAccount::enterpriseId, UserAccount::role)
                .containsExactly(enterprise.id(), UserRole.ENTERPRISE_ADMIN);
        assertThat(requests.findById(request.id())).get()
                .extracting(PermissionRequest::status, PermissionRequest::reviewerUserId)
                .containsExactly(PermissionRequestStatus.APPROVED, operator.id());
    }

    @Test
    void databaseRejectsTwoPendingRequestsForSamePhone() {
        var enterprise = enterprises.save(Enterprise.headquarters("示例总部", "联系人", "13800000000"));
        var admin = users.save(UserAccount.employee(
                enterprise.id(), "企业管理员", "13800000001", UserRole.ENTERPRISE_ADMIN));
        requests.save(PermissionRequest.employee(enterprise.id(), admin.id(), "李四", "13900000000"));

        assertThatThrownBy(() -> requests.save(
                        PermissionRequest.employee(enterprise.id(), admin.id(), "李四", "13900000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code())
                        .isEqualTo("DUPLICATE_PENDING_REQUEST"));
    }
}
