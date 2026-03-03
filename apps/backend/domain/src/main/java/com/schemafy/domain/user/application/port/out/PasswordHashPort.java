package com.schemafy.domain.user.application.port.out;

import reactor.core.publisher.Mono;

public interface PasswordHashPort {

  Mono<String> hash(String rawPassword);

  Mono<Boolean> matches(String rawPassword, String encodedPassword);

}
