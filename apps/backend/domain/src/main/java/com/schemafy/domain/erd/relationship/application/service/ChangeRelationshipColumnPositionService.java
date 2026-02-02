package com.schemafy.domain.erd.relationship.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
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

  @Override
  public Mono<Void> changeRelationshipColumnPosition(ChangeRelationshipColumnPositionCommand command) {
    if (command.seqNo() < 0) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column position must be zero or positive"));
    }
    return getRelationshipColumnByIdPort.findRelationshipColumnById(command.relationshipColumnId())
        .switchIfEmpty(Mono.error(new RelationshipPositionInvalidException(
            "Relationship column not found")))
        .flatMap(relationshipColumn -> getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipColumn.relationshipId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> reorderColumns(
                relationshipColumn,
                columns,
                command.seqNo())));
  }

  private Mono<Void> reorderColumns(
      RelationshipColumn relationshipColumn,
      List<RelationshipColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column not found"));
    }
    if (nextPosition < 0 || nextPosition >= columns.size()) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column position must be between 0 and %d".formatted(columns.size() - 1)));
    }

    List<RelationshipColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, relationshipColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new RelationshipPositionInvalidException(
          "Relationship column not found"));
    }
    RelationshipColumn movingColumn = reordered.remove(currentIndex);
    reordered.add(nextPosition, movingColumn);

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
