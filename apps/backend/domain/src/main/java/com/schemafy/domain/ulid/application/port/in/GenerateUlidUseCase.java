package com.schemafy.domain.ulid.application.port.in;

import reactor.core.publisher.Mono;

public interface GenerateUlidUseCase {

  Mono<String> generateUlid();

}
