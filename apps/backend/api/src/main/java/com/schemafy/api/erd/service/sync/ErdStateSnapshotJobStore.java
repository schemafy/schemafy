package com.schemafy.api.erd.service.sync;

import java.time.Duration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ErdStateSnapshotJobStore {

  Mono<Void> enqueueActive(String projectId, String schemaId,
      long targetRevision);

  Mono<Void> enqueueDeleted(String projectId, String schemaId,
      long targetRevision);

  Flux<String> findDueJobKeys(long nowEpochMillis, int limit);

  Mono<ErdStateSnapshotJob> claim(String jobKey, String leaseToken,
      long nowEpochMillis, Duration leaseTtl);

  Mono<Boolean> publishIfCurrent(ErdStateSnapshotJob job,
      long candidateRevision, String payload);

  Mono<Boolean> renewLease(ErdStateSnapshotJob job,
      long nowEpochMillis, Duration leaseTtl);

  Mono<Void> complete(ErdStateSnapshotJob job, long publishedRevision,
      long nowEpochMillis);

  Mono<Void> requeue(ErdStateSnapshotJob job, long nowEpochMillis,
      Duration delay, ErdStateSnapshotRequeueReason reason);

}
