package com.schemafy.core.erd.sync;

import reactor.core.publisher.Mono;

public interface ErdStateSnapshotEnqueuer {

  Mono<Void> enqueueActive(String projectId, String schemaId, long targetRevision);

  Mono<Void> enqueueDeleted(String projectId, String schemaId, long targetRevision);

}
