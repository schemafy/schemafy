package com.schemafy.core.config;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.PasswordHashPort;
import com.schemafy.core.user.application.port.out.SendEmailVerificationPort;
import com.schemafy.core.user.application.security.SignupVerificationTokenGenerator;
import com.schemafy.core.user.application.security.VerificationCodeGenerator;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Mono;

@TestConfiguration
public class PasswordHashTestConfiguration {

  private static final String TEST_PREFIX = "{test}";

  @Bean
  public PasswordHashPort passwordHashPort() {
    return new PasswordHashPort() {

      @Override
      public Mono<String> hash(String rawPassword) {
        return Mono.just(TEST_PREFIX + rawPassword);
      }

      @Override
      public Mono<Boolean> matches(String rawPassword, String encodedPassword) {
        return Mono.just((TEST_PREFIX + rawPassword).equals(encodedPassword));
      }

    };
  }

  @Bean
  public VerificationCodeGenerator verificationCodeGenerator() {
    return new VerificationCodeGenerator() {

      @Override
      public String generate() {
        return "123456";
      }

    };
  }

  @Bean
  public SignupVerificationTokenGenerator signupVerificationTokenGenerator() {
    return new SignupVerificationTokenGenerator() {

      @Override
      public String generate() {
        return "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
      }

    };
  }

  @Bean
  public SendEmailVerificationPort sendEmailVerificationPort() {
    return (email, code, expiresAt) -> Mono.empty();
  }

  @Bean
  public AuthTokenPort authTokenPort() {
    return new AuthTokenPort() {

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
        if (token.token().equals(rawToken)) {
          tokens.remove(key);
          return Mono.just(AuthTokenConsumeResult.CONSUMED);
        }
        return Mono.just(AuthTokenConsumeResult.MISMATCH);
      }

      private String key(AuthTokenType tokenType, String subject) {
        return tokenType.name() + ":" + subject;
      }

    };
  }

}
