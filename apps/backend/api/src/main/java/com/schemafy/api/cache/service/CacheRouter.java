package com.schemafy.api.cache.service;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.schemafy.api.cache.config.CacheType;
import com.schemafy.api.cache.service.dto.CacheStatsDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CacheRouter {

  private final CacheService caffeineCacheService;
  private final ObjectProvider<CacheService> redisCacheServiceProvider;
  @Nullable
  private CacheService redisCacheService;

  public CacheRouter(
      @Qualifier("caffeineCacheService") CacheService caffeineCacheService,
      @Qualifier("redisCacheService") ObjectProvider<CacheService> redisCacheServiceProvider) {
    this.caffeineCacheService = caffeineCacheService;
    this.redisCacheServiceProvider = redisCacheServiceProvider;
  }

  @PostConstruct
  void initialize() {
    this.redisCacheService = redisCacheServiceProvider.getIfAvailable();
    if (redisCacheService != null) {
      log.info("CacheRouter initialized with Caffeine and Redis caches");
    } else {
      log.info("CacheRouter initialized with Caffeine cache only");
    }
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
    case REDIS -> {
      if (redisCacheService == null) {
        throw new IllegalStateException("Redis cache is disabled");
      }
      yield redisCacheService;
    }
    };
  }

}
