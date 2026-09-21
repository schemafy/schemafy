package com.schemafy.api.testsupport.user;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Mono;

@Component
@Profile("test")
public class InMemoryAuthTokenTestAdapter implements AuthTokenPort {

  private final Map<String, AuthToken> tokens = new ConcurrentHashMap<>();

  @Override
  public Mono<Instant> findExpiresAt(AuthTokenType tokenType, String subject) {
    String key = key(tokenType, subject);
    AuthToken token = tokens.get(key);
    if (token == null || Instant.now().isAfter(token.expiresAt())) {
      tokens.remove(key);
      return Mono.empty();
    }
    return Mono.just(token.expiresAt());
  }

  @Override
  public Mono<Void> save(AuthToken token) {
    tokens.put(key(token.tokenType(), token.subject()), token);
    return Mono.empty();
  }

  @Override
  public Mono<Boolean> saveIfAbsent(AuthToken token) {
    String key = key(token.tokenType(), token.subject());
    AuthToken existingToken = tokens.get(key);
    if (existingToken != null && Instant.now().isBefore(existingToken.expiresAt())) {
      return Mono.just(false);
    }
    tokens.put(key, token);
    return Mono.just(true);
  }

  @Override
  public Mono<Void> delete(AuthTokenType tokenType, String subject) {
    tokens.remove(key(tokenType, subject));
    return Mono.empty();
  }

  @Override
  public Mono<AuthTokenConsumeResult> consume(AuthTokenType tokenType,
      String subject, String rawToken) {
    String key = key(tokenType, subject);
    AuthToken token = tokens.get(key);
    if (token == null || Instant.now().isAfter(token.expiresAt())) {
      tokens.remove(key);
      return Mono.just(AuthTokenConsumeResult.MISSING);
    }
    if (token.attemptCount() >= token.maxAttemptCount()) {
      tokens.remove(key);
      return Mono.just(AuthTokenConsumeResult.ATTEMPTS_EXCEEDED);
    }
    if (token.token().equals(rawToken)) {
      tokens.remove(key);
      return Mono.just(AuthTokenConsumeResult.CONSUMED);
    }
    int nextAttemptCount = token.attemptCount() + 1;
    if (nextAttemptCount >= token.maxAttemptCount()) {
      tokens.remove(key);
      return Mono.just(AuthTokenConsumeResult.ATTEMPTS_EXCEEDED);
    }
    tokens.put(key, new AuthToken(
        token.tokenType(),
        token.subject(),
        token.token(),
        nextAttemptCount,
        token.maxAttemptCount(),
        token.expiresAt()));
    return Mono.just(AuthTokenConsumeResult.MISMATCH);
  }

  public void clear() {
    tokens.clear();
  }

  private String key(AuthTokenType tokenType, String userId) {
    return tokenType.name() + ":" + userId;
  }

}
