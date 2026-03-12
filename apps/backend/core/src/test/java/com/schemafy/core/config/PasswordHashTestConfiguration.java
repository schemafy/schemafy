package com.schemafy.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.schemafy.core.user.application.port.out.PasswordHashPort;

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

}
