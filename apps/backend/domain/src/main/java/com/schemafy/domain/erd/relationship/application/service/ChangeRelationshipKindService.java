package com.schemafy.domain.erd.relationship.application.service;

import com.schemafy.domain.common.exception.InvalidValueException;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipKindService implements ChangeRelationshipKindUseCase {

  private final ChangeRelationshipKindPort changeRelationshipKindPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<Void> changeRelationshipKind(ChangeRelationshipKindCommand command) {
    if (command.kind() == null) {
      return Mono.error(new InvalidValueException("Relationship kind is required"));
    }
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          RelationshipKind oldKind = relationship.kind();
          RelationshipKind newKind = command.kind();

          if (oldKind == newKind) {
            return Mono.empty();
          }

          if (newKind != RelationshipKind.IDENTIFYING) {
            return syncPkAndChangeKind(relationship, oldKind, newKind);
          }
          return getTableByIdPort.findTableById(relationship.fkTableId())
              .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
                  "Relationship fk table not found")))
              .flatMap(table -> getRelationshipsBySchemaIdPort
                  .findRelationshipsBySchemaId(table.schemaId())
                  .defaultIfEmpty(List.of())
                  .flatMap(relationships -> validateCycleAndChange(
                      relationship,
                      relationships,
                      oldKind,
                      newKind)));
        });
  }

  private Mono<Void> validateCycleAndChange(
      Relationship relationship,
      List<Relationship> relationships,
      RelationshipKind oldKind,
      RelationshipKind newKind) {
    RelationshipValidator.validateIdentifyingCycle(
        relationships,
        new RelationshipValidator.RelationshipKindChange(
            relationship.id(),
            newKind),
        null);
    return syncPkAndChangeKind(relationship, oldKind, newKind);
  }

  private Mono<Void> syncPkAndChangeKind(
      Relationship relationship,
      RelationshipKind oldKind,
      RelationshipKind newKind) {
    return pkCascadeHelper.syncPkForKindChange(relationship, oldKind, newKind, new HashSet<>())
        .then(changeRelationshipKindPort.changeRelationshipKind(relationship.id(), newKind));
  }

}
