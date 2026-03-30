package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ErdOperationLogRepository extends ReactiveCrudRepository<ErdOperationLogEntity, String> {
}
