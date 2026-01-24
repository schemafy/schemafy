package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteSchemaService implements DeleteSchemaUseCase {

  private final DeleteSchemaPort deleteSchemaPort;

  @Override
  public Mono<Void> deleteSchema(DeleteSchemaCommand command) {
    return deleteSchemaPort.deleteSchema(command.schemaId());
  }

}
