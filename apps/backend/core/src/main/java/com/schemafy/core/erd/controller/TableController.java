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
import com.schemafy.core.erd.controller.dto.request.ChangeTableExtraRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeTableMetaRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeTableNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequest;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTableQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class TableController {

  private final CreateTableUseCase createTableUseCase;
  private final GetTableUseCase getTableUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final ChangeTableNameUseCase changeTableNameUseCase;
  private final ChangeTableMetaUseCase changeTableMetaUseCase;
  private final ChangeTableExtraUseCase changeTableExtraUseCase;
  private final DeleteTableUseCase deleteTableUseCase;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/tables")
  public Mono<BaseResponse<TableResponse>> createTable(
      @Valid @RequestBody CreateTableRequest request) {
    CreateTableCommand command = new CreateTableCommand(
        request.schemaId(),
        request.name(),
        request.charset(),
        request.collation());
    return createTableUseCase.createTable(command)
        .map(result -> TableResponse.from(result, request.schemaId()))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}")
  public Mono<BaseResponse<TableResponse>> getTable(
      @PathVariable String tableId) {
    GetTableQuery query = new GetTableQuery(tableId);
    return getTableUseCase.getTable(query)
        .map(TableResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/tables")
  public Mono<BaseResponse<List<TableResponse>>> getTablesBySchemaId(
      @PathVariable String schemaId) {
    GetTablesBySchemaIdQuery query = new GetTablesBySchemaIdQuery(schemaId);
    return getTablesBySchemaIdUseCase.getTablesBySchemaId(query)
        .map(TableResponse::from)
        .collectList()
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/name")
  public Mono<BaseResponse<Void>> changeTableName(
      @PathVariable String tableId,
      @Valid @RequestBody ChangeTableNameRequest request) {
    ChangeTableNameCommand command = new ChangeTableNameCommand(
        request.schemaId(),
        tableId,
        request.newName());
    return changeTableNameUseCase.changeTableName(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/meta")
  public Mono<BaseResponse<Void>> changeTableMeta(
      @PathVariable String tableId,
      @RequestBody ChangeTableMetaRequest request) {
    ChangeTableMetaCommand command = new ChangeTableMetaCommand(
        tableId,
        request.charset(),
        request.collation());
    return changeTableMetaUseCase.changeTableMeta(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/extra")
  public Mono<BaseResponse<Void>> changeTableExtra(
      @PathVariable String tableId,
      @RequestBody ChangeTableExtraRequest request) {
    ChangeTableExtraCommand command = new ChangeTableExtraCommand(
        tableId,
        request.extra());
    return changeTableExtraUseCase.changeTableExtra(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/tables/{tableId}")
  public Mono<BaseResponse<Void>> deleteTable(
      @PathVariable String tableId) {
    DeleteTableCommand command = new DeleteTableCommand(tableId);
    return deleteTableUseCase.deleteTable(command)
        .then(Mono.just(BaseResponse.success(null)));
  }

}
