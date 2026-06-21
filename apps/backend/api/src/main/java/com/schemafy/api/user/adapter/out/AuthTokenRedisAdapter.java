package com.schemafy.api.user.adapter.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.api.common.security.hmac.HmacUtil;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnRedisEnabled
public class AuthTokenRedisAdapter implements AuthTokenPort {

  private static final String KEY_PREFIX = "auth-token:";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final JsonCodec jsonCodec;
  private final SecretKey secretKey;

  public AuthTokenRedisAdapter(
      ReactiveStringRedisTemplate redisTemplate,
      JsonCodec jsonCodec,
      @Value("${auth.token.secret}") String secret) {
    this.redisTemplate = redisTemplate;
    this.jsonCodec = jsonCodec;
    this.secretKey = HmacUtil.createSecretKey(secret);
  }

  @Override
  public Mono<Instant> findExpiresAt(AuthTokenType tokenType, String subject) {
    String key = key(tokenType, subject);
    return redisTemplate.opsForValue().get(key)
        .flatMap(serialized -> Mono.fromCallable(() -> jsonCodec.parse(
            serialized,
            AuthTokenPayload.class))
            .flatMap(payload -> redisTemplate.getExpire(key)
                .flatMap(ttl -> ttl.isPositive()
                    ? Mono.just(payload.expiresAt())
                    : redisTemplate.delete(key).then(Mono.empty())))
            .onErrorResume(error -> redisTemplate.delete(key).then(Mono.empty())));
  }

  @Override
  public Mono<Void> save(AuthToken token) {
    Duration ttl = Duration.between(Instant.now(), token.expiresAt());
    if (ttl.isNegative() || ttl.isZero()) {
      return Mono.empty();
    }
    AuthTokenPayload payload = payloadFrom(token);
    return Mono.fromCallable(() -> jsonCodec.serialize(payload))
        .flatMap(serialized -> redisTemplate.opsForValue()
            .set(key(token.tokenType(), token.subject()), serialized, ttl))
        .then();
  }

  @Override
  public Mono<Boolean> saveIfAbsent(AuthToken token) {
    Duration ttl = Duration.between(Instant.now(), token.expiresAt());
    if (ttl.isNegative() || ttl.isZero()) {
      return Mono.just(false);
    }
    AuthTokenPayload payload = payloadFrom(token);
    return Mono.fromCallable(() -> jsonCodec.serialize(payload))
        .flatMap(serialized -> redisTemplate.opsForValue()
            .setIfAbsent(key(token.tokenType(), token.subject()), serialized, ttl))
        .defaultIfEmpty(false);
  }

  @Override
  public Mono<Void> delete(AuthTokenType tokenType, String subject) {
    return redisTemplate.delete(key(tokenType, subject)).then();
  }

  @Override
  public Mono<AuthTokenConsumeResult> consume(AuthTokenType tokenType,
      String subject, String rawToken) {
    return redisTemplate.execute(
        AuthTokenRedisScripts.CONSUME_TOKEN,
        List.of(key(tokenType, subject)),
        List.of(hash(tokenType, subject, rawToken)))
        .next()
        .map(AuthTokenConsumeResult::valueOf)
        .defaultIfEmpty(AuthTokenConsumeResult.MISSING);
  }

  private String hash(AuthTokenType tokenType, String subject,
      String rawToken) {
    return HmacUtil.computeHmac(secretKey,
        tokenType.name() + ":" + subject + ":" + rawToken);
  }

  private AuthTokenPayload payloadFrom(AuthToken token) {
    return new AuthTokenPayload(
        hash(token.tokenType(), token.subject(), token.token()),
        token.attemptCount(),
        token.maxAttemptCount(),
        token.expiresAt());
  }

  private String key(AuthTokenType tokenType, String subject) {
    return KEY_PREFIX + tokenType.name() + ":" + subject;
  }

  private record AuthTokenPayload(
      String tokenHash,
      int attemptCount,
      int maxAttemptCount,
      Instant expiresAt) {

  }

}
