package com.schemafy.api.erd.service.sync;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ErdStateSnapshotProducer {

  private final ErdStateSnapshotJobStore jobStore;

  public Mono<Void> enqueueActive(String projectId, String schemaId,
      long targetRevision) {
    return Mono.defer(() -> jobStore.enqueueActive(projectId, schemaId,
        targetRevision));
  }

  public Mono<Void> enqueueDeleted(String projectId, String schemaId,
      long targetRevision) {
    return Mono.defer(() -> jobStore.enqueueDeleted(projectId, schemaId,
        targetRevision));
  }

}
