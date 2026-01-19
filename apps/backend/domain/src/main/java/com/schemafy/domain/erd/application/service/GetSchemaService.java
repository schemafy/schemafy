package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.domain.Schema;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetSchemaService {

  private final GetSchemaByIdPort getSchemaByIdPort;

  public Mono<Schema> findSchemaById(String schemaId) {
    return getSchemaByIdPort.findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(new RuntimeException("Schema not found")));
  }

}
