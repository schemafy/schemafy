package com.schemafy.mcp.common.security;

import java.time.Clock;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.schemafy.core.mcp.application.port.in.GetMcpTokenQuery;
import com.schemafy.core.mcp.application.port.in.GetMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpToken;
import com.schemafy.core.mcp.domain.McpTokenClaimSupport;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class McpTokenValidator {

  private final McpSecurityProperties properties;
  private final McpTokenRevocationCache revocationCache;
  private final GetMcpTokenUseCase getMcpTokenUseCase;
  private final Clock clock;
  private final SecretKey secretKey;

  public McpTokenValidator(
      McpSecurityProperties properties,
      McpTokenRevocationCache revocationCache,
      GetMcpTokenUseCase getMcpTokenUseCase,
      Clock clock,
      @Qualifier("mcpTokenSecretKey") SecretKey secretKey) {
    this.properties = properties;
    this.revocationCache = revocationCache;
    this.getMcpTokenUseCase = getMcpTokenUseCase;
    this.clock = clock;
    this.secretKey = secretKey;
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
        claims.get(McpTokenClaimSupport.TYPE, String.class))) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }
    if (!StringUtils.hasText(claims.getId())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.TOKEN_INVALID));
    }

    Set<String> scopes = McpTokenClaimSupport.extractScopes(claims::get);
    if (!scopes.contains(properties.getToken().getRequiredScope())) {
      return Mono.just(McpTokenValidationResult.failure(McpSecurityError.INSUFFICIENT_SCOPE));
    }

    McpTokenClaims tokenClaims = new McpTokenClaims(claims.getId(), claims.getSubject(),
        Set.copyOf(scopes));
    return revocationCache.isRevoked(claims.getId())
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

}
