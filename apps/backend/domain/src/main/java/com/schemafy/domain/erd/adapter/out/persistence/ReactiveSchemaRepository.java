package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ReactiveSchemaRepository extends ReactiveCrudRepository<SchemaEntity, String> {

}
