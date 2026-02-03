package com.schemafy.core.erd.controller;

import java.util.List;

import jakarta.validation.Valid;

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
import com.schemafy.core.erd.controller.dto.request.AddConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintRequest;
import com.schemafy.core.erd.controller.dto.response.AddConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
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

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ConstraintController {

  private final CreateConstraintUseCase createConstraintUseCase;
  private final GetConstraintUseCase getConstraintUseCase;
  private final GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;
  private final ChangeConstraintNameUseCase changeConstraintNameUseCase;
  private final DeleteConstraintUseCase deleteConstraintUseCase;
  private final GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;
  private final AddConstraintColumnUseCase addConstraintColumnUseCase;
  private final RemoveConstraintColumnUseCase removeConstraintColumnUseCase;
  private final GetConstraintColumnUseCase getConstraintColumnUseCase;
  private final ChangeConstraintColumnPositionUseCase changeConstraintColumnPositionUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/constraints")
  public Mono<BaseResponse<ConstraintResponse>> createConstraint(
      @Valid @RequestBody CreateConstraintRequest request) {
    CreateConstraintCommand command = new CreateConstraintCommand(
        request.tableId(),
        request.name(),
        request.kind(),
        request.checkExpr(),
        request.defaultExpr(),
        mapConstraintColumns(request.columns()));
    return createConstraintUseCase.createConstraint(command)
        .map(result -> ConstraintResponse.from(result, request.tableId()))
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
  @PatchMapping("/constraints/{constraintId}/name")
  public Mono<BaseResponse<Void>> changeConstraintName(
      @PathVariable String constraintId,
      @Valid @RequestBody ChangeConstraintNameRequest request) {
    ChangeConstraintNameCommand command = new ChangeConstraintNameCommand(
        constraintId,
        request.newName());
    return changeConstraintNameUseCase.changeConstraintName(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/constraints/{constraintId}")
  public Mono<BaseResponse<Void>> deleteConstraint(
      @PathVariable String constraintId) {
    DeleteConstraintCommand command = new DeleteConstraintCommand(constraintId);
    return deleteConstraintUseCase.deleteConstraint(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/constraints/{constraintId}/columns")
  public Mono<BaseResponse<List<ConstraintColumnResponse>>> getConstraintColumns(
      @PathVariable String constraintId) {
    GetConstraintColumnsByConstraintIdQuery query =
        new GetConstraintColumnsByConstraintIdQuery(constraintId);
    return getConstraintColumnsByConstraintIdUseCase.getConstraintColumnsByConstraintId(query)
        .map(columns -> columns.stream()
            .map(ConstraintColumnResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/constraints/{constraintId}/columns")
  public Mono<BaseResponse<AddConstraintColumnResponse>> addConstraintColumn(
      @PathVariable String constraintId,
      @Valid @RequestBody AddConstraintColumnRequest request) {
    AddConstraintColumnCommand command = new AddConstraintColumnCommand(
        constraintId,
        request.columnId(),
        request.seqNo());
    return addConstraintColumnUseCase.addConstraintColumn(command)
        .map(AddConstraintColumnResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @DeleteMapping("/constraints/{constraintId}/columns/{constraintColumnId}")
  public Mono<BaseResponse<Void>> removeConstraintColumn(
      @PathVariable String constraintId,
      @PathVariable String constraintColumnId) {
    RemoveConstraintColumnCommand command = new RemoveConstraintColumnCommand(
        constraintId,
        constraintColumnId);
    return removeConstraintColumnUseCase.removeConstraintColumn(command)
        .then(Mono.just(BaseResponse.success(null)));
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
  public Mono<BaseResponse<Void>> changeConstraintColumnPosition(
      @PathVariable String constraintColumnId,
      @RequestBody ChangeConstraintColumnPositionRequest request) {
    ChangeConstraintColumnPositionCommand command = new ChangeConstraintColumnPositionCommand(
        constraintColumnId,
        request.seqNo());
    return changeConstraintColumnPositionUseCase.changeConstraintColumnPosition(command)
        .then(Mono.just(BaseResponse.success(null)));
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
