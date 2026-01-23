package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.application.port.out.DeleteColumnPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteColumnService implements DeleteColumnUseCase {

  private final DeleteColumnPort deleteColumnPort;

  public DeleteColumnService(DeleteColumnPort deleteColumnPort) {
    this.deleteColumnPort = deleteColumnPort;
  }

  @Override
  public Mono<Void> deleteColumn(DeleteColumnCommand command) {
    return deleteColumnPort.deleteColumn(command.columnId());
  }
}
