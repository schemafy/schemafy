package com.schemafy.core.user.application.port.out;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface SendEmailVerificationPort {

  Mono<Void> sendVerificationCode(String email, String code, Instant expiresAt);

}
