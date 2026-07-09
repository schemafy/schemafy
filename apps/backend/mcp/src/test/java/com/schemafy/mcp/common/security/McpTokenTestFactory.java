package com.schemafy.mcp.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

import com.schemafy.core.mcp.domain.McpTokenClaimSupport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

final class McpTokenTestFactory {

  private static final String DEFAULT_TOKEN_ID = "token-1";
  private static final String DEFAULT_USER_ID = "user-1";

  private final McpSecurityProperties properties;
  private final Clock clock;
  private final SecretKey secretKey;

  McpTokenTestFactory(McpSecurityProperties properties) {
    this(properties, Clock.systemUTC());
  }

  McpTokenTestFactory(McpSecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    this.secretKey = Keys.hmacShaKeyFor(
        properties.getToken().getSecret().getBytes(StandardCharsets.UTF_8));
  }

  String validToken() {
    return token(builder());
  }

  String tokenWithoutRequiredScope() {
    return token(builder().claim(McpTokenClaimSupport.SCOPE, "schema:read"));
  }

  String expiredToken() {
    Instant now = clock.instant();
    return token(builder()
        .issuedAt(Date.from(now.minusSeconds(120)))
        .expiration(Date.from(now.minusSeconds(60))));
  }

  String wrongIssuerToken() {
    return token(builder().issuer("wrong-issuer"));
  }

  String wrongAudienceToken() {
    Instant now = clock.instant();
    return token(Jwts.builder()
        .id(DEFAULT_TOKEN_ID)
        .subject(DEFAULT_USER_ID)
        .issuer(properties.getToken().getIssuer())
        .audience().add("wrong-audience").and()
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(3600)))
        .claim(McpTokenClaimSupport.TYPE, properties.getToken().getTokenType())
        .claim(McpTokenClaimSupport.SCOPE, properties.getToken().getRequiredScope()));
  }

  String wrongTokenTypeToken() {
    return token(builder().claim(McpTokenClaimSupport.TYPE, "ACCESS"));
  }

  String revokedToken(String tokenId) {
    return token(builder().id(tokenId));
  }

  private io.jsonwebtoken.JwtBuilder builder() {
    Instant now = clock.instant();
    return Jwts.builder()
        .id(DEFAULT_TOKEN_ID)
        .subject(DEFAULT_USER_ID)
        .issuer(properties.getToken().getIssuer())
        .audience().add(properties.getToken().getAudience()).and()
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(3600)))
        .claim(McpTokenClaimSupport.TYPE, properties.getToken().getTokenType())
        .claim(McpTokenClaimSupport.SCOPE, String.join(" ", List.of(
            properties.getToken().getRequiredScope(),
            "schema:read")));
  }

  private String token(io.jsonwebtoken.JwtBuilder builder) {
    return builder.signWith(secretKey).compact();
  }

}
