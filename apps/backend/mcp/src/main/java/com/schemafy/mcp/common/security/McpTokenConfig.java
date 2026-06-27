package com.schemafy.mcp.common.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;

@Configuration(proxyBeanMethods = false)
public class McpTokenConfig {

  @Bean
  SecretKey mcpTokenSecretKey(McpSecurityProperties properties) {
    return Keys.hmacShaKeyFor(
        properties.getToken().getSecret().getBytes(StandardCharsets.UTF_8));
  }

}
