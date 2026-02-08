package com.schemafy.domain.erd.relationship.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipPositionInvalidException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeRelationshipColumnPositionService
    implements ChangeRelationshipColumnPositionUseCase {

  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipColumnPosition(
      ChangeRelationshipColumnPositionCommand command) {
    return getRelationshipColumnByIdPort.findRelationshipColumnById(command.relationshipColumnId())
        .switchIfEmpty(Mono.error(new RelationshipPositionInvalidException(
            "Relationship column not found")))
        .flatMap(relationshipColumn -> getRelationshipByIdPort
            .findRelationshipById(relationshipColumn.relationshipId())
            .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
            .flatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
                .findRelationshipColumnsByRelationshipId(relationshipColumn.relationshipId())
                .defaultIfEmpty(List.of())
                .flatMap(columns -> reorderColumns(
                    relationshipColumn,
                    columns,
                    command.seqNo()))
                .thenReturn(MutationResult.<Void>of(null, toTableIdSet(relationship)))));
  }

  private static Set<String> toTableIdSet(Relationship relationship) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(relationship.fkTableId());
    affectedTableIds.add(relationship.pkTableId());
    return affectedTableIds;
  }

  private Mono<Void> reorderColumns(
      RelationshipColumn relationshipColumn,
      List<RelationshipColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column not found"));
    }

    List<RelationshipColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, relationshipColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column not found"));
    }
    RelationshipColumn movingColumn = reordered.remove(currentIndex);
    int normalizedPosition = Math.clamp(nextPosition, 0, columns.size() - 1);
    reordered.add(normalizedPosition, movingColumn);

    List<RelationshipColumn> updated = new ArrayList<>(reordered.size());
    for (int index = 0; index < reordered.size(); index++) {
      RelationshipColumn column = reordered.get(index);
      updated.add(new RelationshipColumn(
          column.id(),
          column.relationshipId(),
          column.pkColumnId(),
          column.fkColumnId(),
          index));
    }

    return changeRelationshipColumnPositionPort
        .changeRelationshipColumnPositions(relationshipColumn.relationshipId(), updated);
  }

  private static int findIndex(List<RelationshipColumn> columns, String relationshipColumnId) {
    for (int index = 0; index < columns.size(); index++) {
      if (equalsIgnoreCase(columns.get(index).id(), relationshipColumnId)) {
        return index;
      }
    }
    return -1;
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

}
