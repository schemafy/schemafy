package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteIndexService implements DeleteIndexUseCase {

  private final DeleteIndexPort deleteIndexPort;

  public DeleteIndexService(DeleteIndexPort deleteIndexPort) {
    this.deleteIndexPort = deleteIndexPort;
  }

  @Override
  public Mono<Void> deleteIndex(DeleteIndexCommand command) {
    return deleteIndexPort.deleteIndex(command.indexId());
  }

}
