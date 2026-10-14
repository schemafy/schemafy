package com.schemafy.api.testsupport.user;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.schemafy.core.user.application.port.out.SendEmailVerificationPort;

import reactor.core.publisher.Mono;

@Component
@Profile("test")
public class CapturingEmailVerificationTestAdapter implements SendEmailVerificationPort {

  private final Map<String, SentVerificationCode> sentCodes = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> sendCounts = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> sendVerificationCode(String email, String code,
      Instant expiresAt) {
    sentCodes.put(email, new SentVerificationCode(email, code, expiresAt));
    sendCounts.computeIfAbsent(email, ignored -> new AtomicInteger())
        .incrementAndGet();
    return Mono.empty();
  }

  public SentVerificationCode get(String email) {
    return sentCodes.get(email);
  }

  public int sendCount(String email) {
    AtomicInteger sendCount = sendCounts.get(email);
    if (sendCount == null) {
      return 0;
    }
    return sendCount.get();
  }

  public void clear() {
    sentCodes.clear();
    sendCounts.clear();
  }

  public record SentVerificationCode(String email, String code,
      Instant expiresAt) {
  }

}
