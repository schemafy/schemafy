package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNameUseCase {

  Mono<MutationResult<Void>> changeRelationshipName(ChangeRelationshipNameCommand command);

}
