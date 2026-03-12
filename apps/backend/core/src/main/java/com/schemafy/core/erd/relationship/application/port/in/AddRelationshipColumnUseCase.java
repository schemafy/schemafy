package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface AddRelationshipColumnUseCase {

  Mono<MutationResult<AddRelationshipColumnResult>> addRelationshipColumn(
      AddRelationshipColumnCommand command);

}
