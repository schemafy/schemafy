package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.mapper.ErdMapper;
import com.schemafy.core.erd.model.EntityType;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final ValidationClient validationClient;
    private final SchemaRepository schemaRepository;

    public Mono<AffectedMappingResponse> createSchema(
            Validation.CreateSchemaRequest request) {
        return validationClient.createSchema(request)
                .flatMap(database -> schemaRepository
                        .save(ErdMapper.toEntity(request.getSchema()))
                        .map(savedSchema -> AffectedMappingResponse.of(
                                request,
                                request.getDatabase(),
                                AffectedMappingResponse.updateEntityIdInDatabase(
                                        database,
                                        EntityType.SCHEMA,
                                        request.getSchema().getId(),
                                        savedSchema.getId()
                                )
                        )));
    }

    public Mono<Schema> getSchema(String id) {
        return schemaRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Flux<Schema> getSchemasByProjectId(String projectId) {
        return schemaRepository.findByProjectIdAndDeletedAtIsNull(projectId);
    }

    public Mono<Schema> updateSchemaName(
            Validation.ChangeSchemaNameRequest request) {
        return schemaRepository
                .findByIdAndDeletedAtIsNull(request.getSchemaId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)))
                .delayUntil(
                        ignore -> validationClient.changeSchemaName(request))
                .doOnNext(schema -> schema.setName(request.getNewName()))
                .flatMap(schemaRepository::save);
    }

    public Mono<Void> deleteSchema(Validation.DeleteSchemaRequest request) {
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
