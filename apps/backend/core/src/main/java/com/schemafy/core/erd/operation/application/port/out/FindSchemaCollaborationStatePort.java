package com.schemafy.core.erd.operation.application.port.out;

import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

import reactor.core.publisher.Mono;

public interface FindSchemaCollaborationStatePort {

  Mono<SchemaCollaborationState> findBySchemaId(String schemaId);

}
