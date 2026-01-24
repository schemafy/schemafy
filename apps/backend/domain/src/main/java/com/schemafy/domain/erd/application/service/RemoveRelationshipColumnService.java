package com.schemafy.domain.erd.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.application.port.in.RemoveRelationshipColumnUseCase;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.domain.RelationshipColumn;
import com.schemafy.domain.erd.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.domain.exception.RelationshipNotExistException;

import reactor.core.publisher.Mono;

@Service
public class RemoveRelationshipColumnService implements RemoveRelationshipColumnUseCase {

  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  public RemoveRelationshipColumnService(
      DeleteRelationshipColumnPort deleteRelationshipColumnPort,
      DeleteRelationshipPort deleteRelationshipPort,
      ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort,
      GetRelationshipByIdPort getRelationshipByIdPort,
      GetRelationshipColumnByIdPort getRelationshipColumnByIdPort,
      GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort) {
    this.deleteRelationshipColumnPort = deleteRelationshipColumnPort;
    this.deleteRelationshipPort = deleteRelationshipPort;
    this.changeRelationshipColumnPositionPort = changeRelationshipColumnPositionPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
    this.getRelationshipColumnByIdPort = getRelationshipColumnByIdPort;
    this.getRelationshipColumnsByRelationshipIdPort = getRelationshipColumnsByRelationshipIdPort;
  }

  @Override
  public Mono<Void> removeRelationshipColumn(RemoveRelationshipColumnCommand command) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> getRelationshipColumnByIdPort
            .findRelationshipColumnById(command.relationshipColumnId())
            .switchIfEmpty(Mono.error(new RelationshipColumnNotExistException(
                "Relationship column not found")))
            .flatMap(relationshipColumn -> {
              if (!relationshipColumn.relationshipId().equalsIgnoreCase(relationship.id())) {
                return Mono.error(new RelationshipColumnNotExistException(
                    "Relationship column not found"));
              }
              return deleteRelationshipColumnPort
                  .deleteRelationshipColumn(relationshipColumn.id())
                  .then(handleRemainingColumns(relationship.id()));
            }));
  }

  private Mono<Void> handleRemainingColumns(String relationshipId) {
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationshipId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteRelationshipPort.deleteRelationship(relationshipId);
          }
          List<RelationshipColumn> reordered = new ArrayList<>(columns.size());
          for (int index = 0; index < columns.size(); index++) {
            RelationshipColumn column = columns.get(index);
            reordered.add(new RelationshipColumn(
                column.id(),
                column.relationshipId(),
                column.pkColumnId(),
                column.fkColumnId(),
                index));
          }
          return changeRelationshipColumnPositionPort
              .changeRelationshipColumnPositions(relationshipId, reordered);
        });
  }
}
