package com.schemafy.api.erd.controller;

import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.MutationResponse;
import com.schemafy.api.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.api.erd.controller.dto.request.ChangeTableExtraRequest;
import com.schemafy.api.erd.controller.dto.request.ChangeTableMetaRequest;
import com.schemafy.api.erd.controller.dto.request.ChangeTableNameRequest;
import com.schemafy.api.erd.controller.dto.request.CreateTableRequest;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.service.TableSnapshotOrchestrator;
import com.schemafy.api.erd.service.table.TableApiResponseMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.api.common.util.PatchFieldConverter.toPatchField;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class TableController {

  private final CreateTableUseCase createTableUseCase;
  private final GetTableUseCase getTableUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final ChangeTableNameUseCase changeTableNameUseCase;
  private final ChangeTableMetaUseCase changeTableMetaUseCase;
  private final ChangeTableExtraUseCase changeTableExtraUseCase;
  private final DeleteTableUseCase deleteTableUseCase;
  private final JsonCodec jsonCodec;
  private final TableApiResponseMapper tableResponseMapper;

  private final ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/tables")
  public Mono<MutationResponse<TableResponse>> createTable(
      @Valid @RequestBody CreateTableRequest request) {
    CreateTableCommand command = new CreateTableCommand(
        request.schemaId(),
        request.name(),
        request.charset(),
        request.collation(),
        jsonCodec.canonicalizeOptional(request.extra()));
    return createTableUseCase.createTable(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds(),
            result.operation())
            .thenReturn(result))
        .map(result -> MutationResponse.of(
            tableResponseMapper.toTableResponse(result.result(),
                request.schemaId()),
            result.affectedTableIds(),
            result.operation()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}")
  public Mono<TableResponse> getTable(
      @PathVariable String tableId) {
    GetTableQuery query = new GetTableQuery(tableId);
    return getTableUseCase.getTable(query)
        .map(tableResponseMapper::toTableResponse);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/snapshot")
  public Mono<TableSnapshotResponse> getTableSnapshot(
      @PathVariable String tableId) {
    return tableSnapshotOrchestrator.getTableSnapshot(tableId);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/snapshots")
  public Mono<Map<String, TableSnapshotResponse>> getTableSnapshots(
      @RequestParam List<String> tableIds) {
    return tableSnapshotOrchestrator.getTableSnapshots(tableIds);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/tables")
  public Mono<List<TableResponse>> getTablesBySchemaId(
      @PathVariable String schemaId) {
    GetTablesBySchemaIdQuery query = new GetTablesBySchemaIdQuery(schemaId);
    return getTablesBySchemaIdUseCase.getTablesBySchemaId(query)
        .map(tableResponseMapper::toTableResponse)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/name")
  public Mono<MutationResponse<Void>> changeTableName(
      @PathVariable String tableId,
      @Valid @RequestBody ChangeTableNameRequest request) {
    ChangeTableNameCommand command = new ChangeTableNameCommand(
        tableId,
        request.newName());
    return changeTableNameUseCase.changeTableName(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds(),
            result.operation())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/meta")
  public Mono<MutationResponse<Void>> changeTableMeta(
      @PathVariable String tableId,
      @RequestBody ChangeTableMetaRequest request) {
    ChangeTableMetaCommand command = new ChangeTableMetaCommand(
        tableId,
        toPatchField(request.charset()),
        toPatchField(request.collation()));
    return changeTableMetaUseCase.changeTableMeta(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds(),
            result.operation())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/extra")
  public Mono<MutationResponse<Void>> changeTableExtra(
      @PathVariable String tableId,
      @Valid @RequestBody ChangeTableExtraRequest request) {
    ChangeTableExtraCommand command = new ChangeTableExtraCommand(
        tableId,
        jsonCodec.canonicalizeOptional(request.extra()));
    return changeTableExtraUseCase.changeTableExtra(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds(),
            result.operation())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/tables/{tableId}")
  public Mono<MutationResponse<Void>> deleteTable(
      @PathVariable String tableId) {
    DeleteTableCommand command = new DeleteTableCommand(tableId);
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return deleteTableUseCase.deleteTable(command)
          .map(result -> MutationResponse.<Void>of(null,
              result.affectedTableIds(), result.operation()));
    }
    return broadcaster.resolveFromTableId(tableId)
        .flatMap(ctx -> deleteTableUseCase.deleteTable(command)
            .flatMap(result -> broadcaster
                .broadcastWithContext(ctx, result.affectedTableIds(),
                    result.operation())
                .thenReturn(result)))
        .map(result -> MutationResponse.<Void>of(null,
            result.affectedTableIds(), result.operation()));
  }

  private Mono<Void> broadcastMutation(Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return Mono.empty();
    }
    return broadcaster.broadcast(affectedTableIds, operation);
  }

}
