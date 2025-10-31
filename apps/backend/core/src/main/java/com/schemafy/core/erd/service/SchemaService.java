package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeSchemaNameRequest;
import validation.Validation.CreateSchemaRequest;
import validation.Validation.DeleteSchemaRequest;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final ValidationClient validationClient;
    private final SchemaRepository schemaRepository;

    public Mono<AffectedMappingResponse> createSchema(
            CreateSchemaRequest request) {
        return validationClient.createSchema(request)
                .delayUntil(database -> schemaRepository
                        .save(ErdMapper.toEntity(request.getSchema())))
                .map(database -> AffectedMappingResponse.of(
                        request,
                        request.getDatabase(),
                        database));
    }

    public Mono<Schema> getSchema(String id) {
        return schemaRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Flux<Schema> getSchemasByProjectId(String projectId) {
        return schemaRepository.findByProjectIdAndDeletedAtIsNull(projectId);
    }

    public Mono<Schema> updateSchemaName(ChangeSchemaNameRequest request) {
        return schemaRepository
                .findByIdAndDeletedAtIsNull(request.getSchemaId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.changeSchemaName(request))
                .doOnNext(schema -> schema.setName(request.getNewName()))
                .flatMap(schemaRepository::save);
    }

    public Mono<Void> deleteSchema(DeleteSchemaRequest request) {
        return schemaRepository
                .findByIdAndDeletedAtIsNull(request.getSchemaId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)))
                .delayUntil(ignore -> validationClient.deleteSchema(request))
                .doOnNext(Schema::delete)
                .flatMap(schemaRepository::save)
                .then();
    }

}
