package com.schemafy.core.erd.operation.adapter.out.persistence;

import reactor.core.publisher.Flux;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ErdOperationLogRepository extends ReactiveCrudRepository<ErdOperationLogEntity, String> {

  Flux<ErdOperationLogEntity> findAllBySchemaIdOrderByCommittedRevisionAsc(String schemaId);

}
