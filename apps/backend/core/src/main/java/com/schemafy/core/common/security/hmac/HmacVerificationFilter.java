package com.schemafy.core.common.security.hmac;

import javax.crypto.SecretKey;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.hmac.HmacProperties.EnforcementMode;
import com.schemafy.core.common.security.jwt.WebExchangeErrorWriter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class HmacVerificationFilter implements WebFilter {

  static final String HEADER_SIGNATURE = "X-Hmac-Signature";
  static final String HEADER_TIMESTAMP = "X-Hmac-Timestamp";
  static final String HEADER_NONCE = "X-Hmac-Nonce";

  private final HmacProperties hmacProperties;
  private final NonceCache nonceCache;
  private final WebExchangeErrorWriter errorWriter;
  private final SecretKey primaryKey;
  private final SecretKey previousKey;

  public HmacVerificationFilter(HmacProperties hmacProperties,
      NonceCache nonceCache, WebExchangeErrorWriter errorWriter) {
    this.hmacProperties = hmacProperties;
    this.nonceCache = nonceCache;
    this.errorWriter = errorWriter;
    this.primaryKey = HmacUtil
        .createSecretKey(hmacProperties.getSecret());
    String prev = hmacProperties.getPreviousSecret();
    this.previousKey = (prev != null && !prev.isBlank())
        ? HmacUtil.createSecretKey(prev)
        : null;
  }

  @Override
  public Mono<Void> filter(@NonNull ServerWebExchange exchange,
      @NonNull WebFilterChain chain) {
    if (!hmacProperties.isEnabled()) {
      return chain.filter(exchange);
    }

    ServerHttpRequest request = exchange.getRequest();

    if (request.getMethod() == HttpMethod.OPTIONS) {
      return chain.filter(exchange);
    }

    String path = request.getPath().pathWithinApplication().value();
    if (path.startsWith("/public/api/") || path.startsWith("/ws/")) {
      return chain.filter(exchange);
    }

    String signature = request.getHeaders().getFirst(HEADER_SIGNATURE);
    String timestamp = request.getHeaders().getFirst(HEADER_TIMESTAMP);
    String nonce = request.getHeaders().getFirst(HEADER_NONCE);

    if (signature == null || timestamp == null || nonce == null) {
      return handleFailure(exchange, chain,
          ErrorCode.HMAC_SIGNATURE_MISSING);
    }

    if (!isTimestampValid(timestamp)) {
      return handleFailure(exchange, chain,
          ErrorCode.HMAC_TIMESTAMP_EXPIRED);
    }

    return nonceCache.isDuplicate(nonce).flatMap(duplicate -> {
      if (duplicate) {
        return handleFailure(exchange, chain,
            ErrorCode.HMAC_NONCE_DUPLICATE);
      }
      return DataBufferUtils.join(request.getBody())
          .defaultIfEmpty(
              DefaultDataBufferFactory.sharedInstance.allocateBuffer(0))
          .flatMap(dataBuffer -> {
            byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bodyBytes);
            DataBufferUtils.release(dataBuffer);

            String method = request.getMethod().name();
            String requestPath = request.getURI().getRawPath();
            String query = request.getURI().getRawQuery();
            if (query != null && !query.isEmpty()) {
              requestPath = requestPath + "?" + query;
            }
            String bodyHash = HmacUtil.computeBodyHash(bodyBytes);
            String canonical = HmacUtil.buildHttpCanonicalString(
                method, requestPath, timestamp, nonce, bodyHash);

            String computed = HmacUtil.computeHmac(primaryKey,
                canonical);
            boolean valid = HmacUtil.verifySignature(signature,
                computed);

            if (!valid && previousKey != null) {
              String fallback = HmacUtil.computeHmac(previousKey,
                  canonical);
              valid = HmacUtil.verifySignature(signature, fallback);
            }

            ServerHttpRequest cachedRequest = new ServerHttpRequestDecorator(
                request) {

              @Override
              public Flux<DataBuffer> getBody() {
                if (bodyBytes.length == 0) {
                  return Flux.empty();
                }
                DataBuffer buffer = DefaultDataBufferFactory.sharedInstance
                    .wrap(bodyBytes);
                return Flux.just(buffer);
              }

            };

            ServerWebExchange mutated = exchange.mutate()
                .request(cachedRequest).build();

            if (!valid) {
              return handleFailure(mutated, chain,
                  ErrorCode.HMAC_SIGNATURE_INVALID);
            }

            return chain.filter(mutated);
          });
    });
  }

  private boolean isTimestampValid(String timestampStr) {
    try {
      long timestamp = Long.parseLong(timestampStr);
      long now = System.currentTimeMillis();
      long toleranceMs = (long) hmacProperties
          .getTimestampToleranceSeconds() * 1000;
      return Math.abs(now - timestamp) <= toleranceMs;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private Mono<Void> handleFailure(ServerWebExchange exchange,
      WebFilterChain chain, ErrorCode errorCode) {
    if (hmacProperties
        .getEnforcementMode() == EnforcementMode.LOG_ONLY) {
      log.warn("[HMAC] Verification failed (LOG_ONLY mode): {}",
          errorCode.getCode());
      return chain.filter(exchange);
    }
    return errorWriter.writeErrorResponse(exchange,
        errorCode.getStatus(), errorCode.getCode(),
        errorCode.getMessage());
  }

}
