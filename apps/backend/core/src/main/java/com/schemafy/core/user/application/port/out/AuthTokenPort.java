package com.schemafy.core.user.application.port.out;

import java.time.Instant;

import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Mono;

public interface AuthTokenPort {

  Mono<Instant> findExpiresAt(AuthTokenType tokenType, String subject);

  Mono<Void> save(AuthToken token);

  Mono<Boolean> saveIfAbsent(AuthToken token);

  Mono<Void> delete(AuthTokenType tokenType, String subject);

  Mono<AuthTokenConsumeResult> consume(AuthTokenType tokenType, String subject, String rawToken);

}
