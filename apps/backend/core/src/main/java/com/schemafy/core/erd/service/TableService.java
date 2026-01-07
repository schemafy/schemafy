package com.schemafy.core.erd.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class TableService {

  private final ValidationClient validationClient;
  private final TableRepository tableRepository;
  private final ColumnService columnService;
  private final ConstraintService constraintService;
  private final IndexService indexService;
  private final RelationshipService relationshipService;
  private final AffectedEntitiesSoftDeleter affectedEntitiesSoftDeleter;
  private final TransactionalOperator transactionalOperator;

  public Mono<AffectedMappingResponse> createTable(
      CreateTableRequestWithExtra request) {
    return validationClient.createTable(request.request())
        .flatMap(database -> tableRepository.save(ErdMapper.toEntity(
            request.request().getTable(), request.extra()))
            .map(savedTable -> AffectedMappingResponse.of(
                request.request(),
                request.request().getDatabase(),
                AffectedMappingResponse
                    .updateEntityIdInDatabase(
                        database,
                        EntityType.TABLE,
                        request.request().getTable()
                            .getId(),
                        savedTable.getId()))));
  }

  public Mono<TableDetailResponse> getTable(String id) {
    return tableRepository.findByIdAndDeletedAtIsNull(id)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
        .flatMap(table -> {
          Mono<List<ColumnResponse>> columnsMono = columnService
              .getColumnsByTableId(id)
              .collectList();
          Mono<List<ConstraintResponse>> constraintsMono = constraintService
              .getConstraintsByTableId(id)
              .collectList();
          Mono<List<IndexResponse>> indexesMono = indexService
              .getIndexesByTableId(id)
              .collectList();
          Mono<List<RelationshipResponse>> relationshipsMono = relationshipService
              .getRelationshipsByTableId(id)
              .collectList();

          return Mono.zip(columnsMono, constraintsMono, indexesMono,
              relationshipsMono)
              .map(tuple -> TableDetailResponse.from(table,
                  tuple.getT1(), tuple.getT2(),
                  tuple.getT3(), tuple.getT4()));
        });
  }

  public Flux<TableResponse> getTablesBySchemaId(String schemaId) {
    return tableRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
        .map(TableResponse::from);
  }

  public Mono<TableResponse> updateTableName(
      Validation.ChangeTableNameRequest request) {
    return tableRepository
        .findByIdAndDeletedAtIsNull(request.getTableId())
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
        .delayUntil(ignore -> validationClient.changeTableName(request))
        .doOnNext(table -> table.setName(request.getNewName()))
        .flatMap(tableRepository::save)
        .map(TableResponse::from);
  }

  public Mono<TableResponse> updateTableExtra(String tableId, String extra) {
    return tableRepository.findByIdAndDeletedAtIsNull(tableId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
        .doOnNext(table -> table.setExtra(extra))
        .flatMap(tableRepository::save)
        .map(TableResponse::from);
  }

  public Mono<Void> deleteTable(Validation.DeleteTableRequest request) {
    if (!request.hasDatabase()) {
      return Mono.error(
          new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
    }

    return tableRepository
        .findByIdAndDeletedAtIsNull(request.getTableId())
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
        .flatMap(table -> validationClient.deleteTable(request)
            .flatMap(afterDatabase -> transactionalOperator
                .transactional(Mono.just(table)
                    .doOnNext(entity -> entity.delete())
                    .flatMap(tableRepository::save)
                    .then(syncTableDeletion(
                        request.getDatabase(),
                        afterDatabase,
                        request.getTableId())))))
        .then();
  }

  private Mono<Void> syncTableDeletion(
      Validation.Database before,
      Validation.Database after,
      String deletedTableId) {
    ValidationDatabaseEntityIds beforeIds = ValidationDatabaseEntityIds
        .from(before);
    ValidationDatabaseEntityIds afterIds = ValidationDatabaseEntityIds
        .from(after);

    Set<String> removedRelationshipColumns = difference(
        beforeIds.relationshipColumns(),
        afterIds.relationshipColumns());
    Set<String> removedConstraintColumns = difference(
        beforeIds.constraintColumns(),
        afterIds.constraintColumns());
    Set<String> removedIndexColumns = difference(
        beforeIds.indexColumns(),
        afterIds.indexColumns());
    Set<String> removedRelationships = difference(beforeIds.relationships(),
        afterIds.relationships());
    Set<String> removedConstraints = difference(beforeIds.constraints(),
        afterIds.constraints());
    Set<String> removedIndexes = difference(beforeIds.indexes(),
        afterIds.indexes());
    Set<String> removedColumns = difference(beforeIds.columns(),
        afterIds.columns());
    Set<String> removedTables = difference(beforeIds.tables(),
        afterIds.tables());
    Set<String> removedSchemas = difference(beforeIds.schemas(),
        afterIds.schemas());

    TableOwnedEntityIds tableOwnedEntityIds = collectTableOwnedEntityIds(
        before,
        deletedTableId);
    removedColumns.removeAll(tableOwnedEntityIds.columns());
    removedIndexes.removeAll(tableOwnedEntityIds.indexes());
    removedIndexColumns.removeAll(tableOwnedEntityIds.indexColumns());
    removedConstraints.removeAll(tableOwnedEntityIds.constraints());
    removedConstraintColumns.removeAll(
        tableOwnedEntityIds.constraintColumns());

    ValidationDatabaseEntityIds idsToDelete = new ValidationDatabaseEntityIds(
        removedSchemas,
        removedTables,
        removedColumns,
        removedIndexes,
        removedIndexColumns,
        removedConstraints,
        removedConstraintColumns,
        removedRelationships,
        removedRelationshipColumns);
    return affectedEntitiesSoftDeleter.softDeleteEntities(idsToDelete);
  }

  private static Set<String> difference(Set<String> left, Set<String> right) {
    Set<String> result = new HashSet<>(left);
    result.removeAll(right);
    return result;
  }

  private record TableOwnedEntityIds(
      Set<String> columns,
      Set<String> indexes,
      Set<String> indexColumns,
      Set<String> constraints,
      Set<String> constraintColumns) {
  }

  private static TableOwnedEntityIds collectTableOwnedEntityIds(
      Validation.Database database,
      String tableId) {
    Set<String> columnIds = new HashSet<>();
    Set<String> indexIds = new HashSet<>();
    Set<String> indexColumnIds = new HashSet<>();
    Set<String> constraintIds = new HashSet<>();
    Set<String> constraintColumnIds = new HashSet<>();

    for (Validation.Schema schema : database.getSchemasList()) {
      for (Validation.Table table : schema.getTablesList()) {
        if (!table.getId().equals(tableId)) {
          continue;
        }

        for (Validation.Column column : table.getColumnsList()) {
          columnIds.add(column.getId());
        }

        for (Validation.Index index : table.getIndexesList()) {
          indexIds.add(index.getId());
          for (Validation.IndexColumn indexColumn : index
              .getColumnsList()) {
            indexColumnIds.add(indexColumn.getId());
          }
        }

        for (Validation.Constraint constraint : table
            .getConstraintsList()) {
          constraintIds.add(constraint.getId());
          for (Validation.ConstraintColumn constraintColumn : constraint
              .getColumnsList()) {
            constraintColumnIds.add(constraintColumn.getId());
          }
        }

        return new TableOwnedEntityIds(
            columnIds,
            indexIds,
            indexColumnIds,
            constraintIds,
            constraintColumnIds);
      }
    }

    return new TableOwnedEntityIds(
        columnIds,
        indexIds,
        indexColumnIds,
        constraintIds,
        constraintColumnIds);
  }

}
