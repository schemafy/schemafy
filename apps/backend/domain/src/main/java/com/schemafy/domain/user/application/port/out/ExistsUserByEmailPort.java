package com.schemafy.domain.user.application.port.out;

import reactor.core.publisher.Mono;

public interface ExistsUserByEmailPort {

  Mono<Boolean> existsUserByEmail(String email);

}
