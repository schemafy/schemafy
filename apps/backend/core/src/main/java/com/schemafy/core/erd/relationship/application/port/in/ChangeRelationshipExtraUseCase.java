package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipExtraUseCase {

  Mono<MutationResult<Void>> changeRelationshipExtra(ChangeRelationshipExtraCommand command);

}
