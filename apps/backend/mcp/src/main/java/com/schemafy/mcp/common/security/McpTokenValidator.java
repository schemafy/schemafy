package com.schemafy.mcp.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.schemafy.core.mcp.application.port.in.GetMcpTokenQuery;
import com.schemafy.core.mcp.application.port.in.GetMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpToken;

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
  private final GetMcpTokenUseCase getMcpTokenUseCase;
  private final Clock clock;
  private final SecretKey secretKey;

  public McpTokenValidator(
      McpSecurityProperties properties,
      McpTokenRevocationStore revocationStore,
      GetMcpTokenUseCase getMcpTokenUseCase,
      Clock clock) {
    this.properties = properties;
    this.revocationStore = revocationStore;
    this.getMcpTokenUseCase = getMcpTokenUseCase;
    this.clock = clock;
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
        .clock(() -> Date.from(clock.instant()))
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
        || claims.getExpiration().before(Date.from(clock.instant()))) {
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
        .flatMap(revoked -> revoked
            ? Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_REVOKED))
            : validateRegisteredToken(tokenClaims))
        .onErrorResume(error -> Mono.just(McpTokenValidationResult.failure(
            McpSecurityError.REVOCATION_CHECK_UNAVAILABLE)));
  }

  private Mono<McpTokenValidationResult> validateRegisteredToken(
      McpTokenClaims tokenClaims) {
    return getMcpTokenUseCase.getMcpToken(new GetMcpTokenQuery(tokenClaims.tokenId()))
        .map(token -> validateRegisteredToken(tokenClaims, token))
        .defaultIfEmpty(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID))
        .onErrorResume(error -> Mono.just(McpTokenValidationResult.failure(
            McpSecurityError.TOKEN_REGISTRY_UNAVAILABLE)));
  }

  private McpTokenValidationResult validateRegisteredToken(
      McpTokenClaims claims,
      McpToken token) {
    if (!token.belongsTo(claims.userId())
        || !claims.hasScope(token.getScope())
        || token.isDeleted()
        || token.isExpiredAt(clock.instant())) {
      return McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID);
    }
    if (token.isRevoked()) {
      return McpTokenValidationResult.failure(McpSecurityError.TOKEN_REVOKED);
    }
    return McpTokenValidationResult.success(claims);
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
