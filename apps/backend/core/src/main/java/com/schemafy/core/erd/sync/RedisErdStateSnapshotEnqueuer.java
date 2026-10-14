package com.schemafy.core.erd.sync;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
class RedisErdStateSnapshotEnqueuer implements ErdStateSnapshotEnqueuer {

  private static final String DUE_KEY = "erd:state-snapshot:{coord}:due";
  private static final String JOB_KEY_PREFIX = "erd:state-snapshot:{coord}:job:";
  private static final RedisScript<Long> ENQUEUE_ACTIVE = RedisScript.of(
      new ClassPathResource("redis/erd-state-snapshot/enqueue-active.lua"), Long.class);
  private static final RedisScript<Long> ENQUEUE_DELETED = RedisScript.of(
      new ClassPathResource("redis/erd-state-snapshot/enqueue-deleted.lua"), Long.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ErdStateSnapshotEnqueueProperties properties;

  @Override
  public Mono<Void> enqueueActive(String projectId, String schemaId, long targetRevision) {
    long now = System.currentTimeMillis();
    return redisTemplate.execute(ENQUEUE_ACTIVE, keys(projectId, schemaId),
        List.of(projectId, schemaId, Long.toString(targetRevision), Long.toString(now),
            Long.toString(properties.getDebounce().toMillis()),
            Long.toString(properties.getMaxWait().toMillis()),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .then();
  }

  @Override
  public Mono<Void> enqueueDeleted(String projectId, String schemaId, long targetRevision) {
    long now = System.currentTimeMillis();
    return redisTemplate.execute(ENQUEUE_DELETED, keys(projectId, schemaId),
        List.of(projectId, schemaId, Long.toString(targetRevision), Long.toString(now),
            Long.toString(properties.getCompletedWatermarkTtl().toMillis())))
        .then();
  }

  private List<String> keys(String projectId, String schemaId) {
    return List.of(DUE_KEY, JOB_KEY_PREFIX + projectId + ":" + schemaId);
  }

}
