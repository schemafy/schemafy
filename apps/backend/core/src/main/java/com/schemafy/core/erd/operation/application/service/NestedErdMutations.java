package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

import com.schemafy.core.erd.operation.ErdOperationContexts;

import reactor.core.publisher.Mono;

public final class NestedErdMutations {

  private NestedErdMutations() {}

  public static <T> Mono<T> run(Mono<T> nestedMutation) {
    Objects.requireNonNull(nestedMutation, "nestedMutation");
    return nestedMutation.contextWrite(ErdOperationContexts.suppressNestedMutation());
  }

}
