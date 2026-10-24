package com.schemafy.api.testsupport.user;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.schemafy.core.user.application.port.out.SendPasswordResetEmailPort;

import reactor.core.publisher.Mono;

@Component
@Profile("test")
public class CapturingPasswordResetEmailTestAdapter implements SendPasswordResetEmailPort {

  private final Map<String, SentPasswordResetLink> sentLinks = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> sendPasswordResetLink(String email, String resetToken, Instant expiresAt) {
    sentLinks.put(email, new SentPasswordResetLink(email, resetToken, expiresAt));
    return Mono.empty();
  }

  public SentPasswordResetLink get(String email) {
    return sentLinks.get(email);
  }

  public void clear() {
    sentLinks.clear();
  }

  public record SentPasswordResetLink(String email, String resetToken, Instant expiresAt) {
  }

}
