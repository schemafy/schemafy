package com.schemafy.domain.erd.relationship.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipCardinalityService implements ChangeRelationshipCardinalityUseCase {

  private final ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipCardinality(
      ChangeRelationshipCardinalityCommand command) {
    if (command.cardinality() == null) {
      return Mono.error(new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "Relationship cardinality is required"));
    }
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          return changeRelationshipCardinalityPort
              .changeRelationshipCardinality(relationship.id(), command.cardinality())
              .thenReturn(MutationResult.<Void>of(null, affectedTableIds));
        });
  }

}
