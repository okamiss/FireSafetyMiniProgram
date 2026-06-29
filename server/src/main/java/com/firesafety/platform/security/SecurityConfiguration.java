package com.firesafety.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.auth.SessionService;
import com.firesafety.platform.organization.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SessionAuthenticationFilter sessionAuthenticationFilter(
            SessionService sessions, UserAccountRepository users) {
        return new SessionAuthenticationFilter(sessions, users);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionAuthenticationFilter sessionFilter,
            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/admin-login",
                                "/api/auth/wechat-login",
                                "/api/auth/wechat-bind-phone",
                                "/api/auth/refresh").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> ApiSecurityWriter.write(
                                response, objectMapper, HttpStatus.UNAUTHORIZED.value(),
                                "UNAUTHORIZED", "请先登录"))
                        .accessDeniedHandler((request, response, exception) -> ApiSecurityWriter.write(
                                response, objectMapper, HttpStatus.FORBIDDEN.value(),
                                "FORBIDDEN", "无权执行此操作")))
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
