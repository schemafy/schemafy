package com.schemafy.core.user.adapter.out;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.schemafy.domain.user.application.port.out.PasswordHashPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PasswordHashAdapter implements PasswordHashPort {

  private final PasswordEncoder passwordEncoder;

  @Override
  public Mono<String> hash(String rawPassword) {
    return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<Boolean> matches(String rawPassword, String encodedPassword) {
    return Mono.fromCallable(
        () -> passwordEncoder.matches(rawPassword, encodedPassword))
        .subscribeOn(Schedulers.boundedElastic());
  }

}
