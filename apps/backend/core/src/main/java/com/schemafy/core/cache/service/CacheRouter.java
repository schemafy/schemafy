package com.schemafy.core.cache.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.schemafy.core.cache.config.CacheType;
import com.schemafy.core.cache.service.dto.CacheStatsDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CacheRouter {

    private final CacheService caffeineCacheService;
    private final CacheService redisCacheService;

    public CacheRouter(
            CacheService caffeineCacheService,
            @Qualifier("redisCacheService") CacheService redisCacheService) {
        this.caffeineCacheService = caffeineCacheService;
        this.redisCacheService = redisCacheService;
        log.info("CacheRouter initialized with Caffeine and Redis caches");
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
        case REDIS -> redisCacheService;
        };
    }

}
