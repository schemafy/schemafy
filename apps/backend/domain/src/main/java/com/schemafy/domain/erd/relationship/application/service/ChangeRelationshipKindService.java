package com.schemafy.domain.erd.relationship.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.InvalidValueException;
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
  private final TransactionalOperator transactionalOperator;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipKind(ChangeRelationshipKindCommand command) {
    if (command.kind() == null) {
      return Mono.error(new InvalidValueException("Relationship kind is required"));
    }
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          RelationshipKind oldKind = relationship.kind();
          RelationshipKind newKind = command.kind();

          if (oldKind == newKind) {
            return Mono.just(MutationResult.<Void>of(null, affectedTableIds));
          }

          if (newKind != RelationshipKind.IDENTIFYING) {
            return syncPkAndChangeKind(
                relationship,
                oldKind,
                newKind,
                affectedTableIds)
                .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
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
                      newKind,
                      affectedTableIds))
                  .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds))));
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> validateCycleAndChange(
      Relationship relationship,
      List<Relationship> relationships,
      RelationshipKind oldKind,
      RelationshipKind newKind,
      Set<String> affectedTableIds) {
    RelationshipValidator.validateIdentifyingCycle(
        relationships,
        new RelationshipValidator.RelationshipKindChange(
            relationship.id(),
            newKind),
        null);
    return syncPkAndChangeKind(relationship, oldKind, newKind, affectedTableIds);
  }

  private Mono<Void> syncPkAndChangeKind(
      Relationship relationship,
      RelationshipKind oldKind,
      RelationshipKind newKind,
      Set<String> affectedTableIds) {
    return pkCascadeHelper.syncPkForKindChange(
        relationship,
        oldKind,
        newKind,
        new HashSet<>(),
        affectedTableIds)
        .then(changeRelationshipKindPort.changeRelationshipKind(relationship.id(), newKind));
  }

}
