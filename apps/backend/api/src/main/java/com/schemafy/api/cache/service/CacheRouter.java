package com.schemafy.api.cache.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.schemafy.api.cache.config.CacheType;
import com.schemafy.api.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;

@Service
public class CacheRouter {

  private final CacheService caffeineCacheService;
  private final ObjectProvider<CacheService> redisCacheServiceProvider;

  public CacheRouter(
      @Qualifier("caffeineCacheService") CacheService caffeineCacheService,
      @Qualifier("redisCacheService") ObjectProvider<CacheService> redisCacheServiceProvider) {
    this.caffeineCacheService = caffeineCacheService;
    this.redisCacheServiceProvider = redisCacheServiceProvider;
  }

  public Mono<String> get(String key, CacheType cacheType) {
    return selectCache(cacheType).get(key);
  }

  public Mono<Void> put(String key, String value, CacheType cacheType) {
    return selectCache(cacheType).put(key, value);
  }

  public Mono<Void> evict(String key, CacheType cacheType) {
    return selectCache(cacheType).evict(key);
  }

  public Mono<Boolean> exists(String key, CacheType cacheType) {
    return selectCache(cacheType).exists(key);
  }

  public Mono<CacheStatsDto> getStats(CacheType cacheType) {
    return selectCache(cacheType).getStats();
  }

  private CacheService selectCache(CacheType cacheType) {
    return switch (cacheType) {
    case CAFFEINE -> caffeineCacheService;
    case REDIS -> redisCacheService();
    };
  }

  private CacheService redisCacheService() {
    CacheService redisCacheService = redisCacheServiceProvider.getIfAvailable();
    if (redisCacheService == null) {
      throw new IllegalStateException("Redis cache is disabled");
    }
    return redisCacheService;
  }

}
