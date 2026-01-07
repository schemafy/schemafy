package com.schemafy.core.cache.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.core.cache.config.CacheProperties;
import com.schemafy.core.cache.service.dto.CacheStatsDto;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service("redisCacheService")
@ConditionalOnRedisEnabled
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final CacheProperties cacheProperties;

  private final AtomicLong hitCount = new AtomicLong(0);
  private final AtomicLong missCount = new AtomicLong(0);

  @Override
  public Mono<String> get(String key) {
    String prefixedKey = getPrefixedKey(key);
    return redisTemplate.opsForValue()
        .get(prefixedKey)
        .doOnNext(v -> hitCount.incrementAndGet())
        .switchIfEmpty(Mono.defer(() -> {
          missCount.incrementAndGet();
          return Mono.empty();
        }));
  }

  @Override
  public Mono<Void> put(String key, String value) {
    String prefixedKey = getPrefixedKey(key);
    Duration ttl = Duration
        .ofMinutes(cacheProperties.getRedis().getDefaultTtlMinutes());
    return redisTemplate.opsForValue()
        .set(prefixedKey, value, ttl)
        .then();
  }

  @Override
  public Mono<Void> evict(String key) {
    String prefixedKey = getPrefixedKey(key);
    return redisTemplate.delete(prefixedKey).then();
  }

  @Override
  public Mono<Boolean> exists(String key) {
    String prefixedKey = getPrefixedKey(key);
    return redisTemplate.hasKey(prefixedKey);
  }

  @Override
  public Mono<CacheStatsDto> getStats() {
    return redisTemplate.execute(connection -> connection.serverCommands()
        .dbSize())
        .next()
        .defaultIfEmpty(0L)
        .map(size -> {
          long hits = hitCount.get();
          long misses = missCount.get();
          double hitRate = (hits + misses > 0)
              ? (double) hits / (hits + misses)
              : 0.0;
          return new CacheStatsDto(hits, misses, hitRate, size);
        });
  }

  private String getPrefixedKey(String key) {
    return cacheProperties.getRedis().getKeyPrefix() + key;
  }

}
