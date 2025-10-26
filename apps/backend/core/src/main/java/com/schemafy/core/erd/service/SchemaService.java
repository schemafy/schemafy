package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.validation.client.ValidationClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation.CreateSchemaRequest;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final ValidationClient validationClient;
    private final SchemaRepository schemaRepository;

    public Mono<AffectedMappingResponse> createSchema(
            CreateSchemaRequest schema) {
        return validationClient.createSchema(schema)
                .map(database -> AffectedMappingResponse.of(
                        schema.getSchema(),
                        database.getSchemasList().stream()
                                .filter(s -> s.getId()
                                        .equals(schema.getSchema().getId()))
                                .findFirst().orElse(null)));
    }

    public Mono<Schema> getSchema(String id) {
        return schemaRepository.findByIdAndDeletedAtIsNull(id);
    }

    public Flux<Schema> getSchemasByProjectId(String projectId) {
        return schemaRepository.findByProjectIdAndDeletedAtIsNull(projectId);
    }

    public Mono<Schema> changeSchemaName(String schemaId, String newName) {
        return schemaRepository.findById(schemaId)
                .map(schema -> {
                    schema.setName(newName);
                    return schema;
                })
                .flatMap(schemaRepository::save)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)));
    }

    public Mono<Void> deleteSchema(String schemaId) {
        return schemaRepository.findById(schemaId)
                .map(schema -> {
                    schema.delete();
                    return schema;
                })
                .flatMap(schemaRepository::save)
                .then();
    }

}
