package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse.ConstraintRename;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse.RelationshipRename;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class ChangeTableNameUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeTableNameInverse> {

  private final ChangeTableNamePort changeTableNamePort;
  private final GetTableByIdPort getTableByIdPort;
  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  ChangeTableNameUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeTableNamePort changeTableNamePort,
      GetTableByIdPort getTableByIdPort,
      ChangeConstraintNamePort changeConstraintNamePort,
      GetConstraintByIdPort getConstraintByIdPort,
      ChangeRelationshipNamePort changeRelationshipNamePort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    super(ErdOperationType.CHANGE_TABLE_NAME, ChangeTableNameInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeTableNamePort = changeTableNamePort;
    this.getTableByIdPort = getTableByIdPort;
    this.changeConstraintNamePort = changeConstraintNamePort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.changeRelationshipNamePort = changeRelationshipNamePort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeTableNameInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getTableByIdPort.findTableById(inversePayload.tableId())
        .switchIfEmpty(Mono.error(new DomainException(
            TableErrorCode.NOT_FOUND,
            "Table not found: " + inversePayload.tableId())))
        .flatMap(table -> captureForwardSnapshot(table, inversePayload)
            .flatMap(forwardSnapshot -> coordinate(resolved, inversePayload,
                () -> applyTableInverse(inversePayload)
                    .thenReturn(MutationResult.<Void>of(null, table.id())
                        .withInverse(forwardSnapshot)))));
  }

  private Mono<ChangeTableNameInverse> captureForwardSnapshot(
      Table table,
      ChangeTableNameInverse inversePayload) {
    Mono<List<ConstraintRename>> constraintRenames = Flux.fromIterable(inversePayload.constraintRenames())
        .concatMap(rename -> findConstraint(rename.constraintId())
            .map(constraint -> new ConstraintRename(constraint.id(), constraint.name())))
        .collectList();
    Mono<List<RelationshipRename>> relationshipRenames = Flux.fromIterable(inversePayload.relationshipRenames())
        .concatMap(rename -> getRelationshipByIdPort.findRelationshipById(rename.relationshipId())
            .switchIfEmpty(Mono.error(new DomainException(
                RelationshipErrorCode.NOT_FOUND,
                "Relationship not found: " + rename.relationshipId())))
            .map(relationship -> new RelationshipRename(relationship.id(), relationship.name())))
        .collectList();

    return Mono.zip(constraintRenames, relationshipRenames)
        .map(tuple -> new ChangeTableNameInverse(
            table.id(),
            table.name(),
            inversePayload.oldPkConstraintId(),
            firstLegacyPkName(tuple.getT1(), inversePayload.oldPkConstraintId()),
            tuple.getT1(),
            tuple.getT2()));
  }

  private Mono<Constraint> findConstraint(String constraintId) {
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            "Constraint not found: " + constraintId)));
  }

  private static String firstLegacyPkName(
      List<ConstraintRename> constraintRenames,
      String legacyPkConstraintId) {
    if (legacyPkConstraintId == null) {
      return null;
    }
    return constraintRenames.stream()
        .filter(rename -> legacyPkConstraintId.equals(rename.constraintId()))
        .map(ConstraintRename::oldName)
        .findFirst()
        .orElse(null);
  }

  private Mono<Void> applyTableInverse(ChangeTableNameInverse inversePayload) {
    return changeTableNamePort.changeTableName(inversePayload.tableId(), inversePayload.oldName())
        .then(applyConstraintInverse(inversePayload.constraintRenames()))
        .then(applyRelationshipInverse(inversePayload.relationshipRenames()));
  }

  private Mono<Void> applyConstraintInverse(List<ConstraintRename> constraintRenames) {
    return Flux.fromIterable(constraintRenames)
        .concatMap(rename -> changeConstraintNamePort.changeConstraintName(
            rename.constraintId(),
            rename.oldName()))
        .then();
  }

  private Mono<Void> applyRelationshipInverse(List<RelationshipRename> relationshipRenames) {
    return Flux.fromIterable(relationshipRenames)
        .concatMap(rename -> changeRelationshipNamePort.changeRelationshipName(
            rename.relationshipId(),
            rename.oldName()))
        .then();
  }

}
