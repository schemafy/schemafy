package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.entity.Column;
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

    public Mono<AffectedMappingResponse> createColumn(
            Validation.CreateColumnRequest request) {
        return validationClient.createColumn(request)
                .flatMap(database -> columnRepository
                        .save(ErdMapper.toEntity(request.getColumn()))
                        .map(savedColumn -> AffectedMappingResponse.of(
                                request,
                                request.getDatabase(),
                                AffectedMappingResponse.updateEntityIdInDatabase(
                                        database,
                                        EntityType.COLUMN,
                                        request.getColumn().getId(),
                                        savedColumn.getId()
                                )
                        )));
    }

    public Mono<Column> getColumn(String id) {
        return columnRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Flux<Column> getColumnsByTableId(String tableId) {
        return columnRepository.findByTableIdAndDeletedAtIsNull(tableId);
    }

    public Mono<Column> updateColumnName(
            Validation.ChangeColumnNameRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.changeColumnName(request))
                .doOnNext(column -> column.setName(request.getNewName()))
                .flatMap(columnRepository::save);
    }

    public Mono<Column> updateColumnType(
            Validation.ChangeColumnTypeRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.changeColumnType(request))
                .doOnNext(column -> column.setDataType(request.getDataType()))
                .flatMap(columnRepository::save);
    }

    public Mono<Column> updateColumnPosition(
            Validation.ChangeColumnPositionRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient
                                .changeColumnPosition(request))
                .doOnNext(column -> column.setOrdinalPosition(
                        request.getNewPosition()))
                .flatMap(columnRepository::save);
    }

    public Mono<Void> deleteColumn(Validation.DeleteColumnRequest request) {
        return columnRepository
                .findByIdAndDeletedAtIsNull(request.getColumnId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_COLUMN_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.deleteColumn(request))
                .doOnNext(Column::delete)
                .flatMap(columnRepository::save)
                .then();
    }

}
