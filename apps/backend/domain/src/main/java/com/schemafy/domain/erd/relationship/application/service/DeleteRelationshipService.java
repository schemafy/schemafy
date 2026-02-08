package com.schemafy.domain.erd.relationship.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteRelationshipService implements DeleteRelationshipUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<MutationResult<Void>> deleteRelationship(DeleteRelationshipCommand command) {
    String relationshipId = command.relationshipId();

    return getRelationshipByIdPort.findRelationshipById(relationshipId)
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
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
                        new DeleteColumnCommand(fkColumnId))
                        .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                        .then())
                    .then()
                    .thenReturn(MutationResult.<Void>of(null, affectedTableIds));
              });
        })
        .as(transactionalOperator::transactional);
  }

}
