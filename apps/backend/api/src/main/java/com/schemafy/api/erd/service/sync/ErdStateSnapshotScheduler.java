package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class ErdStateSnapshotScheduler {

  private final ErdStateSnapshotJobStore jobStore;
  private final ErdStateSnapshotWorker worker;
  private final ErdStateSnapshotProperties properties;
  private final LongSupplier currentTimeMillis;
  private final LongSupplier jitterMillis;

  @Autowired
  public ErdStateSnapshotScheduler(ErdStateSnapshotJobStore jobStore,
      ErdStateSnapshotWorker worker,
      ErdStateSnapshotProperties properties) {
    this(jobStore, worker, properties, System::currentTimeMillis,
        () -> randomJitterMillis(properties));
  }

  ErdStateSnapshotScheduler(ErdStateSnapshotJobStore jobStore,
      ErdStateSnapshotWorker worker,
      ErdStateSnapshotProperties properties,
      LongSupplier currentTimeMillis,
      LongSupplier jitterMillis) {
    this.jobStore = jobStore;
    this.worker = worker;
    this.properties = properties;
    this.currentTimeMillis = currentTimeMillis;
    this.jitterMillis = jitterMillis;
  }

  @Scheduled(fixedDelayString = "${collaboration.erd-state-snapshot.poll-interval:50ms}")
  public Mono<Void> poll() {
    return Mono.defer(() -> Mono.delay(
        Duration.ofMillis(jitterMillis.getAsLong()))
        .then(Mono.defer(() -> jobStore.findDueJobKeys(
            currentTimeMillis.getAsLong(), properties.getBatchSize())
            .flatMap(worker::process, properties.getWorkerConcurrency())
            .then())))
        .doOnError(error -> log.warn(
            "[ErdStateSnapshotScheduler] poll failed: {}", error.getMessage()))
        .onErrorResume(error -> Mono.empty());
  }

  private static long randomJitterMillis(ErdStateSnapshotProperties properties) {
    long maxJitterMillis = properties.getPollJitter().toMillis();
    return maxJitterMillis <= 0 ? 0
        : ThreadLocalRandom.current().nextLong(maxJitterMillis + 1);
  }

}
