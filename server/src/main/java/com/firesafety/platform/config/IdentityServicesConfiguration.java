package com.firesafety.platform.config;

import com.firesafety.platform.auth.AdminAuthenticationService;
import com.firesafety.platform.auth.SessionService;
import com.firesafety.platform.auth.WeChatAuthenticationService;
import com.firesafety.platform.auth.WeChatBindingTicketStore;
import com.firesafety.platform.auth.WeChatIdentityProvider;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.organization.OrganizationService;
import com.firesafety.platform.permission.PermissionNotificationPort;
import com.firesafety.platform.permission.PermissionRequestRepository;
import com.firesafety.platform.permission.PermissionRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class IdentityServicesConfiguration {
    @Bean
    AdminAuthenticationService adminAuthenticationService(
            UserAccountRepository users, PasswordEncoder encoder, SessionService sessions) {
        return new AdminAuthenticationService(users, encoder, sessions);
    }

    @Bean
    WeChatAuthenticationService weChatAuthenticationService(
            UserAccountRepository users,
            WeChatIdentityProvider identityProvider,
            WeChatBindingTicketStore bindingTickets,
            SessionService sessions) {
        return new WeChatAuthenticationService(users, identityProvider, bindingTickets, sessions);
    }

    @Bean
    PermissionRequestService permissionRequestService(
            PermissionRequestRepository requests,
            EnterpriseRepository enterprises,
            UserAccountRepository users,
            PermissionNotificationPort notifications) {
        return new PermissionRequestService(requests, enterprises, users, notifications);
    }

    @Bean
    OrganizationService organizationService(EnterpriseRepository enterprises, UserAccountRepository users) {
        return new OrganizationService(enterprises, users);
    }
}
