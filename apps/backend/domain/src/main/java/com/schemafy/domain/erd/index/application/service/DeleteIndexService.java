package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteIndexService implements DeleteIndexUseCase {

  private final DeleteIndexPort deleteIndexPort;
  private final DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<Void> deleteIndex(DeleteIndexCommand command) {
    String indexId = command.indexId();
    return deleteIndexColumnsPort.deleteByIndexId(indexId)
        .then(deleteIndexPort.deleteIndex(indexId))
        .as(transactionalOperator::transactional);
  }

}
