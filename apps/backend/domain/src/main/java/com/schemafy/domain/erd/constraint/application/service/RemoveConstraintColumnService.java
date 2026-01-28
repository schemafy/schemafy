package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RemoveConstraintColumnService implements RemoveConstraintColumnUseCase {

  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final DeleteConstraintPort deleteConstraintPort;
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;
  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;

  public RemoveConstraintColumnService(
      DeleteConstraintColumnPort deleteConstraintColumnPort,
      DeleteConstraintPort deleteConstraintPort,
      ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetConstraintColumnByIdPort getConstraintColumnByIdPort,
      GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort,
      GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort,
      GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort,
      DeleteRelationshipPort deleteRelationshipPort,
      DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort,
      DeleteRelationshipColumnPort deleteRelationshipColumnPort) {
    this.deleteConstraintColumnPort = deleteConstraintColumnPort;
    this.deleteConstraintPort = deleteConstraintPort;
    this.changeConstraintColumnPositionPort = changeConstraintColumnPositionPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getConstraintColumnByIdPort = getConstraintColumnByIdPort;
    this.getConstraintColumnsByConstraintIdPort = getConstraintColumnsByConstraintIdPort;
    this.getRelationshipsByPkTableIdPort = getRelationshipsByPkTableIdPort;
    this.getRelationshipColumnsByRelationshipIdPort = getRelationshipColumnsByRelationshipIdPort;
    this.deleteRelationshipPort = deleteRelationshipPort;
    this.deleteRelationshipColumnsByRelationshipIdPort = deleteRelationshipColumnsByRelationshipIdPort;
    this.deleteRelationshipColumnPort = deleteRelationshipColumnPort;
  }

  @Override
  public Mono<Void> removeConstraintColumn(RemoveConstraintColumnCommand command) {
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new ConstraintNotExistException(
            "Constraint not found: " + command.constraintId())))
        .flatMap(constraint -> getConstraintColumnByIdPort
            .findConstraintColumnById(command.constraintColumnId())
            .switchIfEmpty(Mono.error(new ConstraintColumnNotExistException(
                "Constraint column not found: " + command.constraintColumnId())))
            .flatMap(constraintColumn -> {
              if (!constraintColumn.constraintId().equalsIgnoreCase(constraint.id())) {
                return Mono.error(new ConstraintColumnNotExistException(
                    "Constraint column does not belong to the constraint"));
              }
              return deleteConstraintColumnPort.deleteConstraintColumn(constraintColumn.id())
                  .then(handlePkConstraintColumnRemoval(constraint, constraintColumn.columnId()))
                  .then(reorderOrDeleteConstraint(constraint.id()));
            }));
  }

  /**
   * PK Constraint에서 컬럼이 제거될 때, 해당 컬럼을 pkColumnId로 참조하는
   * RelationshipColumn들을 삭제하고, 마지막 RelationshipColumn이면 Relationship도 삭제
   */
  private Mono<Void> handlePkConstraintColumnRemoval(Constraint constraint, String columnId) {
    if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
      return Mono.empty();
    }

    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(constraint.tableId())
        .flatMapMany(Flux::fromIterable)
        .flatMap(relationship -> cascadeDeleteRelationshipColumnsByPkColumnId(relationship, columnId))
        .then();
  }

  /**
   * 특정 Relationship에서 pkColumnId가 일치하는 RelationshipColumn을 삭제하고,
   * 마지막 RelationshipColumn이 삭제되면 Relationship 자체도 삭제
   */
  private Mono<Void> cascadeDeleteRelationshipColumnsByPkColumnId(
      Relationship relationship, String pkColumnId) {
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationship.id())
        .flatMap(columns -> {
          List<RelationshipColumn> toRemove = columns.stream()
              .filter(col -> col.pkColumnId().equals(pkColumnId))
              .toList();

          if (toRemove.isEmpty()) {
            return Mono.empty();
          }

          List<RelationshipColumn> remaining = columns.stream()
              .filter(col -> !col.pkColumnId().equals(pkColumnId))
              .toList();

          if (remaining.isEmpty()) {
            // 마지막 RelationshipColumn이면 Relationship 자체 삭제
            return deleteRelationshipColumnsByRelationshipIdPort
                .deleteByRelationshipId(relationship.id())
                .then(deleteRelationshipPort.deleteRelationship(relationship.id()));
          } else {
            // 해당 pkColumnId를 가진 RelationshipColumn만 삭제
            return Flux.fromIterable(toRemove)
                .flatMap(col -> deleteRelationshipColumnPort.deleteRelationshipColumn(col.id()))
                .then();
          }
        });
  }

  /**
   * 제약조건 컬럼 삭제 후 남은 컬럼들의 seqNo를 재정렬하거나,
   * 컬럼이 없으면 제약조건 자체를 삭제
   */
  private Mono<Void> reorderOrDeleteConstraint(String constraintId) {
    return getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteConstraintPort.deleteConstraint(constraintId);
          }
          List<ConstraintColumn> reordered = new ArrayList<>(columns.size());
          for (int index = 0; index < columns.size(); index++) {
            ConstraintColumn column = columns.get(index);
            reordered.add(new ConstraintColumn(
                column.id(),
                column.constraintId(),
                column.columnId(),
                index));
          }
          return changeConstraintColumnPositionPort
              .changeConstraintColumnPositions(constraintId, reordered);
        });
  }

}
