package com.schemafy.api.erd.service.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import reactor.core.publisher.Mono;

/** Enqueues snapshot jobs into Redis. A single attempt only: enqueue failures
 * are on the synchronous mutation response path, so they are left for the
 * caller (ErdStateSyncPublisher) to log and swallow rather than retried here
 * — retrying would add repeated Redis round-trip latency to every mutation
 * response during a transient Redis blip. */
@Service
@ConditionalOnRedisEnabled
public class ErdStateSnapshotProducer {

  private final ErdStateSnapshotJobStore jobStore;

  @Autowired
  public ErdStateSnapshotProducer(ErdStateSnapshotJobStore jobStore) {
    this.jobStore = jobStore;
  }

  public Mono<Void> enqueueActive(String projectId, String schemaId,
      long targetRevision) {
    return jobStore.enqueueActive(projectId, schemaId, targetRevision);
  }

  public Mono<Void> enqueueDeleted(String projectId, String schemaId,
      long targetRevision) {
    return jobStore.enqueueDeleted(projectId, schemaId, targetRevision);
  }

}
