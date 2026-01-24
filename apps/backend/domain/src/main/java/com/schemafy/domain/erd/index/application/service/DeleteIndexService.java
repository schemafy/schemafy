package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteIndexService implements DeleteIndexUseCase {

  private final DeleteIndexPort deleteIndexPort;
  private final DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;

  public DeleteIndexService(
      DeleteIndexPort deleteIndexPort,
      DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort) {
    this.deleteIndexPort = deleteIndexPort;
    this.deleteIndexColumnsPort = deleteIndexColumnsPort;
  }

  @Override
  public Mono<Void> deleteIndex(DeleteIndexCommand command) {
    String indexId = command.indexId();
    return deleteIndexColumnsPort.deleteByIndexId(indexId)
        .then(deleteIndexPort.deleteIndex(indexId));
  }

}
