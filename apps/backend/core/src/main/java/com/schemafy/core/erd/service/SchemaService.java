package com.schemafy.core.erd.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final SchemaRepository schemaRepository;

    public Mono<Schema> createSchema(Schema schema) {
        return schemaRepository.save(schema);
    }

    public Mono<Schema> getSchema(String id) {
        return schemaRepository.findById(id);
    }

    public Flux<Schema> getSchemasByProjectId(String projectId) {
        return schemaRepository.findByProjectId(projectId);
    }

    public Mono<Schema> changeSchemaName(Schema schema, String name) {
        schema.setName(name);
        return schemaRepository.save(schema);
    }

    public Mono<Void> deleteSchema(Schema schema) {
        schema.delete();
        return schemaRepository.save(schema).then();
    }

}
