package com.schemafy.api.mcp.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.schemafy.api.mcp.service.McpTokenProperties;

import io.jsonwebtoken.security.Keys;

@Configuration(proxyBeanMethods = false)
public class McpTokenConfig {

  @Bean
  SecretKey mcpTokenSecretKey(McpTokenProperties properties) {
    return Keys.hmacShaKeyFor(
        properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

}
