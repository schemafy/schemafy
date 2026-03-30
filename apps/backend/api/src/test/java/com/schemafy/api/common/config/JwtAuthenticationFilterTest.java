package com.schemafy.api.common.config;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.common.exception.ProblemDetailFactory;
import com.schemafy.api.common.exception.ProblemProperties;
import com.schemafy.api.common.security.jwt.JwtAuthenticationFilter;
import com.schemafy.api.common.security.jwt.JwtProvider;
import com.schemafy.api.common.security.jwt.WebExchangeErrorWriter;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  JwtProvider jwtProvider;

  @Mock
  WebFilterChain filterChain;

  JwtAuthenticationFilter jwtAuthenticationFilter;

  WebExchangeErrorWriter errorResponseWriter;

  @BeforeEach
  void setUp() {
    errorResponseWriter = new WebExchangeErrorWriter(
        new JsonCodec(new ObjectMapper()),
        new ProblemDetailFactory(new ProblemProperties()));
    jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider,
        errorResponseWriter);
  }

  @Test
  @DisplayName("유효한 JWT 토큰으로 인증에 성공한다")
  void authenticateValidToken() {
    String token = "valid-token";
    String userId = "user123";
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(jwtProvider.extractUserId(token)).thenReturn(userId);
    when(jwtProvider.getTokenType(token))
        .thenReturn(JwtProvider.ACCESS_TOKEN);
    when(jwtProvider.validateToken(token, userId)).thenReturn(true);
    when(filterChain.filter(any())).thenReturn(Mono.empty());

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain)
            .contextWrite(ctx -> {
              return ctx;
            })
            .then(Mono.deferContextual(Mono::just))
            .flatMap(ctx -> ReactiveSecurityContextHolder
                .getContext()))
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  @DisplayName("유효한 JWT 토큰이면 actorUserId를 typed operation metadata에 저장한다")
  void storesActorUserIdInTypedOperationMetadata() {
    String token = "valid-token";
    String userId = "user123";
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicReference<ErdOperationMetadata> metadataRef = new AtomicReference<>();

    when(jwtProvider.extractUserId(token)).thenReturn(userId);
    when(jwtProvider.getTokenType(token))
        .thenReturn(JwtProvider.ACCESS_TOKEN);
    when(jwtProvider.validateToken(token, userId)).thenReturn(true);
    when(filterChain.filter(any())).thenReturn(Mono.deferContextual(ctx -> {
      metadataRef.set(ErdOperationContexts.metadata(ctx));
      return Mono.empty();
    }));

    StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(metadataRef.get()).isEqualTo(new ErdOperationMetadata(
        null,
        null,
        null,
        userId));
  }

  @Test
    @DisplayName("Authorization 헤더가 없는 요청을 거부한다")
    void rejectRequestWithoutAuthHeader() {
        when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

  @Test
    @DisplayName("잘못된 형식의 Authorization 헤더를 거부한다")
    void rejectMalformedAuthHeader() {
        when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

  @Test
  @DisplayName("만료된 토큰을 거부한다")
  void rejectExpiredToken() {
    String token = "expired-token";
    String userId = "user123";
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(jwtProvider.extractUserId(token)).thenReturn(userId);
    when(jwtProvider.getTokenType(token))
        .thenReturn(JwtProvider.ACCESS_TOKEN);
    when(jwtProvider.validateToken(token, userId)).thenReturn(false);

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("REFRESH 토큰 타입을 거부한다")
  void rejectRefreshTokenType() {
    String token = "refresh-token";
    String userId = "user123";
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(jwtProvider.extractUserId(token)).thenReturn(userId);
    when(jwtProvider.getTokenType(token))
        .thenReturn(JwtProvider.REFRESH_TOKEN);

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("토큰 검증 중 발생한 예외를 처리한다")
  void handleTokenValidationException() {
    String token = "invalid-token";
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    when(jwtProvider.extractUserId(token))
        .thenThrow(new RuntimeException("Invalid token"));

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("Bearer 접두사 뒤에 빈 토큰을 처리한다")
  void handleEmptyTokenAfterBearer() {
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer ")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
    @DisplayName("대소문자가 다른 Bearer 접두사를 처리한다")
    void handleBearerPrefixCasing() {
        when(filterChain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "bearer token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

  @Test
  @DisplayName("공백만 있는 토큰을 거부한다")
  void rejectWhitespaceToken() {
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.AUTHORIZATION, "Bearer    ")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier
        .create(jwtAuthenticationFilter.filter(exchange, filterChain))
        .verifyComplete();
  }

}
