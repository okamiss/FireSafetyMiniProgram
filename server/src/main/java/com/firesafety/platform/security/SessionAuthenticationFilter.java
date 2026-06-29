package com.firesafety.platform.security;

import com.firesafety.platform.auth.SessionService;
import com.firesafety.platform.organization.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionService sessions;
    private final UserAccountRepository users;

    public SessionAuthenticationFilter(SessionService sessions, UserAccountRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            var token = authorization.substring(BEARER_PREFIX.length()).trim();
            sessions.resolve(token).flatMap(principal -> users.findById(principal.userId())
                    .filter(user -> user.enabled()
                            && user.role() == principal.role()
                            && Objects.equals(user.enterpriseId(), principal.enterpriseId()))
                    .map(user -> principal)).ifPresent(principal -> {
                        var authority = new SimpleGrantedAuthority(principal.role().authority());
                        var authentication =
                                new UsernamePasswordAuthenticationToken(principal, token, List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
