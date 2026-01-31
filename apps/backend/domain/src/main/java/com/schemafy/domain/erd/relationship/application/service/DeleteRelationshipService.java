package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteRelationshipService implements DeleteRelationshipUseCase {

  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;

  @Override
  public Mono<Void> deleteRelationship(DeleteRelationshipCommand command) {
    String relationshipId = command.relationshipId();

    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationshipId)
        .defaultIfEmpty(List.of())
        .flatMap(relColumns -> {
          List<String> fkColumnIds = relColumns.stream()
              .map(RelationshipColumn::fkColumnId)
              .toList();

          return deleteRelationshipColumnsPort.deleteByRelationshipId(relationshipId)
              .then(deleteRelationshipPort.deleteRelationship(relationshipId))
              .thenMany(Flux.fromIterable(fkColumnIds))
              .concatMap(fkColumnId -> deleteColumnUseCase.deleteColumn(
                  new DeleteColumnCommand(fkColumnId)))
              .then();
        });
  }

}
