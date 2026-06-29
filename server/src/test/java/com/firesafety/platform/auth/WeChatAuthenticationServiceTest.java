package com.firesafety.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.support.InMemorySessionStore;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WeChatAuthenticationServiceTest {

    @Test
    void bindsAuthorizedPhoneToPreProvisionedEmployeeAndIssuesSession() {
        var users = new MemoryUsers();
        users.save(UserAccount.employee(20L, "张三", "13800000000", UserRole.EMPLOYEE));
        var sessions = new SessionService(
                new InMemorySessionStore(), Clock.systemUTC(), Duration.ofHours(2), Duration.ofDays(30));
        var tickets = new MemoryBindingTickets();
        var provider = new WeChatIdentityProvider() {
            @Override
            public String exchangeLoginCode(String loginCode) {
                return "openid-zhangsan";
            }

            @Override
            public String exchangePhoneCode(String phoneCode) {
                return "13800000000";
            }
        };
        var service = new WeChatAuthenticationService(users, provider, tickets, sessions);

        var login = service.login("wx-login-code");
        assertThat(login.bindingRequired()).isTrue();

        var bound = service.bindPhone(login.bindingTicket(), "wx-phone-code");

        assertThat(bound.user().enterpriseId()).isEqualTo(20L);
        assertThat(users.findByOpenid("openid-zhangsan")).isPresent();
        assertThat(sessions.resolve(bound.tokens().accessToken())).contains(bound.user());
    }

    @Test
    void disabledBoundAccountCannotRestartPhoneBinding() {
        var users = new MemoryUsers();
        var account = UserAccount.employee(20L, "张三", "13800000000", UserRole.EMPLOYEE);
        account.bindOpenid("openid-disabled");
        account.disable();
        users.save(account);
        var service = new WeChatAuthenticationService(
                users,
                new WeChatIdentityProvider() {
                    @Override public String exchangeLoginCode(String loginCode) { return "openid-disabled"; }
                    @Override public String exchangePhoneCode(String phoneCode) { return "13800000000"; }
                },
                new MemoryBindingTickets(),
                new SessionService(new InMemorySessionStore(), Clock.systemUTC(),
                        Duration.ofHours(2), Duration.ofDays(30)));

        assertThatThrownBy(() -> service.login("code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("ACCOUNT_DISABLED"));
    }

    private static final class MemoryBindingTickets implements WeChatBindingTicketStore {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public String create(String openid) {
            var ticket = "ticket-" + openid;
            values.put(ticket, openid);
            return ticket;
        }

        @Override
        public Optional<String> consume(String ticket) {
            return Optional.ofNullable(values.remove(ticket));
        }
    }

    private static final class MemoryUsers implements UserAccountRepository {
        private final Map<Long, UserAccount> values = new HashMap<>();
        private long sequence = 1;

        @Override
        public UserAccount save(UserAccount user) {
            if (user.id() == null) user.assignId(sequence++);
            values.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) { return Optional.empty(); }

        @Override
        public Optional<UserAccount> findByPhone(String phone) {
            return values.values().stream().filter(user -> phone.equals(user.phone())).findFirst();
        }

        @Override
        public Optional<UserAccount> findByOpenid(String openid) {
            return values.values().stream().filter(user -> openid.equals(user.openid())).findFirst();
        }

        @Override
        public Optional<UserAccount> findById(Long id) { return Optional.ofNullable(values.get(id)); }
    }
}
