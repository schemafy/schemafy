package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipExtraService implements ChangeRelationshipExtraUseCase {

  private final ChangeRelationshipExtraPort changeRelationshipExtraPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipExtra(ChangeRelationshipExtraCommand command) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          return changeRelationshipExtraPort
              .changeRelationshipExtra(relationship.id(), normalizeOptional(command.extra()))
              .thenReturn(MutationResult.<Void>of(null, affectedTableIds));
        });
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

}
