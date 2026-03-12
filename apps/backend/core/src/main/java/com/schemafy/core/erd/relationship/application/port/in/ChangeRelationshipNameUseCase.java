package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNameUseCase {

  Mono<MutationResult<Void>> changeRelationshipName(ChangeRelationshipNameCommand command);

}
