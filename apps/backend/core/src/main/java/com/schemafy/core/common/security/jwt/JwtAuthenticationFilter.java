package com.schemafy.core.common.security.jwt;

import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.principal.AuthenticatedUser;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
  private static final String CLAIM_NAME = "name";

  private final JwtProvider jwtProvider;
  private final WebExchangeErrorWriter errorResponseWriter;

  @Override
  public Mono<Void> filter(@NotNull ServerWebExchange exchange,
      @NonNull WebFilterChain chain) {
    String token = extractToken(exchange.getRequest());

    if (token == null) {
      return chain.filter(exchange);
    }

    // JWT 검증을 boundedElastic 스케줄러로 분리 (블로킹 호출 방지)
    return Mono.fromCallable(() -> validateTokenAndGetAuth(token))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(result -> {
          if (result.valid()) {
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder
                    .withAuthentication(
                        result.authentication()));
          } else {
            return handleJwtError(exchange,
                result.errorType(),
                result.errorMessage(),
                result.status());
          }
        })
        .onErrorResume(e -> handleUnexpectedError(exchange));
  }

  private AuthenticationResult validateTokenAndGetAuth(String token) {
    try {
      String userId = jwtProvider.extractUserId(token);
      String tokenType = jwtProvider.getTokenType(token);

      if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
        return AuthenticationResult
            .error(ErrorCode.INVALID_ACCESS_TOKEN_TYPE);
      }

      if (!jwtProvider.validateToken(token, userId)) {
        return AuthenticationResult.error(ErrorCode.INVALID_TOKEN);
      }

      String userName = jwtProvider.extractClaim(token,
          claims -> claims.get(CLAIM_NAME, String.class));
      AuthenticatedUser principal = createPrincipal(userId, userName);
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          principal, null, principal.asAuthorities());

      return AuthenticationResult.success(authentication);

    } catch (ExpiredJwtException e) {
      return AuthenticationResult.error(ErrorCode.EXPIRED_TOKEN);
    } catch (JwtException e) {
      return AuthenticationResult.error(ErrorCode.MALFORMED_TOKEN);
    } catch (Exception e) {
      return AuthenticationResult.error(ErrorCode.TOKEN_VALIDATION_ERROR);
    }
  }

  private Mono<Void> handleJwtError(ServerWebExchange exchange,
      String errorCode, String errorMessage, HttpStatus status) {
    return errorResponseWriter.writeErrorResponse(
        exchange,
        status,
        errorCode,
        errorMessage);
  }

  private Mono<Void> handleJwtError(ServerWebExchange exchange,
      ErrorCode errorCode) {
    return errorResponseWriter.writeErrorResponse(
        exchange,
        errorCode.getStatus(),
        errorCode.getCode(),
        errorCode.getMessage());
  }

  private Mono<Void> handleUnexpectedError(ServerWebExchange exchange) {
    return handleJwtError(exchange, ErrorCode.TOKEN_VALIDATION_ERROR);
  }

  private AuthenticatedUser createPrincipal(String userId, String userName) {
    // TODO: JWT 클레임 또는 DB 조회를 통해 역할(roles)을 채워 넣는다.
    // 현재는 모든 프로젝트 롤을 부여해 hasAuthority 기반 접근제어를 통과시키는 임시 구현.
    return AuthenticatedUser.withAllRoles(userId, userName);
  }

  private String extractToken(ServerHttpRequest request) {
    String bearerToken = request.getHeaders()
        .getFirst(HttpHeaders.AUTHORIZATION);

    if (StringUtils.hasText(bearerToken)
        && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }

    String path = request.getPath().pathWithinApplication().value();
    if (path != null && path.startsWith("/ws/")) {
      var cookie = request.getCookies()
          .getFirst(ACCESS_TOKEN_COOKIE_NAME);
      if (cookie != null && StringUtils.hasText(cookie.getValue())) {
        return cookie.getValue();
      }
    }

    return null;
  }

  private record AuthenticationResult(
      boolean valid,
      UsernamePasswordAuthenticationToken authentication,
      HttpStatus status,
      String errorType,
      String errorMessage) {

    static AuthenticationResult success(
        UsernamePasswordAuthenticationToken authentication) {
      return new AuthenticationResult(true, authentication, null, null,
          null);
    }

    static AuthenticationResult error(ErrorCode errorCode) {
      return new AuthenticationResult(false, null, errorCode.getStatus(),
          errorCode.getCode(), errorCode.getMessage());
    }

  }

}
