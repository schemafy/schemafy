package com.schemafy.core.common.security.hmac;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.security.hmac.HmacProperties.EnforcementMode;
import com.schemafy.core.common.security.jwt.WebExchangeErrorWriter;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HmacVerificationFilterTest {

  private static final String SECRET = "test-secret-key";
  private static final SecretKey KEY = HmacUtil.createSecretKey(SECRET);

  @Mock
  WebFilterChain filterChain;

  @Mock
  NonceCache nonceCache;

  HmacProperties hmacProperties;
  WebExchangeErrorWriter errorWriter;
  HmacVerificationFilter filter;

  @BeforeEach
  void setUp() {
    hmacProperties = new HmacProperties();
    hmacProperties.setSecret(SECRET);
    hmacProperties.setTimestampToleranceSeconds(30);
    hmacProperties.setEnabled(true);
    hmacProperties.setEnforcementMode(EnforcementMode.ENFORCE);

    errorWriter = new WebExchangeErrorWriter(new ObjectMapper());
    filter = new HmacVerificationFilter(hmacProperties, nonceCache,
        errorWriter);
  }

  private String currentTimestamp() {
    return String.valueOf(System.currentTimeMillis());
  }

  private MockServerWebExchange createExchangeWithHmac(String method,
      String path, String body, String nonce) {
    String timestamp = currentTimestamp();
    byte[] bodyBytes = body != null ? body.getBytes() : new byte[0];
    String bodyHash = HmacUtil.computeBodyHash(bodyBytes);
    String canonical = HmacUtil.buildHttpCanonicalString(method, path,
        timestamp, nonce, bodyHash);
    String signature = HmacUtil.computeHmac(KEY, canonical);

    MockServerHttpRequest request;
    if ("POST".equals(method)) {
      request = MockServerHttpRequest.post(path)
          .header(HmacVerificationFilter.HEADER_SIGNATURE, signature)
          .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
          .header(HmacVerificationFilter.HEADER_NONCE, nonce)
          .body(body != null ? body : "");
    } else {
      request = MockServerHttpRequest.get(path)
          .header(HmacVerificationFilter.HEADER_SIGNATURE, signature)
          .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
          .header(HmacVerificationFilter.HEADER_NONCE, nonce)
          .build();
    }

    return MockServerWebExchange.from(request);
  }

  @Test
  @DisplayName("유효한 HMAC 서명이 있으면 요청을 통과시킨다")
  void validHmacPassesThrough() {
    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(false));
    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerWebExchange exchange = createExchangeWithHmac("GET",
        "/api/v1.0/tables", null, "nonce-valid-1");

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("HMAC 서명 헤더가 누락되면 H001 에러를 반환한다")
  void missingSignatureReturnsH001() {
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("서명이 일치하지 않으면 H002 에러를 반환한다")
  void invalidSignatureReturnsH002() {
    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(false));

    String timestamp = currentTimestamp();
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables")
        .header(HmacVerificationFilter.HEADER_SIGNATURE,
            "invalidsignature")
        .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
        .header(HmacVerificationFilter.HEADER_NONCE,
            "nonce-invalid-1")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("타임스탬프가 만료되면 H003 에러를 반환한다")
  void expiredTimestampReturnsH003() {
    String expiredTimestamp = String
        .valueOf(System.currentTimeMillis() - 60_000);
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables")
        .header(HmacVerificationFilter.HEADER_SIGNATURE, "anysig")
        .header(HmacVerificationFilter.HEADER_TIMESTAMP,
            expiredTimestamp)
        .header(HmacVerificationFilter.HEADER_NONCE,
            "nonce-expired-1")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("중복 nonce는 H004 에러를 반환한다")
  void duplicateNonceReturnsH004() {
    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(true));

    String timestamp = currentTimestamp();
    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables")
        .header(HmacVerificationFilter.HEADER_SIGNATURE, "anysig")
        .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
        .header(HmacVerificationFilter.HEADER_NONCE, "nonce-dup-1")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Public API 경로는 HMAC 검증을 스킵한다")
  void publicApiSkipsHmac() {
    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerHttpRequest request = MockServerHttpRequest
        .get("/public/api/v1.0/health").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("WebSocket 핸드셰이크 경로는 HMAC 검증을 스킵한다")
  void webSocketPathSkipsHmac() {
    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerHttpRequest request = MockServerHttpRequest
        .get("/ws/collaboration?projectId=test-project-id").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("enabled가 false이면 HMAC 검증을 전체 스킵한다")
  void disabledSkipsAll() {
    hmacProperties.setEnabled(false);
    filter = new HmacVerificationFilter(hmacProperties, nonceCache,
        errorWriter);

    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("LOG_ONLY 모드에서는 검증 실패해도 요청을 통과시킨다")
  void logOnlyModePassesOnFailure() {
    hmacProperties.setEnforcementMode(EnforcementMode.LOG_ONLY);
    filter = new HmacVerificationFilter(hmacProperties, nonceCache,
        errorWriter);

    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    // LOG_ONLY이므로 401이 아닌 정상 통과
    assertThat(exchange.getResponse().getStatusCode())
        .isNotEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("previousSecret으로 서명된 요청도 통과한다")
  void previousSecretFallback() {
    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(false));
    given(filterChain.filter(any())).willReturn(Mono.empty());

    String prevSecret = "old-secret-key";
    hmacProperties.setPreviousSecret(prevSecret);
    filter = new HmacVerificationFilter(hmacProperties, nonceCache,
        errorWriter);

    String timestamp = currentTimestamp();
    String nonce = "nonce-fallback-1";
    String bodyHash = HmacUtil.computeBodyHash(new byte[0]);
    String canonical = HmacUtil.buildHttpCanonicalString("GET",
        "/api/v1.0/tables", timestamp, nonce, bodyHash);
    SecretKey prevKey = HmacUtil.createSecretKey(prevSecret);
    String signature = HmacUtil.computeHmac(prevKey, canonical);

    MockServerHttpRequest request = MockServerHttpRequest
        .get("/api/v1.0/tables")
        .header(HmacVerificationFilter.HEADER_SIGNATURE, signature)
        .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
        .header(HmacVerificationFilter.HEADER_NONCE, nonce)
        .build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("POST 요청의 body를 포함하여 HMAC을 검증한다")
  void postRequestWithBody() {
    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(false));
    given(filterChain.filter(any())).willReturn(Mono.empty());

    String body = "{\"name\":\"test-table\"}";
    MockServerWebExchange exchange = createExchangeWithHmac("POST",
        "/api/v1.0/tables", body, "nonce-post-1");

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  @DisplayName("OPTIONS 요청은 HMAC 검증을 스킵한다")
  void optionsRequestSkipsHmac() {
    given(filterChain.filter(any())).willReturn(Mono.empty());

    MockServerHttpRequest request = MockServerHttpRequest
        .options("/api/v1.0/tables").build();
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();

    verify(filterChain).filter(any());
  }

  @Test
  @DisplayName("LOG_ONLY 모드에서 서명 검증 실패 시 POST body가 보존된다")
  void logOnlyModePreservesBodyOnSignatureFailure() {
    hmacProperties.setEnforcementMode(EnforcementMode.LOG_ONLY);
    filter = new HmacVerificationFilter(hmacProperties, nonceCache,
        errorWriter);

    given(nonceCache.isDuplicate(anyString()))
        .willReturn(Mono.just(false));

    String body = "{\"name\":\"test-table\"}";

    // filterChain에서 전달받은 exchange의 body를 검증
    given(filterChain.filter(any())).willAnswer(invocation -> {
      ServerWebExchange passedExchange = invocation.getArgument(0);
      return DataBufferUtils
          .join(passedExchange.getRequest().getBody())
          .map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            String readBody = new String(bytes,
                StandardCharsets.UTF_8);
            assertThat(readBody).isEqualTo(body);
            return readBody;
          }).then();
    });

    String timestamp = currentTimestamp();
    MockServerHttpRequest request = MockServerHttpRequest
        .post("/api/v1.0/tables")
        .header(HmacVerificationFilter.HEADER_SIGNATURE,
            "invalid-signature")
        .header(HmacVerificationFilter.HEADER_TIMESTAMP, timestamp)
        .header(HmacVerificationFilter.HEADER_NONCE,
            "nonce-logonly-body-1")
        .body(body);
    MockServerWebExchange exchange = MockServerWebExchange
        .from(request);

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

}
