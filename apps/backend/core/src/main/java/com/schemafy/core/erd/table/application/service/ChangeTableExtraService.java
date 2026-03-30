package com.schemafy.core.erd.table.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.out.ChangeTableExtraPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeTableExtraService implements ChangeTableExtraUseCase {

  private final ChangeTableExtraPort changeTableExtraPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeTableExtra(ChangeTableExtraCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_TABLE_EXTRA, command,
        () -> changeTableExtraPort
            .changeTableExtra(command.tableId(), command.extra())
            .thenReturn(MutationResult.<Void>of(null, command.tableId())));
  }

}
