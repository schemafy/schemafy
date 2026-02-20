package com.schemafy.core.common.security.jwt;

import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.core.common.exception.AuthErrorCode;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.domain.common.exception.DomainErrorCode;

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

    return Mono.fromCallable(() -> validateTokenAndGetAuth(token))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(result -> {
          if (result.valid()) {
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder
                    .withAuthentication(result.authentication()));
          }
          return handleJwtError(exchange, result.errorCode(),
              result.errorMessage());
        })
        .onErrorResume(e -> handleUnexpectedError(exchange));
  }

  private AuthenticationResult validateTokenAndGetAuth(String token) {
    try {
      String userId = jwtProvider.extractUserId(token);
      String tokenType = jwtProvider.getTokenType(token);

      if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
        return AuthenticationResult
            .error(AuthErrorCode.INVALID_ACCESS_TOKEN_TYPE);
      }

      if (!jwtProvider.validateToken(token, userId)) {
        return AuthenticationResult.error(AuthErrorCode.INVALID_TOKEN);
      }

      String userName = jwtProvider.extractClaim(token,
          claims -> claims.get(CLAIM_NAME, String.class));
      AuthenticatedUser principal = createPrincipal(userId, userName);
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          principal, null, principal.asAuthorities());

      return AuthenticationResult.success(authentication);

    } catch (ExpiredJwtException e) {
      return AuthenticationResult.error(AuthErrorCode.EXPIRED_TOKEN);
    } catch (JwtException e) {
      return AuthenticationResult.error(AuthErrorCode.MALFORMED_TOKEN);
    } catch (Exception e) {
      return AuthenticationResult.error(AuthErrorCode.TOKEN_VALIDATION_ERROR);
    }
  }

  private Mono<Void> handleJwtError(ServerWebExchange exchange,
      DomainErrorCode errorCode, String errorMessage) {
    return errorResponseWriter.writeErrorResponse(
        exchange,
        errorCode.status(),
        errorCode.code(),
        errorMessage);
  }

  private Mono<Void> handleUnexpectedError(ServerWebExchange exchange) {
    DomainErrorCode errorCode = AuthErrorCode.TOKEN_VALIDATION_ERROR;
    return handleJwtError(exchange, errorCode, errorCode.code());
  }

  private AuthenticatedUser createPrincipal(String userId, String userName) {
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
      DomainErrorCode errorCode,
      String errorMessage) {

    static AuthenticationResult success(
        UsernamePasswordAuthenticationToken authentication) {
      return new AuthenticationResult(true, authentication, null, null);
    }

    static AuthenticationResult error(DomainErrorCode errorCode) {
      return new AuthenticationResult(false, null, errorCode,
          errorCode.code());
    }

  }

}
