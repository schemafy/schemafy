package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.CollaborationChannel;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnRedisEnabled
public class RedisErdStateSnapshotJobStore implements ErdStateSnapshotJobStore {

  private static final String DUE_KEY = "erd:state-snapshot:{coord}:due";
  private static final String JOB_KEY_PREFIX = "erd:state-snapshot:{coord}:job:";
  // Bounds per-tick stale-key probing: caps candidate fetch instead of
  // scanning the entire due ZSET when garbage (expired job hashes with a
  // surviving ZSET member) accumulates ahead of live jobs.
  private static final int STALE_SCAN_MULTIPLIER = 5;

  private final ReactiveStringRedisTemplate redisTemplate;
  private final JsonCodec jsonCodec;
  private final ErdStateSnapshotProperties properties;
  private final LongSupplier currentTimeMillis;

  @Autowired
  public RedisErdStateSnapshotJobStore(
      ReactiveStringRedisTemplate redisTemplate,
      JsonCodec jsonCodec,
      ErdStateSnapshotProperties properties) {
    this(redisTemplate, jsonCodec, properties, System::currentTimeMillis);
  }

  RedisErdStateSnapshotJobStore(
      ReactiveStringRedisTemplate redisTemplate,
      JsonCodec jsonCodec,
      ErdStateSnapshotProperties properties,
      LongSupplier currentTimeMillis) {
    this.redisTemplate = redisTemplate;
    this.jsonCodec = jsonCodec;
    this.properties = properties;
    this.currentTimeMillis = currentTimeMillis;
  }

  @Override
  public Mono<Void> enqueueActive(String projectId, String schemaId,
      long targetRevision) {
    long now = currentTimeMillis.getAsLong();
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.ENQUEUE_ACTIVE,
        keys(projectId, schemaId),
        List.of(projectId, schemaId, Long.toString(targetRevision),
            Long.toString(now), Long.toString(properties.getDebounce().toMillis()),
            Long.toString(properties.getMaxWait().toMillis()),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .then();
  }

  @Override
  public Mono<Void> enqueueDeleted(String projectId, String schemaId,
      long targetRevision) {
    long now = currentTimeMillis.getAsLong();
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.ENQUEUE_DELETED,
        keys(projectId, schemaId),
        List.of(projectId, schemaId, Long.toString(targetRevision),
            Long.toString(now),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .then();
  }

  @Override
  public Flux<String> findDueJobKeys(long nowEpochMillis, int limit) {
    Range<Double> dueRange = Range.closed(0D, (double) nowEpochMillis);
    Limit scanLimit = Limit.limit().count(limit * STALE_SCAN_MULTIPLIER);
    return redisTemplate.opsForZSet()
        .rangeByScore(DUE_KEY, dueRange, scanLimit)
        .concatMap(this::removeIfStale)
        .take(limit);
  }

  @Override
  public Mono<ErdStateSnapshotJob> claim(String jobKey, String leaseToken,
      long nowEpochMillis, Duration leaseTtl) {
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.CLAIM,
        List.of(DUE_KEY, jobKey),
        List.of(jobKey, leaseToken, Long.toString(nowEpochMillis),
            Long.toString(leaseTtl.toMillis()),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .next()
        .map(payload -> jsonCodec.fromJson(payload, ErdStateSnapshotJob.class));
  }

  @Override
  public Mono<Boolean> publishIfCurrent(ErdStateSnapshotJob job,
      long candidateRevision, String payload) {
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.PUBLISH_IF_CURRENT,
        List.of(job.jobKey()),
        List.of(CollaborationChannel.forProject(job.projectId()),
            job.leaseToken(), Long.toString(job.generation()),
            job.kind().name(), Long.toString(candidateRevision), payload))
        .next()
        .map(result -> result == 1L)
        .defaultIfEmpty(false);
  }

  @Override
  public Mono<Boolean> renewLease(ErdStateSnapshotJob job,
      long nowEpochMillis, Duration leaseTtl) {
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.RENEW_LEASE,
        List.of(DUE_KEY, job.jobKey()),
        List.of(job.leaseToken(), Long.toString(job.generation()),
            Long.toString(nowEpochMillis), Long.toString(leaseTtl.toMillis()),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .next()
        .map(result -> result == 1L)
        .defaultIfEmpty(false);
  }

  @Override
  public Mono<Void> complete(ErdStateSnapshotJob job, long publishedRevision,
      long nowEpochMillis) {
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.COMPLETE,
        List.of(DUE_KEY, job.jobKey()),
        List.of(job.leaseToken(), Long.toString(job.generation()),
            job.kind().name(), Long.toString(publishedRevision),
            Long.toString(nowEpochMillis),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .next()
        .flatMap(applied -> requireApplied(applied, job, "complete"));
  }

  @Override
  public Mono<Void> requeue(ErdStateSnapshotJob job, long nowEpochMillis,
      Duration delay, ErdStateSnapshotRequeueReason reason) {
    return redisTemplate.execute(ErdStateSnapshotRedisScripts.REQUEUE,
        List.of(DUE_KEY, job.jobKey()),
        List.of(job.leaseToken(), Long.toString(job.generation()),
            Long.toString(nowEpochMillis), Long.toString(delay.toMillis()),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis()),
            Boolean.toString(reason.incrementFailureCount())))
        .next()
        .flatMap(applied -> requireApplied(applied, job, "requeue"));
  }

  private Mono<Void> requireApplied(Long applied, ErdStateSnapshotJob job,
      String operation) {
    if (applied == 1L) {
      return Mono.empty();
    }
    return Mono.error(new JobTransitionRejectedException(
        "%s rejected for jobKey=%s: lease/generation/kind no longer matches"
            .formatted(operation, job.jobKey())));
  }

  private Mono<String> removeIfStale(String jobKey) {
    return redisTemplate.hasKey(jobKey)
        .flatMap(exists -> exists
            ? Mono.just(jobKey)
            : redisTemplate.opsForZSet().remove(DUE_KEY, jobKey)
                .then(Mono.empty()));
  }

  private List<String> keys(String projectId, String schemaId) {
    return List.of(DUE_KEY, JOB_KEY_PREFIX + projectId + ":" + schemaId);
  }

}
