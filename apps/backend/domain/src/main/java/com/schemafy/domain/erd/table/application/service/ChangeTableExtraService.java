package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableExtraPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeTableExtraService implements ChangeTableExtraUseCase {

  private final ChangeTableExtraPort changeTableExtraPort;

  @Override
  public Mono<MutationResult<Void>> changeTableExtra(ChangeTableExtraCommand command) {
    return changeTableExtraPort
        .changeTableExtra(command.tableId(), command.extra())
        .thenReturn(MutationResult.<Void>of(null, command.tableId()));
  }

}
