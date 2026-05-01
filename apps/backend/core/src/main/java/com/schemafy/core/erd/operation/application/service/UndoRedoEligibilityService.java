package com.schemafy.core.erd.operation.application.service;

import reactor.core.publisher.Mono;

public interface UndoRedoEligibilityService {

  Mono<ResolvedUndoRedoEligibility> resolve(UndoRedoAction action, String targetOpId);

}
