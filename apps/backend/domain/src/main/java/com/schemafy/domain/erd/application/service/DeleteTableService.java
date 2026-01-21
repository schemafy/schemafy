package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.application.port.out.DeleteTablePort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteTableService implements DeleteTableUseCase {

  private final DeleteTablePort deleteTablePort;

  @Override
  public Mono<Void> deleteTable(DeleteTableCommand command) {
    return deleteTablePort.deleteTable(command.tableId());
  }

}
