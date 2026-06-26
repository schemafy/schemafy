package com.schemafy.mcp.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class McpTokenValidator {

  private static final String CLAIM_TYPE = "type";
  private static final String CLAIM_SCOPE = "scope";
  private static final String CLAIM_SCOPES = "scopes";
  private static final String CLAIM_SCP = "scp";

  private final McpSecurityProperties properties;
  private final McpTokenRevocationStore revocationStore;
  private final SecretKey secretKey;

  public McpTokenValidator(
      McpSecurityProperties properties,
      McpTokenRevocationStore revocationStore) {
    this.properties = properties;
    this.revocationStore = revocationStore;
    this.secretKey = Keys.hmacShaKeyFor(
        properties.getToken().getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public Mono<McpTokenValidationResult> validate(String token) {
    if (!StringUtils.hasText(token)) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_MISSING));
    }
    return Mono.fromCallable(() -> parseClaims(token))
        .flatMap(this::validateClaims)
        .onErrorResume(ExpiredJwtException.class,
            e -> Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_EXPIRED)))
        .onErrorResume(JwtException.class,
            e -> Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_MALFORMED)))
        .onErrorResume(IllegalArgumentException.class,
            e -> Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_MALFORMED)));
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .clockSkewSeconds(properties.getToken().getClockSkewSeconds())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Mono<McpTokenValidationResult> validateClaims(Claims claims) {
    if (!Objects.equals(properties.getToken().getIssuer(), claims.getIssuer())
        || claims.getAudience() == null
        || !claims.getAudience().contains(properties.getToken().getAudience())
        || claims.getExpiration() == null
        || claims.getExpiration().before(new Date())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }
    if (!StringUtils.hasText(claims.getSubject())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }
    if (!Objects.equals(properties.getToken().getTokenType(),
        claims.get(CLAIM_TYPE, String.class))) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }
    if (!StringUtils.hasText(claims.getId())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }

    Set<String> scopes = extractScopes(claims);
    if (!scopes.contains(properties.getToken().getRequiredScope())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.INSUFFICIENT_SCOPE));
    }

    McpTokenClaims tokenClaims = new McpTokenClaims(claims.getId(), claims.getSubject(),
        Set.copyOf(scopes));
    return revocationStore.isRevoked(claims.getId())
        .map(revoked -> revoked
            ? McpTokenValidationResult.failure(McpSecurityError.TOKEN_REVOKED)
            : McpTokenValidationResult.success(tokenClaims));
  }

  private Set<String> extractScopes(Claims claims) {
    Set<String> scopes = new LinkedHashSet<>();
    addScopes(scopes, claims.get(CLAIM_SCOPE));
    addScopes(scopes, claims.get(CLAIM_SCOPES));
    addScopes(scopes, claims.get(CLAIM_SCP));
    return scopes;
  }

  private void addScopes(Set<String> scopes, Object value) {
    if (value instanceof String stringValue) {
      Arrays.stream(stringValue.split("[\\s,]+"))
          .filter(StringUtils::hasText)
          .forEach(scopes::add);
      return;
    }
    if (value instanceof Collection<?> values) {
      values.stream()
          .filter(Objects::nonNull)
          .map(Object::toString)
          .filter(StringUtils::hasText)
          .forEach(scopes::add);
    }
  }

}
