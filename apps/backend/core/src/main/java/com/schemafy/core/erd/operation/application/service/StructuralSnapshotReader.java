package com.schemafy.core.erd.operation.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ConstraintColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.IndexColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.IndexSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.RelationshipColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.TableSnapshot;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class StructuralSnapshotReader {

  private final GetTableByIdPort getTableByIdPort;
  private final GetTablesBySchemaIdPort getTablesBySchemaIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  Mono<StructuralSnapshot> captureByConstraintId(String constraintId) {
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            "Constraint not found: " + constraintId)))
        .flatMap(constraint -> captureByTableId(constraint.tableId()));
  }

  Mono<StructuralSnapshot> captureByColumnId(String columnId) {
    return getColumnByIdPort.findColumnById(columnId)
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + columnId)))
        .flatMap(column -> captureByTableId(column.tableId()));
  }

  Mono<StructuralSnapshot> captureByConstraintColumnId(String constraintColumnId) {
    return getConstraintColumnByIdPort.findConstraintColumnById(constraintColumnId)
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.COLUMN_NOT_FOUND,
            "Constraint column not found: " + constraintColumnId)))
        .flatMap(column -> captureByConstraintId(column.constraintId()));
  }

  Mono<StructuralSnapshot> captureByIndexId(String indexId) {
    return getIndexByIdPort.findIndexById(indexId)
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found: " + indexId)))
        .flatMap(index -> captureByTableId(index.tableId()));
  }

  Mono<StructuralSnapshot> captureByIndexColumnId(String indexColumnId) {
    return getIndexColumnByIdPort.findIndexColumnById(indexColumnId)
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.COLUMN_NOT_FOUND,
            "Index column not found: " + indexColumnId)))
        .flatMap(column -> captureByIndexId(column.indexId()));
  }

  Mono<StructuralSnapshot> captureByRelationshipId(String relationshipId) {
    return getRelationshipByIdPort.findRelationshipById(relationshipId)
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            "Relationship not found: " + relationshipId)))
        .flatMap(relationship -> captureByTableId(relationship.fkTableId()));
  }

  Mono<StructuralSnapshot> captureByRelationshipColumnId(String relationshipColumnId) {
    return getRelationshipColumnByIdPort.findRelationshipColumnById(relationshipColumnId)
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found: " + relationshipColumnId)))
        .flatMap(column -> captureByRelationshipId(column.relationshipId()));
  }

  Mono<StructuralSnapshot> captureByTableId(String tableId) {
    return getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + tableId)))
        .flatMap(table -> captureBySchemaId(table.schemaId()));
  }

  Mono<StructuralSnapshot> captureBySchemaId(String schemaId) {
    Mono<List<Table>> tablesMono = getTablesBySchemaIdPort.findTablesBySchemaId(schemaId)
        .collectList()
        .cache();

    return tablesMono.flatMap(tables -> {
      Mono<List<TableSnapshot>> tableSnapshotsMono = Mono.just(sortBy(tables, Table::id).stream()
          .map(TableSnapshot::from)
          .toList());
      List<String> tableIds = tables.stream()
          .map(Table::id)
          .sorted()
          .toList();

      Mono<List<ColumnSnapshot>> columnsMono = Flux.fromIterable(tableIds)
          .concatMap(tableId -> getColumnsByTableIdPort.findColumnsByTableId(tableId)
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(ColumnSnapshot::from)
          .collectList()
          .map(columns -> sortBy(columns, ColumnSnapshot::id));

      Mono<List<ConstraintSnapshot>> constraintsMono = Flux.fromIterable(tableIds)
          .concatMap(tableId -> getConstraintsByTableIdPort.findConstraintsByTableId(tableId)
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(ConstraintSnapshot::from)
          .collectList()
          .map(constraints -> sortBy(constraints, ConstraintSnapshot::id))
          .cache();

      Mono<List<ConstraintColumnSnapshot>> constraintColumnsMono = constraintsMono
          .flatMapMany(Flux::fromIterable)
          .concatMap(constraint -> getConstraintColumnsByConstraintIdPort
              .findConstraintColumnsByConstraintId(constraint.id())
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(ConstraintColumnSnapshot::from)
          .collectList()
          .map(columns -> sortBy(columns, ConstraintColumnSnapshot::id));

      Mono<List<IndexSnapshot>> indexesMono = Flux.fromIterable(tableIds)
          .concatMap(tableId -> getIndexesByTableIdPort.findIndexesByTableId(tableId)
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(IndexSnapshot::from)
          .collectList()
          .map(indexes -> sortBy(indexes, IndexSnapshot::id))
          .cache();

      Mono<List<IndexColumnSnapshot>> indexColumnsMono = indexesMono
          .flatMapMany(Flux::fromIterable)
          .concatMap(index -> getIndexColumnsByIndexIdPort
              .findIndexColumnsByIndexId(index.id())
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(IndexColumnSnapshot::from)
          .collectList()
          .map(columns -> sortBy(columns, IndexColumnSnapshot::id));

      Mono<List<RelationshipSnapshot>> relationshipsMono = getRelationshipsBySchemaIdPort
          .findRelationshipsBySchemaId(schemaId)
          .defaultIfEmpty(List.of())
          .flatMapMany(Flux::fromIterable)
          .map(RelationshipSnapshot::from)
          .collectList()
          .map(relationships -> sortBy(relationships, RelationshipSnapshot::id))
          .cache();

      Mono<List<RelationshipColumnSnapshot>> relationshipColumnsMono = relationshipsMono
          .flatMapMany(Flux::fromIterable)
          .concatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
              .findRelationshipColumnsByRelationshipId(relationship.id())
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable))
          .map(RelationshipColumnSnapshot::from)
          .collectList()
          .map(columns -> sortBy(columns, RelationshipColumnSnapshot::id));

      return Mono.zip(
          tableSnapshotsMono,
          columnsMono,
          constraintsMono,
          constraintColumnsMono,
          indexesMono,
          indexColumnsMono,
          relationshipsMono,
          relationshipColumnsMono)
          .map(tuple -> new StructuralSnapshot(
              schemaId,
              tuple.getT1(),
              tuple.getT2(),
              tuple.getT3(),
              tuple.getT4(),
              tuple.getT5(),
              tuple.getT6(),
              tuple.getT7(),
              tuple.getT8()));
    });
  }

  private static <T, U extends Comparable<? super U>> List<T> sortBy(List<T> values, Function<T, U> keyExtractor) {
    return values.stream()
        .sorted(Comparator.comparing(keyExtractor, Comparator.nullsFirst(Comparator.naturalOrder())))
        .toList();
  }

}
