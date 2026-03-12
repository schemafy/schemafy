package com.schemafy.domain.ulid.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.ulid.application.port.in.GenerateUlidUseCase;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GenerateUlidService implements GenerateUlidUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;

  @Override
  public Mono<String> generateUlid() {
    return Mono.fromCallable(ulidGeneratorPort::generate);
  }

}
