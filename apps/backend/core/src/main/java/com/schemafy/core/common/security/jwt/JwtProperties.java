package com.schemafy.core.common.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "my-super-secret-key-for-jwt-token-generation-min-256-bits-required";
    private long accessTokenExpiration = 7200000L; // 2 hours
    private long refreshTokenExpiration = 604800000L; // 7 days
    private String issuer = "schemafy";
    private String audience = "schemafy-audience";
}