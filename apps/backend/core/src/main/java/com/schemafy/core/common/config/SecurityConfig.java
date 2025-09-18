package com.schemafy.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()                           // CSRF 보호 비활성화
                .cors().disable()                           // CORS 설정 비활성화
                .httpBasic().disable()                      // HTTP Basic 인증 비활성화
                .formLogin().disable()                      // Form 로그인 비활성화
                .logout().disable()                         // 로그아웃 기능 비활성화
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()              // 모든 요청 허용
                )
                .build();
    }
}
