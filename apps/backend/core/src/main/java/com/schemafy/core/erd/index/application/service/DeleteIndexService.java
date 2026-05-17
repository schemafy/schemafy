package com.schemafy.core.erd.index.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.DeleteIndexInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteIndexService implements DeleteIndexUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteIndexPort deleteIndexPort;
  private final DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final StructuralSnapshotService structuralSnapshotService;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> deleteIndex(DeleteIndexCommand command) {
    String indexId = command.indexId();
    return erdMutationCoordinator.coordinate(ErdOperationType.DELETE_INDEX, command, () -> structuralSnapshotService
        .captureByIndexId(indexId).flatMap(beforeSnapshot -> getIndexByIdPort
            .findIndexById(indexId)
            .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
            .flatMap(index -> deleteIndexColumnsPort.deleteByIndexId(indexId)
                .then(deleteIndexPort.deleteIndex(indexId))
                .thenReturn(MutationResult.<Void>of(null, index.tableId())))
            .flatMap(result -> structuralSnapshotService.captureBySchemaId(beforeSnapshot.schemaId())
                .map(afterSnapshot -> result.withInverse(new DeleteIndexInverse(
                    beforeSnapshot.schemaId(),
                    indexId,
                    beforeSnapshot,
                    afterSnapshot,
                    result.sortedAffectedTableIds()))))))
        .as(transactionalOperator::transactional);
  }

}
