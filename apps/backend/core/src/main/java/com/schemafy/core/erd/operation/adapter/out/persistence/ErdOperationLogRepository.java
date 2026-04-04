package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

interface ErdOperationLogRepository extends ReactiveCrudRepository<ErdOperationLogEntity, String> {

  Flux<ErdOperationLogEntity> findAllBySchemaIdOrderByCommittedRevisionAsc(String schemaId);

}
