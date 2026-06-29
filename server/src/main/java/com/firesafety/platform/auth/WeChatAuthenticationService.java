package com.firesafety.platform.auth;

import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import org.springframework.http.HttpStatus;

public class WeChatAuthenticationService {
    private final UserAccountRepository users;
    private final WeChatIdentityProvider identityProvider;
    private final WeChatBindingTicketStore bindingTickets;
    private final SessionService sessions;

    public WeChatAuthenticationService(
            UserAccountRepository users,
            WeChatIdentityProvider identityProvider,
            WeChatBindingTicketStore bindingTickets,
            SessionService sessions) {
        this.users = users;
        this.identityProvider = identityProvider;
        this.bindingTickets = bindingTickets;
        this.sessions = sessions;
    }

    public WeChatLoginResult login(String loginCode) {
        var openid = identityProvider.exchangeLoginCode(loginCode);
        var existing = users.findByOpenid(openid);
        if (existing.isPresent()) {
            if (!existing.get().enabled()) {
                throw new BusinessException("ACCOUNT_DISABLED", "账号已停用", HttpStatus.FORBIDDEN);
            }
            return WeChatLoginResult.authenticated(authenticated(existing.get()));
        }
        return WeChatLoginResult.bindingRequired(bindingTickets.create(openid));
    }

    public AuthenticationResult bindPhone(String bindingTicket, String phoneCode) {
        var openid = bindingTickets.consume(bindingTicket)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_BINDING_TICKET", "手机号绑定凭证已失效", HttpStatus.UNAUTHORIZED));
        var phone = identityProvider.exchangePhoneCode(phoneCode);
        UserAccount user = users.findByPhone(phone)
                .filter(UserAccount::enabled)
                .orElseThrow(() -> new BusinessException(
                        "PHONE_NOT_PROVISIONED", "该手机号尚未开通系统权限", HttpStatus.FORBIDDEN));
        user.bindOpenid(openid);
        users.save(user);
        return authenticated(user);
    }

    private AuthenticationResult authenticated(UserAccount user) {
        var principal = user.toPrincipal();
        return new AuthenticationResult(principal, sessions.create(principal));
    }
}
