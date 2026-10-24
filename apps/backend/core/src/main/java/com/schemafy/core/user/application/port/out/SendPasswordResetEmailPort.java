package com.schemafy.core.user.application.port.out;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface SendPasswordResetEmailPort {

  Mono<Void> sendPasswordResetLink(String email, String resetToken, Instant expiresAt);

}
