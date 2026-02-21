package com.schemafy.core.erd.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.MutationResponse;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.erd.controller.dto.request.ChangeTableExtraRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeTableMetaRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeTableNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequest;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.IndexSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.common.util.PatchFieldConverter.toPatchField;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class TableController {

  private final CreateTableUseCase createTableUseCase;
  private final GetTableUseCase getTableUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;
  private final GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;
  private final GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;
  private final GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;
  private final GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;
  private final GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;
  private final GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;
  private final ChangeTableNameUseCase changeTableNameUseCase;
  private final ChangeTableMetaUseCase changeTableMetaUseCase;
  private final ChangeTableExtraUseCase changeTableExtraUseCase;
  private final DeleteTableUseCase deleteTableUseCase;

  private final ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PostMapping("/tables")
  public Mono<MutationResponse<TableResponse>> createTable(
      @Valid @RequestBody CreateTableRequest request) {
    CreateTableCommand command = new CreateTableCommand(
        request.schemaId(),
        request.name(),
        request.charset(),
        request.collation());
    return createTableUseCase.createTable(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.of(
            TableResponse.from(result.result(), request.schemaId()),
            result.affectedTableIds()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}")
  public Mono<TableResponse> getTable(
      @PathVariable String tableId) {
    GetTableQuery query = new GetTableQuery(tableId);
    return getTableUseCase.getTable(query)
        .map(TableResponse::from);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/{tableId}/snapshot")
  public Mono<TableSnapshotResponse> getTableSnapshot(
      @PathVariable String tableId) {
    return fetchTableSnapshot(tableId);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/tables/snapshots")
  public Mono<Map<String, TableSnapshotResponse>> getTableSnapshots(
      @RequestParam List<String> tableIds) {
    return Flux.fromIterable(tableIds)
        .flatMap(tableId -> fetchTableSnapshot(tableId)
            .onErrorResume(e -> Mono.empty()))
        .collectMap(snapshot -> snapshot.table().id(), Function.identity());
  }

  private Mono<TableSnapshotResponse> fetchTableSnapshot(String tableId) {
    Mono<TableResponse> tableMono = getTableUseCase
        .getTable(new GetTableQuery(tableId))
        .map(TableResponse::from);

    Mono<List<ColumnResponse>> columnsMono = getColumnsByTableIdUseCase
        .getColumnsByTableId(new GetColumnsByTableIdQuery(tableId))
        .defaultIfEmpty(List.of())
        .map(columns -> columns.stream()
            .sorted(Comparator.comparingInt(Column::seqNo))
            .map(ColumnResponse::from)
            .toList());

    Mono<List<ConstraintSnapshotResponse>> constraintsMono = getConstraintsByTableIdUseCase
        .getConstraintsByTableId(new GetConstraintsByTableIdQuery(tableId))
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> Flux.fromIterable(constraints)
            .flatMap(constraint -> getConstraintColumnsByConstraintIdUseCase
                .getConstraintColumnsByConstraintId(
                    new GetConstraintColumnsByConstraintIdQuery(constraint.id()))
                .defaultIfEmpty(List.of())
                .map(columns -> columns.stream()
                    .sorted(Comparator.comparingInt(ConstraintColumn::seqNo))
                    .map(ConstraintColumnResponse::from)
                    .toList())
                .map(columns -> new ConstraintSnapshotResponse(
                    ConstraintResponse.from(constraint),
                    columns)))
            .collectList());

    Mono<List<RelationshipSnapshotResponse>> relationshipsMono = getRelationshipsByTableIdUseCase
        .getRelationshipsByTableId(new GetRelationshipsByTableIdQuery(tableId))
        .defaultIfEmpty(List.of())
        .flatMap(relationships -> Flux.fromIterable(relationships)
            .flatMap(relationship -> getRelationshipColumnsByRelationshipIdUseCase
                .getRelationshipColumnsByRelationshipId(
                    new GetRelationshipColumnsByRelationshipIdQuery(relationship.id()))
                .defaultIfEmpty(List.of())
                .map(columns -> columns.stream()
                    .sorted(Comparator.comparingInt(RelationshipColumn::seqNo))
                    .map(RelationshipColumnResponse::from)
                    .toList())
                .map(columns -> new RelationshipSnapshotResponse(
                    RelationshipResponse.from(relationship),
                    columns)))
            .collectList());

    Mono<List<IndexSnapshotResponse>> indexesMono = getIndexesByTableIdUseCase
        .getIndexesByTableId(new GetIndexesByTableIdQuery(tableId))
        .defaultIfEmpty(List.of())
        .flatMap(indexes -> Flux.fromIterable(indexes)
            .flatMap(index -> getIndexColumnsByIndexIdUseCase
                .getIndexColumnsByIndexId(new GetIndexColumnsByIndexIdQuery(index.id()))
                .defaultIfEmpty(List.of())
                .map(columns -> columns.stream()
                    .sorted(Comparator.comparingInt(IndexColumn::seqNo))
                    .map(IndexColumnResponse::from)
                    .toList())
                .map(columns -> new IndexSnapshotResponse(
                    IndexResponse.from(index),
                    columns)))
            .collectList());

    return Mono.zip(tableMono, columnsMono, constraintsMono, relationshipsMono, indexesMono)
        .map(tuple -> new TableSnapshotResponse(
            tuple.getT1(),
            tuple.getT2(),
            tuple.getT3(),
            tuple.getT4(),
            tuple.getT5()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/tables")
  public Mono<List<TableResponse>> getTablesBySchemaId(
      @PathVariable String schemaId) {
    GetTablesBySchemaIdQuery query = new GetTablesBySchemaIdQuery(schemaId);
    return getTablesBySchemaIdUseCase.getTablesBySchemaId(query)
        .map(TableResponse::from)
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
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
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
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
  @PatchMapping("/tables/{tableId}/extra")
  public Mono<MutationResponse<Void>> changeTableExtra(
      @PathVariable String tableId,
      @RequestBody ChangeTableExtraRequest request) {
    ChangeTableExtraCommand command = new ChangeTableExtraCommand(
        tableId,
        request.extra());
    return changeTableExtraUseCase.changeTableExtra(command)
        .flatMap(result -> broadcastMutation(result.affectedTableIds())
            .thenReturn(result))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/tables/{tableId}")
  public Mono<MutationResponse<Void>> deleteTable(
      @PathVariable String tableId) {
    DeleteTableCommand command = new DeleteTableCommand(tableId);
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return deleteTableUseCase.deleteTable(command)
          .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
    }
    return broadcaster.resolveFromTableId(tableId)
        .flatMap(ctx -> deleteTableUseCase.deleteTable(command)
            .flatMap(result -> broadcaster
                .broadcastWithContext(ctx, result.affectedTableIds())
                .thenReturn(result)))
        .map(result -> MutationResponse.<Void>of(null, result.affectedTableIds()));
  }

  private Mono<Void> broadcastMutation(Set<String> affectedTableIds) {
    ErdMutationBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
    if (broadcaster == null) {
      return Mono.empty();
    }
    return broadcaster.broadcast(affectedTableIds);
  }

}
