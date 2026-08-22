package com.schemafy.api.erd.service.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ErdStateSnapshotSchedulerTest {

  @Mock
  private ErdStateSnapshotJobStore jobStore;
  @Mock
  private ErdStateSnapshotWorker worker;

  @Test
  void processesDueJobsFromTheSharedQueue() {
    ErdStateSnapshotProperties properties = new ErdStateSnapshotProperties();
    properties.setBatchSize(2);
    properties.setWorkerConcurrency(1);
    ErdStateSnapshotScheduler scheduler = new ErdStateSnapshotScheduler(
        jobStore, worker, properties, () -> 2_000L);
    given(jobStore.findDueJobKeys(2_000L, 2))
        .willReturn(Flux.just("job-1", "job-2"));
    given(worker.process("job-1")).willReturn(Mono.empty());
    given(worker.process("job-2")).willReturn(Mono.empty());

    scheduler.poll().block();

    InOrder inOrder = inOrder(worker);
    inOrder.verify(worker).process("job-1");
    inOrder.verify(worker).process("job-2");
  }

}
