package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.Table;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeTableNameRequest;
import validation.Validation.DeleteTableRequest;

@Service
@RequiredArgsConstructor
public class TableService {

    private final ValidationClient validationClient;
    private final TableRepository tableRepository;

    public Mono<AffectedMappingResponse> createTable(
            CreateTableRequestWithExtra request) {
        return validationClient.createTable(request.request())
                .delayUntil(database -> tableRepository.save(ErdMapper.toEntity(
                        request.request().getTable(), request.extra())))
                .map(database -> AffectedMappingResponse.of(
                        request.request(),
                        request.request().getDatabase(),
                        database));
    }

    public Mono<Table> getTable(String id) {
        return tableRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Flux<Table> getTablesBySchemaId(String schemaId) {
        return tableRepository.findBySchemaIdAndDeletedAtIsNull(schemaId);
    }

    public Mono<Table> updateTableName(ChangeTableNameRequest request) {
        return tableRepository
                .findByIdAndDeletedAtIsNull(request.getTableId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_TABLE_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.changeTableName(request))
                .doOnNext(table -> table.setName(request.getNewName()))
                .flatMap(tableRepository::save);
    }

    public Mono<Void> deleteTable(DeleteTableRequest request) {
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
