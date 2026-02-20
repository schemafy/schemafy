package com.schemafy.core.erd.controller;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.MutationResponse;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.erd.controller.dto.request.AddConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintCheckExprRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintDefaultExprRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintRequest;
import com.schemafy.core.erd.controller.dto.response.AddConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintCheckExprUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintDefaultExprUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ConstraintController {

  private final CreateConstraintUseCase createConstraintUseCase;
  private final GetConstraintUseCase getConstraintUseCase;
  private final GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;
  private final ChangeConstraintCheckExprUseCase changeConstraintCheckExprUseCase;
  private final ChangeConstraintDefaultExprUseCase changeConstraintDefaultExprUseCase;
  private final ChangeConstraintNameUseCase changeConstraintNameUseCase;
  private final DeleteConstraintUseCase deleteConstraintUseCase;
  private final GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;
  private final AddConstraintColumnUseCase addConstraintColumnUseCase;
  private final RemoveConstraintColumnUseCase removeConstraintColumnUseCase;
  private final GetConstraintColumnUseCase getConstraintColumnUseCase;
  private final ChangeConstraintColumnPositionUseCase changeConstraintColumnPositionUseCase;

  private final ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/constraints")
  public Mono<BaseResponse<MutationResponse<ConstraintResponse>>> createConstraint(
      @Valid @RequestBody CreateConstraintRequest request) {
    CreateConstraintCommand command = new CreateConstraintCommand(
        request.tableId(),
        request.name(),
        request.kind(),
        request.checkExpr(),
        request.defaultExpr(),
        mapConstraintColumns(request.columns()));
    return createConstraintUseCase.createConstraint(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.of(
            ConstraintResponse.from(result.result(), request.tableId()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/constraints/{constraintId}")
  public Mono<BaseResponse<ConstraintResponse>> getConstraint(
      @PathVariable String constraintId) {
    GetConstraintQuery query = new GetConstraintQuery(constraintId);
    return getConstraintUseCase.getConstraint(query)
        .map(ConstraintResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/constraints")
  public Mono<BaseResponse<List<ConstraintResponse>>> getConstraintsByTableId(
      @PathVariable String tableId) {
    GetConstraintsByTableIdQuery query = new GetConstraintsByTableIdQuery(tableId);
    return getConstraintsByTableIdUseCase.getConstraintsByTableId(query)
        .map(constraints -> constraints.stream()
            .map(ConstraintResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/constraints/{constraintId}/check-expr")
  public Mono<BaseResponse<MutationResponse<Void>>> changeConstraintCheckExpr(
      @PathVariable String constraintId,
      @Valid @RequestBody ChangeConstraintCheckExprRequest request) {
    if (!request.checkExpr().isPresent()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "checkExpr must be provided");
    }
    ChangeConstraintCheckExprCommand command = new ChangeConstraintCheckExprCommand(
        constraintId,
        request.checkExpr().orElse(null));
    return changeConstraintCheckExprUseCase.changeConstraintCheckExpr(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/constraints/{constraintId}/default-expr")
  public Mono<BaseResponse<MutationResponse<Void>>> changeConstraintDefaultExpr(
      @PathVariable String constraintId,
      @Valid @RequestBody ChangeConstraintDefaultExprRequest request) {
    if (!request.defaultExpr().isPresent()) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE, "defaultExpr must be provided");
    }
    ChangeConstraintDefaultExprCommand command = new ChangeConstraintDefaultExprCommand(
        constraintId,
        request.defaultExpr().orElse(null));
    return changeConstraintDefaultExprUseCase.changeConstraintDefaultExpr(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/constraints/{constraintId}/name")
  public Mono<BaseResponse<MutationResponse<Void>>> changeConstraintName(
      @PathVariable String constraintId,
      @Valid @RequestBody ChangeConstraintNameRequest request) {
    ChangeConstraintNameCommand command = new ChangeConstraintNameCommand(
        constraintId,
        request.newName());
    return changeConstraintNameUseCase.changeConstraintName(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/constraints/{constraintId}")
  public Mono<BaseResponse<MutationResponse<Void>>> deleteConstraint(
      @PathVariable String constraintId) {
    DeleteConstraintCommand command = new DeleteConstraintCommand(constraintId);
    return deleteConstraintUseCase.deleteConstraint(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/constraints/{constraintId}/columns")
  public Mono<BaseResponse<List<ConstraintColumnResponse>>> getConstraintColumns(
      @PathVariable String constraintId) {
    GetConstraintColumnsByConstraintIdQuery query = new GetConstraintColumnsByConstraintIdQuery(constraintId);
    return getConstraintColumnsByConstraintIdUseCase.getConstraintColumnsByConstraintId(query)
        .map(columns -> columns.stream()
            .map(ConstraintColumnResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/constraints/{constraintId}/columns")
  public Mono<BaseResponse<MutationResponse<AddConstraintColumnResponse>>> addConstraintColumn(
      @PathVariable String constraintId,
      @Valid @RequestBody AddConstraintColumnRequest request) {
    AddConstraintColumnCommand command = new AddConstraintColumnCommand(
        constraintId,
        request.columnId(),
        request.seqNo());
    return addConstraintColumnUseCase.addConstraintColumn(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.of(
            AddConstraintColumnResponse.from(result.result()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @DeleteMapping("/constraint-columns/{constraintColumnId}")
  public Mono<BaseResponse<MutationResponse<Void>>> removeConstraintColumn(
      @PathVariable String constraintColumnId) {
    RemoveConstraintColumnCommand command = new RemoveConstraintColumnCommand(
        constraintColumnId);
    return removeConstraintColumnUseCase.removeConstraintColumn(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/constraint-columns/{constraintColumnId}")
  public Mono<BaseResponse<ConstraintColumnResponse>> getConstraintColumn(
      @PathVariable String constraintColumnId) {
    GetConstraintColumnQuery query = new GetConstraintColumnQuery(constraintColumnId);
    return getConstraintColumnUseCase.getConstraintColumn(query)
        .map(ConstraintColumnResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/constraint-columns/{constraintColumnId}/position")
  public Mono<BaseResponse<MutationResponse<Void>>> changeConstraintColumnPosition(
      @PathVariable String constraintColumnId,
      @RequestBody ChangeConstraintColumnPositionRequest request) {
    ChangeConstraintColumnPositionCommand command = new ChangeConstraintColumnPositionCommand(
        constraintColumnId,
        request.seqNo());
    return changeConstraintColumnPositionUseCase.changeConstraintColumnPosition(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  private Mono<Void> broadcastMutation(Set<String> affectedTableIds) {
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return Mono.empty();
    }
    return broadcaster.broadcast(affectedTableIds);
  }

  private static List<CreateConstraintColumnCommand> mapConstraintColumns(
      List<CreateConstraintColumnRequest> columns) {
    if (columns == null || columns.isEmpty()) {
      return List.of();
    }
    return columns.stream()
        .map(column -> new CreateConstraintColumnCommand(
            column.columnId(),
            column.seqNo()))
        .toList();
  }

}
