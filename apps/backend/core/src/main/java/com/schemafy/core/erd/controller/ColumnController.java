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

import static com.schemafy.core.common.util.PatchFieldConverter.toPatchField;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.MutationResponse;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnMetaRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnNameRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnTypeRequest;
import com.schemafy.core.erd.controller.dto.request.CreateColumnRequest;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ColumnController {

  private final CreateColumnUseCase createColumnUseCase;
  private final GetColumnUseCase getColumnUseCase;
  private final GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;
  private final ChangeColumnNameUseCase changeColumnNameUseCase;
  private final ChangeColumnTypeUseCase changeColumnTypeUseCase;
  private final ChangeColumnMetaUseCase changeColumnMetaUseCase;
  private final ChangeColumnPositionUseCase changeColumnPositionUseCase;
  private final DeleteColumnUseCase deleteColumnUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/columns")
  public Mono<BaseResponse<MutationResponse<ColumnResponse>>> createColumn(
      @Valid @RequestBody CreateColumnRequest request) {
    CreateColumnCommand command = new CreateColumnCommand(
        request.tableId(),
        request.name(),
        request.dataType(),
        request.length(),
        request.precision(),
        request.scale(),
        request.autoIncrement(),
        request.charset(),
        request.collation(),
        request.comment());
    return createColumnUseCase.createColumn(command)
        .map(result -> MutationResponse.of(
            ColumnResponse.from(result.result(), request.tableId()),
            result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/columns/{columnId}")
  public Mono<BaseResponse<ColumnResponse>> getColumn(
      @PathVariable String columnId) {
    GetColumnQuery query = new GetColumnQuery(columnId);
    return getColumnUseCase.getColumn(query)
        .map(ColumnResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/columns")
  public Mono<BaseResponse<List<ColumnResponse>>> getColumnsByTableId(
      @PathVariable String tableId) {
    GetColumnsByTableIdQuery query = new GetColumnsByTableIdQuery(tableId);
    return getColumnsByTableIdUseCase.getColumnsByTableId(query)
        .map(columns -> columns.stream()
            .map(ColumnResponse::from)
            .toList())
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/columns/{columnId}/name")
  public Mono<BaseResponse<MutationResponse<Void>>> changeColumnName(
      @PathVariable String columnId,
      @Valid @RequestBody ChangeColumnNameRequest request) {
    ChangeColumnNameCommand command = new ChangeColumnNameCommand(
        columnId,
        request.newName());
    return changeColumnNameUseCase.changeColumnName(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/columns/{columnId}/type")
  public Mono<BaseResponse<MutationResponse<Void>>> changeColumnType(
      @PathVariable String columnId,
      @Valid @RequestBody ChangeColumnTypeRequest request) {
    ChangeColumnTypeCommand command = new ChangeColumnTypeCommand(
        columnId,
        request.dataType(),
        request.length(),
        request.precision(),
        request.scale());
    return changeColumnTypeUseCase.changeColumnType(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/columns/{columnId}/meta")
  public Mono<BaseResponse<MutationResponse<Void>>> changeColumnMeta(
      @PathVariable String columnId,
      @RequestBody ChangeColumnMetaRequest request) {
    ChangeColumnMetaCommand command = new ChangeColumnMetaCommand(
        columnId,
        toPatchField(request.autoIncrement()),
        toPatchField(request.charset()),
        toPatchField(request.collation()),
        toPatchField(request.comment()));
    return changeColumnMetaUseCase.changeColumnMeta(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/columns/{columnId}/position")
  public Mono<BaseResponse<MutationResponse<Void>>> changeColumnPosition(
      @PathVariable String columnId,
      @RequestBody ChangeColumnPositionRequest request) {
    ChangeColumnPositionCommand command = new ChangeColumnPositionCommand(
        columnId,
        request.seqNo());
    return changeColumnPositionUseCase.changeColumnPosition(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/columns/{columnId}")
  public Mono<BaseResponse<MutationResponse<Void>>> deleteColumn(
      @PathVariable String columnId) {
    DeleteColumnCommand command = new DeleteColumnCommand(columnId);
    return deleteColumnUseCase.deleteColumn(command)
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()))
        .map(BaseResponse::success);
  }

}
