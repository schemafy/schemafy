package com.schemafy.api.erd.controller;

import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.MutationResponse;
import com.schemafy.api.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class OperationController {

  private final UndoErdOperationUseCase undoErdOperationUseCase;
  private final RedoErdOperationUseCase redoErdOperationUseCase;
  private final ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/operations/{opId}/undo")
  public Mono<MutationResponse<Void>> undo(@PathVariable String opId) {
    return undoErdOperationUseCase.undo(new UndoErdOperationCommand(opId))
        .flatMap(this::broadcastMutation)
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/operations/{opId}/redo")
  public Mono<MutationResponse<Void>> redo(@PathVariable String opId) {
    return redoErdOperationUseCase.redo(new RedoErdOperationCommand(opId))
        .flatMap(this::broadcastMutation)
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  private Mono<MutationResult<Void>> broadcastMutation(
      MutationResult<Void> result) {
    return broadcast(result.affectedTableIds(), result.operation())
        .thenReturn(result);
  }

  private Mono<Void> broadcast(Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return Mono.empty();
    }
    return broadcaster.broadcast(affectedTableIds, operation);
  }

}
