package com.schemafy.core.erd.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedColumnsResponse;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class ColumnService {

    private final ValidationClient validationClient;
    private final ColumnRepository columnRepository;
    private final AffectedEntitiesSoftDeleter affectedEntitiesSoftDeleter;
    private final TransactionalOperator transactionalOperator;

    public Mono<AffectedMappingResponse> createColumn(
            Validation.CreateColumnRequest request) {
        return validationClient.createColumn(request)
                .flatMap(database -> columnRepository
                        .save(ErdMapper.toEntity(request.getColumn()))
                        .map(savedColumn -> AffectedMappingResponse.of(
                                request,
                                request.getDatabase(),
                                AffectedMappingResponse
                                        .updateEntityIdInDatabase(
                                                database,
                                                EntityType.COLUMN,
                                                request.getColumn().getId(),
                                                savedColumn.getId()))));
    }

    public Mono<ColumnResponse> getColumn(String id) {
        return columnRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .map(ColumnResponse::from);
    }

    public Flux<ColumnResponse> getColumnsByTableId(String tableId) {
        return columnRepository.findByTableIdAndDeletedAtIsNull(tableId)
                .map(ColumnResponse::from);
    }

    public Mono<ColumnResponse> updateColumnName(
            Validation.ChangeColumnNameRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.changeColumnName(request))
                .doOnNext(column -> column.setName(request.getNewName()))
                .flatMap(columnRepository::save)
                .map(ColumnResponse::from);
    }

    public Mono<AffectedColumnsResponse> updateColumnType(
            Validation.ChangeColumnTypeRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .flatMap(ignore -> validationClient.changeColumnType(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(saveAffectedColumns(
                                        afterDatabase))));
    }

    private Mono<AffectedColumnsResponse> saveAffectedColumns(
            Validation.Database afterDatabase) {
        List<Validation.Column> affectedColumns = collectAffectedColumns(
                afterDatabase);

        return Flux.fromIterable(affectedColumns)
                .concatMap(column -> columnRepository
                        .findByIdAndDeletedAtIsNull(column.getId())
                        .switchIfEmpty(Mono.error(new BusinessException(
                                ErrorCode.ERD_COLUMN_NOT_FOUND)))
                        .doOnNext(entity -> {
                            entity.setDataType(column.getDataType());
                            entity.setLengthScale(column.getLengthScale());
                        })
                        .flatMap(columnRepository::save)
                        .map(ColumnResponse::from))
                .collectList()
                .map(AffectedColumnsResponse::new);
    }

    private static List<Validation.Column> collectAffectedColumns(
            Validation.Database database) {
        List<Validation.Column> affectedColumns = new ArrayList<>();
        for (Validation.Schema schema : database.getSchemasList()) {
            for (Validation.Table table : schema.getTablesList()) {
                for (Validation.Column column : table.getColumnsList()) {
                    if (column.getIsAffected()) {
                        affectedColumns.add(column);
                    }
                }
            }
        }
        return affectedColumns;
    }

    public Mono<ColumnResponse> updateColumnPosition(
            Validation.ChangeColumnPositionRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient
                                .changeColumnPosition(request))
                .doOnNext(column -> column
                        .setOrdinalPosition(request.getNewPosition()))
                .flatMap(columnRepository::save)
                .map(ColumnResponse::from);
    }

    public Mono<Void> deleteColumn(Validation.DeleteColumnRequest request) {
        if (!request.hasDatabase()) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .flatMap(column -> validationClient.deleteColumn(request)
                        .flatMap(afterDatabase -> transactionalOperator
                                .transactional(Mono.just(column)
                                        .doOnNext(entity -> entity.delete())
                                        .flatMap(columnRepository::save)
                                        .then(affectedEntitiesSoftDeleter
                                                .softDeleteRemovedEntities(
                                                        request.getDatabase(),
                                                        afterDatabase)))))
                .then();
    }

}
