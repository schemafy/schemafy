package com.schemafy.api.mcp.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;
import javax.crypto.SecretKey;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.schemafy.api.mcp.exception.McpTokenErrorCode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenUseCase;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpTokenClaimSupport;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class McpTokenService {

  private final McpTokenProperties properties;
  @Nullable
  private final McpTokenRevocationCache revocationCache;
  private final RegisterMcpTokenUseCase registerMcpTokenUseCase;
  private final RevokeMcpTokenUseCase revokeMcpTokenUseCase;
  private final Clock clock;
  private final SecretKey secretKey;

  public McpTokenService(
      McpTokenProperties properties,
      @Nullable McpTokenRevocationCache revocationCache,
      RegisterMcpTokenUseCase registerMcpTokenUseCase,
      RevokeMcpTokenUseCase revokeMcpTokenUseCase,
      Clock clock) {
    this.properties = properties;
    this.revocationCache = revocationCache;
    this.registerMcpTokenUseCase = registerMcpTokenUseCase;
    this.revokeMcpTokenUseCase = revokeMcpTokenUseCase;
    this.clock = clock;
    this.secretKey = Keys.hmacShaKeyFor(
        properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public Mono<McpTokenIssueResult> issue(String userId) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.getExpiresIn());
    String tokenId = UlidGenerator.generate();
    String scope = properties.getRequiredScope();

    String token = Jwts.builder()
        .id(tokenId)
        .subject(userId)
        .issuer(properties.getIssuer())
        .audience().add(properties.getAudience()).and()
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .claim(McpTokenClaimSupport.TYPE, properties.getTokenType())
        .claim(McpTokenClaimSupport.SCOPE, scope)
        .signWith(secretKey)
        .compact();

    McpTokenIssueResult result = new McpTokenIssueResult(
        token,
        tokenId,
        scope,
        issuedAt,
        expiresAt,
        properties.getExpiresIn().toSeconds());
    return registerMcpTokenUseCase.registerMcpToken(new RegisterMcpTokenCommand(
        tokenId,
        userId,
        scope,
        issuedAt,
        expiresAt))
        .thenReturn(result)
        .onErrorMap(notDomainException(), error -> new DomainException(
            McpTokenErrorCode.REGISTRY_UNAVAILABLE,
            "MCP token registry is temporarily unavailable"));
  }

  public Mono<Void> revoke(String userId, String token) {
    return Mono.defer(() -> {
      Claims claims = parseClaims(token);
      validateRevocableClaims(userId, claims);

      Instant revokedAt = clock.instant();
      Instant expiresAt = claims.getExpiration().toInstant();
      Duration ttl = Duration.between(revokedAt, expiresAt);
      if (ttl.isZero() || ttl.isNegative()) {
        return Mono.empty();
      }

      return revokeMcpTokenUseCase.revokeMcpToken(new RevokeMcpTokenCommand(
          claims.getId(), userId, revokedAt))
          .flatMap(registered -> registered
              ? cacheRevocation(claims.getId(), ttl)
              : Mono.error(new DomainException(McpTokenErrorCode.INVALID,
                  "MCP token is not registered")));
    })
        .onErrorMap(notDomainException(), error -> new DomainException(
            McpTokenErrorCode.REGISTRY_UNAVAILABLE,
            "MCP token registry is temporarily unavailable"));
  }

  private Claims parseClaims(String token) {
    if (!StringUtils.hasText(token)) {
      throw new DomainException(McpTokenErrorCode.INVALID,
          "MCP token is required");
    }
    try {
      return Jwts.parser()
          .verifyWith(secretKey)
          .clock(() -> Date.from(clock.instant()))
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      return e.getClaims();
    } catch (JwtException | IllegalArgumentException e) {
      throw new DomainException(McpTokenErrorCode.INVALID,
          "MCP token is invalid");
    }
  }

  private void validateRevocableClaims(String userId, Claims claims) {
    if (!Objects.equals(properties.getIssuer(), claims.getIssuer())
        || claims.getAudience() == null
        || !claims.getAudience().contains(properties.getAudience())
        || !Objects.equals(properties.getTokenType(),
            claims.get(McpTokenClaimSupport.TYPE, String.class))
        || !StringUtils.hasText(claims.getId())
        || claims.getExpiration() == null
        || !McpTokenClaimSupport.extractScopes(claims::get)
            .contains(properties.getRequiredScope())) {
      throw new DomainException(McpTokenErrorCode.INVALID,
          "MCP token is invalid");
    }
    if (!Objects.equals(userId, claims.getSubject())) {
      throw new DomainException(McpTokenErrorCode.OWNER_MISMATCH,
          "MCP token belongs to another user");
    }
  }

  private Mono<Void> cacheRevocation(String tokenId, Duration ttl) {
    if (revocationCache == null) {
      return Mono.empty();
    }
    return revocationCache.cacheRevocation(tokenId, ttl)
        .onErrorResume(error -> {
          log.warn("Failed to cache MCP token revocation: tokenId={}", tokenId,
              error);
          return Mono.empty();
        });
  }

  private Predicate<Throwable> notDomainException() {
    return error -> !(error instanceof DomainException);
  }

}
