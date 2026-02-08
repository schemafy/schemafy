package com.schemafy.domain.erd.relationship.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipNameService implements ChangeRelationshipNameUseCase {

  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipName(ChangeRelationshipNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      RelationshipValidator.validateName(normalizedName);
      return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
          .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
          .flatMap(relationship -> relationshipExistsPort.existsByFkTableIdAndNameExcludingId(
              relationship.fkTableId(),
              normalizedName,
              relationship.id())
              .flatMap(exists -> {
                if (exists) {
                  return Mono.error(new RelationshipNameDuplicateException(
                      "Relationship name '%s' already exists in table".formatted(normalizedName)));
                }
                Set<String> affectedTableIds = new HashSet<>();
                affectedTableIds.add(relationship.fkTableId());
                affectedTableIds.add(relationship.pkTableId());
                return changeRelationshipNamePort
                    .changeRelationshipName(relationship.id(), normalizedName)
                    .thenReturn(MutationResult.<Void>of(null, affectedTableIds));
              }));
    });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
