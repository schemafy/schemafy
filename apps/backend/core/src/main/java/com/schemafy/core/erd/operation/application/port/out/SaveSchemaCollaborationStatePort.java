package com.schemafy.core.erd.operation.application.port.out;

import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;

import reactor.core.publisher.Mono;

public interface SaveSchemaCollaborationStatePort {

  Mono<SchemaCollaborationState> save(SchemaCollaborationState schemaCollaborationState);

}
