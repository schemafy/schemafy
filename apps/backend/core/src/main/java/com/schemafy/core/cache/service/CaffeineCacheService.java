package com.schemafy.core.cache.service;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.schemafy.core.cache.config.CacheProperties;
import com.schemafy.core.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;

@Primary
@Service("caffeineCacheService")
@ConditionalOnProperty(name = "cache.caffeine.enabled", havingValue = "true", matchIfMissing = true)
public class CaffeineCacheService implements CacheService {

  private final Cache<String, String> cache;

  public CaffeineCacheService(CacheProperties properties) {
    CacheProperties.CaffeineProperties caffeine = properties.getCaffeine();
    this.cache = Caffeine.newBuilder()
        .maximumSize(caffeine.getMaximumSize())
        .expireAfterWrite(caffeine.getExpireAfterWriteMinutes(),
            TimeUnit.MINUTES)
        .expireAfterAccess(caffeine.getExpireAfterAccessMinutes(),
            TimeUnit.MINUTES)
        .recordStats()
        .build();
  }

  @Override
  public Mono<String> get(String key) {
    return Mono.justOrEmpty(cache.getIfPresent(key));
  }

  @Override
  public Mono<Void> put(String key, String value) {
    cache.put(key, value);
    return Mono.empty();
  }

  @Override
  public Mono<Void> evict(String key) {
    cache.invalidate(key);
    return Mono.empty();
  }

  @Override
  public Mono<Boolean> exists(String key) {
    return Mono.just(cache.getIfPresent(key) != null);
  }

  @Override
  public Mono<CacheStatsDto> getStats() {
    CacheStats stats = cache.stats();
    return Mono.just(new CacheStatsDto(
        stats.hitCount(),
        stats.missCount(),
        stats.hitRate(),
        cache.estimatedSize()));
  }

}
