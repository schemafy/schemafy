package com.schemafy.api.user.adapter.out;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.schemafy.api.common.security.hmac.HmacUtil;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Mono;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class AuthTokenLocalAdapter implements AuthTokenPort {

  private final Object tokensLock = new Object();
  private final Map<String, StoredToken> tokens = new HashMap<>();
  private final SecretKey secretKey;

  public AuthTokenLocalAdapter(@Value("${auth.token.secret}") String secret) {
    this.secretKey = HmacUtil.createSecretKey(secret);
  }

  @Override
  public Mono<Instant> findExpiresAt(AuthTokenType tokenType, String subject) {
    return Mono.fromCallable(() -> {
      String key = key(tokenType, subject);
      synchronized (tokensLock) {
        StoredToken token = tokens.get(key);
        if (token == null || token.isExpired()) {
          tokens.remove(key);
          return null;
        }
        return token.expiresAt();
      }
    });
  }

  @Override
  public Mono<Void> save(AuthToken token) {
    return Mono.fromRunnable(() -> {
      if (!isValidTtl(token)) {
        return;
      }
      synchronized (tokensLock) {
        tokens.put(key(token.tokenType(), token.subject()), storedFrom(token));
      }
    });
  }

  @Override
  public Mono<Boolean> saveIfAbsent(AuthToken token) {
    return Mono.fromCallable(() -> {
      if (!isValidTtl(token)) {
        return false;
      }
      String key = key(token.tokenType(), token.subject());
      synchronized (tokensLock) {
        StoredToken existingToken = tokens.get(key);
        if (existingToken != null && !existingToken.isExpired()) {
          return false;
        }
        tokens.put(key, storedFrom(token));
        return true;
      }
    });
  }

  @Override
  public Mono<Void> delete(AuthTokenType tokenType, String subject) {
    return Mono.fromRunnable(() -> {
      synchronized (tokensLock) {
        tokens.remove(key(tokenType, subject));
      }
    });
  }

  @Override
  public Mono<AuthTokenConsumeResult> consume(AuthTokenType tokenType,
      String subject, String rawToken) {
    return Mono.fromCallable(() -> {
      String key = key(tokenType, subject);
      synchronized (tokensLock) {
        StoredToken token = tokens.get(key);
        if (token == null || token.isExpired()) {
          tokens.remove(key);
          return AuthTokenConsumeResult.MISSING;
        }
        if (token.attemptCount() >= token.maxAttemptCount()) {
          tokens.remove(key);
          return AuthTokenConsumeResult.ATTEMPTS_EXCEEDED;
        }
        if (token.tokenHash().equals(hash(tokenType, subject, rawToken))) {
          tokens.remove(key);
          return AuthTokenConsumeResult.CONSUMED;
        }

        int nextAttemptCount = token.attemptCount() + 1;
        if (nextAttemptCount >= token.maxAttemptCount()) {
          tokens.remove(key);
          return AuthTokenConsumeResult.ATTEMPTS_EXCEEDED;
        }
        tokens.put(key, token.withAttemptCount(nextAttemptCount));
        return AuthTokenConsumeResult.MISMATCH;
      }
    });
  }

  private boolean isValidTtl(AuthToken token) {
    return token.expiresAt().isAfter(Instant.now());
  }

  private StoredToken storedFrom(AuthToken token) {
    return new StoredToken(
        hash(token.tokenType(), token.subject(), token.token()),
        token.attemptCount(),
        token.maxAttemptCount(),
        token.expiresAt());
  }

  private String hash(AuthTokenType tokenType, String subject,
      String rawToken) {
    return HmacUtil.computeHmac(secretKey,
        tokenType.name() + ":" + subject + ":" + rawToken);
  }

  private String key(AuthTokenType tokenType, String subject) {
    return tokenType.name() + ":" + subject;
  }

  private record StoredToken(
      String tokenHash,
      int attemptCount,
      int maxAttemptCount,
      Instant expiresAt) {

    boolean isExpired() { return !expiresAt.isAfter(Instant.now()); }

    StoredToken withAttemptCount(int nextAttemptCount) {
      return new StoredToken(tokenHash, nextAttemptCount, maxAttemptCount, expiresAt);
    }

  }

}
