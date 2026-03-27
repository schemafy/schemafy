package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface SchemaCollaborationStateRepository
    extends ReactiveCrudRepository<SchemaCollaborationStateEntity, String> {
}
