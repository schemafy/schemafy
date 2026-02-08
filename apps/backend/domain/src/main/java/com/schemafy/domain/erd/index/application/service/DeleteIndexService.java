package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteIndexService implements DeleteIndexUseCase {

  private final DeleteIndexPort deleteIndexPort;
  private final DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;
  private final TransactionalOperator transactionalOperator;
  private final GetIndexByIdPort getIndexByIdPort;

  @Override
  public Mono<MutationResult<Void>> deleteIndex(DeleteIndexCommand command) {
    String indexId = command.indexId();
    return getIndexByIdPort.findIndexById(indexId)
        .switchIfEmpty(Mono.error(new IndexNotExistException("Index not found")))
        .flatMap(index -> deleteIndexColumnsPort.deleteByIndexId(indexId)
            .then(deleteIndexPort.deleteIndex(indexId))
            .thenReturn(MutationResult.<Void>of(null, index.tableId())))
        .as(transactionalOperator::transactional);
  }

}
