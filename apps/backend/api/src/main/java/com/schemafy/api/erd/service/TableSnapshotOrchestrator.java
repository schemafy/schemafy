package com.schemafy.api.erd.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.api.erd.controller.dto.response.IndexResponse;
import com.schemafy.api.erd.controller.dto.response.IndexSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TableSnapshotOrchestrator {

  private final GetTableUseCase getTableUseCase;
  private final GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;
  private final GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;
  private final GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;
  private final GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;
  private final GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;
  private final GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;
  private final GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  public Mono<TableSnapshotResponse> getTableSnapshot(String tableId) {
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

  public Mono<Map<String, TableSnapshotResponse>> getTableSnapshots(
      List<String> tableIds) {
    return Flux.fromIterable(tableIds)
        .flatMap(tableId -> getTableSnapshot(tableId)
            .onErrorResume(e -> Mono.empty()))
        .collectMap(snapshot -> snapshot.table().id(), Function.identity());
  }

}
