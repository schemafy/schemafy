package com.schemafy.core.erd.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;

public abstract class BaseErdService {

    protected final TransactionalOperator transactionalOperator;

    protected BaseErdService(TransactionalOperator transactionalOperator) {
        this.transactionalOperator = transactionalOperator;
    }

    protected <T> Mono<T> transactional(Mono<T> operation) {
        return transactionalOperator.transactional(operation);
    }

}
