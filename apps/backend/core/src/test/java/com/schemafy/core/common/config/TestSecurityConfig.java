package com.schemafy.core.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtAccessDeniedHandler;
import com.schemafy.core.common.security.jwt.JwtAuthenticationEntryPoint;

@TestConfiguration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Import({ JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class })
public class TestSecurityConfig {

    private static final String API_BASE_PATH = ApiPath.API.replace("{version}",
            "v1.0");

    private static final String[] PUBLIC_ENDPOINTS = {
        API_BASE_PATH + "/auth/**",
        API_BASE_PATH + "/public/**",
        API_BASE_PATH + "/users/signup",
        API_BASE_PATH + "/users/login",
        API_BASE_PATH + "/users/refresh",
        "/actuator/health",
        "/actuator/info",
        "/webjars/**",
        "/v3/api-docs/**",
        "/swagger-ui/**"
    };

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public TestSecurityConfig(
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityFilterChain(
            ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated())
                .build();
    }
}
