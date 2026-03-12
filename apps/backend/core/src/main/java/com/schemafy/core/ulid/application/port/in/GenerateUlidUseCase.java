package com.schemafy.core.ulid.application.port.in;

import reactor.core.publisher.Mono;

public interface GenerateUlidUseCase {

  Mono<String> generateUlid();

}
