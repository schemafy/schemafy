package com.schemafy.core.erd.relationship.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipKindService implements ChangeRelationshipKindUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ChangeRelationshipKindPort changeRelationshipKindPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;
  private final PkCascadeHelper pkCascadeHelper;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeRelationshipKind(ChangeRelationshipKindCommand command) {
    if (command.kind() == null) {
      return Mono.error(new DomainException(RelationshipErrorCode.INVALID_VALUE, "Relationship kind is required"));
    }

    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          RelationshipKind oldKind = relationship.kind();
          RelationshipKind newKind = command.kind();

          if (oldKind == newKind) {
            return Mono.just(MutationResult.<Void>of(null, affectedTableIds));
          }

          return erdMutationCoordinator.coordinate(
              ErdOperationType.CHANGE_RELATIONSHIP_KIND,
              command,
              () -> changeRelationshipKind(
                  relationship,
                  oldKind,
                  newKind,
                  affectedTableIds)
                  .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds))));
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> changeRelationshipKind(
      Relationship relationship,
      RelationshipKind oldKind,
      RelationshipKind newKind,
      Set<String> affectedTableIds) {
    if (newKind != RelationshipKind.IDENTIFYING) {
      return syncPkAndChangeKind(
          relationship,
          oldKind,
          newKind,
          affectedTableIds);
    }
    return getTableByIdPort.findTableById(relationship.fkTableId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
            "Relationship fk table not found")))
        .flatMap(table -> getRelationshipsBySchemaIdPort
            .findRelationshipsBySchemaId(table.schemaId())
            .defaultIfEmpty(List.of())
            .flatMap(relationships -> validateCycleAndChange(
                relationship,
                relationships,
                oldKind,
                newKind,
                affectedTableIds)));
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
