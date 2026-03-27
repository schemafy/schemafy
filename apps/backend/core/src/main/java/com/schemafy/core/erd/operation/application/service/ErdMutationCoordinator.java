package com.schemafy.core.erd.operation.application.service;

import java.util.function.Supplier;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

public interface ErdMutationCoordinator {

  <T> Mono<MutationResult<T>> coordinate(
      ErdOperationType operationType,
      Object payload,
      Supplier<Mono<MutationResult<T>>> mutationSupplier);

  /**
   * Null-object fallback used as the default field value in mutation services.
   * Spring overrides it via setter injection at runtime, while legacy Mockito
   * {@code @InjectMocks} tests can keep instantiating services without an extra mock.
   */
  static ErdMutationCoordinator noop() {
    return new ErdMutationCoordinator() {
      @Override
      public <T> Mono<MutationResult<T>> coordinate(
          ErdOperationType operationType,
          Object payload,
          Supplier<Mono<MutationResult<T>>> mutationSupplier) {
        return mutationSupplier.get();
      }
    };
  }

}
