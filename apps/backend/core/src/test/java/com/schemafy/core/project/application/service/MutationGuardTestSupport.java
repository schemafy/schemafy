package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.mockito.stubbing.Answer;

import reactor.core.publisher.Mono;

final class MutationGuardTestSupport {

  private MutationGuardTestSupport() {}

  static <T> Answer<Mono<T>> invokeGuardAction() {
    return invocation -> {
      Supplier<Mono<T>> action = invocation.getArgument(1);
      return action.get();
    };
  }

}
