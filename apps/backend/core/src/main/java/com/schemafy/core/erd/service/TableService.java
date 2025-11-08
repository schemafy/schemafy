package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.Table;
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

    public Mono<AffectedMappingResponse> createTable(
            CreateTableRequestWithExtra request) {
        return validationClient.createTable(request.request())
                .flatMap(database -> tableRepository.save(ErdMapper.toEntity(
                        request.request().getTable(), request.extra()))
                        .map(savedTable -> AffectedMappingResponse.of(
                                request.request(),
                                request.request().getDatabase(),
                                AffectedMappingResponse.updateEntityIdInDatabase(
                                        database,
                                        EntityType.TABLE,
                                        request.request().getTable().getId(),
                                        savedTable.getId()
                                )
                        )));
    }

    public Mono<TableResponse> getTable(String id) {
        return tableRepository.findByIdAndDeletedAtIsNull(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
                .map(TableResponse::from);
    }

    public Flux<TableResponse> getTablesBySchemaId(String schemaId) {
        return tableRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
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

    public Mono<Void> deleteTable(Validation.DeleteTableRequest request) {
        return tableRepository
                .findByIdAndDeletedAtIsNull(request.getTableId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.deleteTable(request))
                .doOnNext(Table::delete)
                .flatMap(tableRepository::save)
                .then();
    }

}
