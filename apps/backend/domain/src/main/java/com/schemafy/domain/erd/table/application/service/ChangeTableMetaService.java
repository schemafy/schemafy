package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableMetaPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeTableMetaService implements ChangeTableMetaUseCase {

  private final ChangeTableMetaPort changeTableMetaPort;

  @Override
  public Mono<Void> changeTableMeta(ChangeTableMetaCommand command) {
    return changeTableMetaPort.changeTableMeta(
        command.tableId(),
        command.charset(),
        command.collation());
  }

}
