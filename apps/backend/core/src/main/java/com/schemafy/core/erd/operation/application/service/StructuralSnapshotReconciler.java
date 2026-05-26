package com.schemafy.core.erd.operation.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.core.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.core.erd.column.application.port.out.RestoreColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.core.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.core.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.core.erd.constraint.application.port.out.RestoreConstraintColumnPort;
import com.schemafy.core.erd.constraint.application.port.out.RestoreConstraintPort;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.core.erd.index.application.port.out.CreateIndexColumnPort;
import com.schemafy.core.erd.index.application.port.out.CreateIndexPort;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexColumnPort;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.core.erd.index.application.port.out.RestoreIndexColumnPort;
import com.schemafy.core.erd.index.application.port.out.RestoreIndexPort;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ConstraintColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.IndexColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.IndexSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.RelationshipColumnSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot.TableSnapshot;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.RestoreRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.RestoreRelationshipPort;
import com.schemafy.core.erd.table.application.port.out.CreateTablePort;
import com.schemafy.core.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.core.erd.table.application.port.out.RestoreTablePort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class StructuralSnapshotReconciler {

  private final StructuralSnapshotReader reader;
  private final CreateTablePort createTablePort;
  private final DeleteTablePort deleteTablePort;
  private final RestoreTablePort restoreTablePort;
  private final CreateColumnPort createColumnPort;
  private final DeleteColumnPort deleteColumnPort;
  private final RestoreColumnPort restoreColumnPort;
  private final CreateConstraintPort createConstraintPort;
  private final CreateConstraintColumnPort createConstraintColumnPort;
  private final DeleteConstraintPort deleteConstraintPort;
  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final RestoreConstraintPort restoreConstraintPort;
  private final RestoreConstraintColumnPort restoreConstraintColumnPort;
  private final CreateIndexPort createIndexPort;
  private final CreateIndexColumnPort createIndexColumnPort;
  private final DeleteIndexPort deleteIndexPort;
  private final DeleteIndexColumnPort deleteIndexColumnPort;
  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final RestoreIndexPort restoreIndexPort;
  private final RestoreIndexColumnPort restoreIndexColumnPort;
  private final CreateRelationshipPort createRelationshipPort;
  private final CreateRelationshipColumnPort createRelationshipColumnPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final RestoreRelationshipPort restoreRelationshipPort;
  private final RestoreRelationshipColumnPort restoreRelationshipColumnPort;

  Mono<Void> reconcileTo(StructuralSnapshot target) {
    return reader.captureBySchemaId(target.schemaId())
        .flatMap(current -> deleteRowsNotInTarget(current, target)
            .then(createRowsMissingFromCurrent(current, target))
            .then(restoreRowsExistingInCurrent(current, target))
            .then(applyMembershipPositions(target)));
  }

  private Mono<Void> deleteRowsNotInTarget(StructuralSnapshot current, StructuralSnapshot target) {
    Set<String> targetRelationshipColumnIds = ids(target.relationshipColumns(), RelationshipColumnSnapshot::id);
    Set<String> targetConstraintColumnIds = ids(target.constraintColumns(), ConstraintColumnSnapshot::id);
    Set<String> targetIndexColumnIds = ids(target.indexColumns(), IndexColumnSnapshot::id);
    Set<String> targetRelationshipIds = ids(target.relationships(), RelationshipSnapshot::id);
    Set<String> targetConstraintIds = ids(target.constraints(), ConstraintSnapshot::id);
    Set<String> targetIndexIds = ids(target.indexes(), IndexSnapshot::id);
    Set<String> targetColumnIds = ids(target.columns(), ColumnSnapshot::id);
    Set<String> targetTableIds = ids(target.tables(), TableSnapshot::id);

    return Flux.fromIterable(current.relationshipColumns())
        .filter(column -> !targetRelationshipColumnIds.contains(column.id()))
        .concatMap(column -> deleteRelationshipColumnPort.deleteRelationshipColumn(column.id()))
        .thenMany(Flux.fromIterable(current.constraintColumns())
            .filter(column -> !targetConstraintColumnIds.contains(column.id()))
            .concatMap(column -> deleteConstraintColumnPort.deleteConstraintColumn(column.id())))
        .thenMany(Flux.fromIterable(current.indexColumns())
            .filter(column -> !targetIndexColumnIds.contains(column.id()))
            .concatMap(column -> deleteIndexColumnPort.deleteIndexColumn(column.id())))
        .thenMany(Flux.fromIterable(current.relationships())
            .filter(relationship -> !targetRelationshipIds.contains(relationship.id()))
            .concatMap(relationship -> deleteRelationshipPort.deleteRelationship(relationship.id())))
        .thenMany(Flux.fromIterable(current.constraints())
            .filter(constraint -> !targetConstraintIds.contains(constraint.id()))
            .concatMap(constraint -> deleteConstraintPort.deleteConstraint(constraint.id())))
        .thenMany(Flux.fromIterable(current.indexes())
            .filter(index -> !targetIndexIds.contains(index.id()))
            .concatMap(index -> deleteIndexPort.deleteIndex(index.id())))
        .thenMany(Flux.fromIterable(current.columns())
            .filter(column -> !targetColumnIds.contains(column.id()))
            .concatMap(column -> deleteColumnPort.deleteColumn(column.id())))
        .thenMany(Flux.fromIterable(current.tables())
            .filter(table -> !targetTableIds.contains(table.id()))
            .concatMap(table -> deleteTablePort.deleteTable(table.id())))
        .then();
  }

  private Mono<Void> restoreRowsExistingInCurrent(StructuralSnapshot current, StructuralSnapshot target) {
    Map<String, TableSnapshot> currentTablesById = byId(current.tables(), TableSnapshot::id);
    Map<String, ColumnSnapshot> currentColumnsById = byId(current.columns(), ColumnSnapshot::id);
    Map<String, ConstraintSnapshot> currentConstraintsById = byId(current.constraints(), ConstraintSnapshot::id);
    Map<String, IndexSnapshot> currentIndexesById = byId(current.indexes(), IndexSnapshot::id);
    Map<String, RelationshipSnapshot> currentRelationshipsById = byId(
        current.relationships(), RelationshipSnapshot::id);
    Map<String, ConstraintColumnSnapshot> currentConstraintColumnsById = byId(
        current.constraintColumns(), ConstraintColumnSnapshot::id);
    Map<String, IndexColumnSnapshot> currentIndexColumnsById = byId(
        current.indexColumns(), IndexColumnSnapshot::id);
    Map<String, RelationshipColumnSnapshot> currentRelationshipColumnsById = byId(
        current.relationshipColumns(), RelationshipColumnSnapshot::id);

    return restoreChangedRows(
        target.tables(),
        currentTablesById,
        TableSnapshot::id,
        table -> restoreTablePort.restoreTable(table.toDomain()))
        .then(restoreChangedRows(
            target.columns(),
            currentColumnsById,
            ColumnSnapshot::id,
            column -> restoreColumnPort.restoreColumn(column.toDomain())))
        .then(restoreChangedRows(
            target.constraints(),
            currentConstraintsById,
            ConstraintSnapshot::id,
            constraint -> restoreConstraintPort.restoreConstraint(constraint.toDomain())))
        .then(restoreChangedRows(
            target.indexes(),
            currentIndexesById,
            IndexSnapshot::id,
            index -> restoreIndexPort.restoreIndex(index.toDomain())))
        .then(restoreChangedRows(
            target.relationships(),
            currentRelationshipsById,
            RelationshipSnapshot::id,
            relationship -> restoreRelationshipPort.restoreRelationship(relationship.toDomain())))
        .then(restoreChangedRows(
            target.constraintColumns(),
            currentConstraintColumnsById,
            ConstraintColumnSnapshot::id,
            column -> restoreConstraintColumnPort.restoreConstraintColumn(column.toDomain())))
        .then(restoreChangedRows(
            target.indexColumns(),
            currentIndexColumnsById,
            IndexColumnSnapshot::id,
            column -> restoreIndexColumnPort.restoreIndexColumn(column.toDomain())))
        .then(restoreChangedRows(
            target.relationshipColumns(),
            currentRelationshipColumnsById,
            RelationshipColumnSnapshot::id,
            column -> restoreRelationshipColumnPort.restoreRelationshipColumn(column.toDomain())));
  }

  private Mono<Void> createRowsMissingFromCurrent(StructuralSnapshot current, StructuralSnapshot target) {
    Set<String> currentTableIds = ids(current.tables(), TableSnapshot::id);
    Set<String> currentColumnIds = ids(current.columns(), ColumnSnapshot::id);
    Set<String> currentConstraintIds = ids(current.constraints(), ConstraintSnapshot::id);
    Set<String> currentIndexIds = ids(current.indexes(), IndexSnapshot::id);
    Set<String> currentRelationshipIds = ids(current.relationships(), RelationshipSnapshot::id);
    Set<String> currentConstraintColumnIds = ids(current.constraintColumns(), ConstraintColumnSnapshot::id);
    Set<String> currentIndexColumnIds = ids(current.indexColumns(), IndexColumnSnapshot::id);
    Set<String> currentRelationshipColumnIds = ids(current.relationshipColumns(), RelationshipColumnSnapshot::id);

    return Flux.fromIterable(target.tables())
        .filter(table -> !currentTableIds.contains(table.id()))
        .concatMap(table -> createTablePort.createTable(table.toDomain()))
        .thenMany(Flux.fromIterable(target.columns())
            .filter(column -> !currentColumnIds.contains(column.id()))
            .concatMap(column -> createColumnPort.createColumn(column.toDomain())))
        .thenMany(Flux.fromIterable(target.constraints())
            .filter(constraint -> !currentConstraintIds.contains(constraint.id()))
            .concatMap(constraint -> createConstraintPort.createConstraint(constraint.toDomain())))
        .thenMany(Flux.fromIterable(target.indexes())
            .filter(index -> !currentIndexIds.contains(index.id()))
            .concatMap(index -> createIndexPort.createIndex(index.toDomain())))
        .thenMany(Flux.fromIterable(target.relationships())
            .filter(relationship -> !currentRelationshipIds.contains(relationship.id()))
            .concatMap(relationship -> createRelationshipPort.createRelationship(relationship.toDomain())))
        .thenMany(Flux.fromIterable(target.constraintColumns())
            .filter(column -> !currentConstraintColumnIds.contains(column.id()))
            .concatMap(column -> createConstraintColumnPort.createConstraintColumn(column.toDomain())))
        .thenMany(Flux.fromIterable(target.indexColumns())
            .filter(column -> !currentIndexColumnIds.contains(column.id()))
            .concatMap(column -> createIndexColumnPort.createIndexColumn(column.toDomain())))
        .thenMany(Flux.fromIterable(target.relationshipColumns())
            .filter(column -> !currentRelationshipColumnIds.contains(column.id()))
            .concatMap(column -> createRelationshipColumnPort.createRelationshipColumn(column.toDomain())))
        .then();
  }

  private Mono<Void> applyMembershipPositions(StructuralSnapshot target) {
    return applyConstraintColumnPositions(target)
        .then(applyIndexColumnPositions(target))
        .then(applyRelationshipColumnPositions(target));
  }

  private Mono<Void> applyConstraintColumnPositions(StructuralSnapshot target) {
    Map<String, List<ConstraintColumnSnapshot>> byConstraintId = target.constraintColumns().stream()
        .collect(Collectors.groupingBy(ConstraintColumnSnapshot::constraintId));
    return Flux.fromIterable(byConstraintId.entrySet())
        .concatMap(entry -> changeConstraintColumnPositionPort.changeConstraintColumnPositions(
            entry.getKey(),
            sortBy(entry.getValue(), ConstraintColumnSnapshot::seqNo).stream()
                .map(ConstraintColumnSnapshot::toDomain)
                .toList()))
        .then();
  }

  private Mono<Void> applyIndexColumnPositions(StructuralSnapshot target) {
    Map<String, List<IndexColumnSnapshot>> byIndexId = target.indexColumns().stream()
        .collect(Collectors.groupingBy(IndexColumnSnapshot::indexId));
    return Flux.fromIterable(byIndexId.entrySet())
        .concatMap(entry -> changeIndexColumnPositionPort.changeIndexColumnPositions(
            entry.getKey(),
            sortBy(entry.getValue(), IndexColumnSnapshot::seqNo).stream()
                .map(IndexColumnSnapshot::toDomain)
                .toList()))
        .then();
  }

  private Mono<Void> applyRelationshipColumnPositions(StructuralSnapshot target) {
    Map<String, List<RelationshipColumnSnapshot>> byRelationshipId = target.relationshipColumns().stream()
        .collect(Collectors.groupingBy(RelationshipColumnSnapshot::relationshipId));
    return Flux.fromIterable(byRelationshipId.entrySet())
        .concatMap(entry -> changeRelationshipColumnPositionPort.changeRelationshipColumnPositions(
            entry.getKey(),
            sortBy(entry.getValue(), RelationshipColumnSnapshot::seqNo).stream()
                .map(RelationshipColumnSnapshot::toDomain)
                .toList()))
        .then();
  }

  private static <T> Set<String> ids(List<T> values, Function<T, String> idExtractor) {
    return values.stream()
        .map(idExtractor)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static <T> Map<String, T> byId(List<T> values, Function<T, String> idExtractor) {
    return values.stream()
        .collect(Collectors.toUnmodifiableMap(idExtractor, Function.identity()));
  }

  private static <T> Mono<Void> restoreChangedRows(
      List<T> targetRows,
      Map<String, T> currentRowsById,
      Function<T, String> idExtractor,
      Function<T, Mono<Void>> restore) {
    return Flux.fromIterable(targetRows)
        .filter(targetRow -> {
          T currentRow = currentRowsById.get(idExtractor.apply(targetRow));
          return currentRow != null && !currentRow.equals(targetRow);
        })
        .concatMap(restore)
        .then();
  }

  private static <T, U extends Comparable<? super U>> List<T> sortBy(List<T> values, Function<T, U> keyExtractor) {
    return values.stream()
        .sorted(Comparator.comparing(keyExtractor, Comparator.nullsFirst(Comparator.naturalOrder())))
        .toList();
  }

}
