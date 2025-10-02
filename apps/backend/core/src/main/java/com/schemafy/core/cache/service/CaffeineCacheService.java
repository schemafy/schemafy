package com.schemafy.core.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.schemafy.core.cache.config.CacheProperties;
import com.schemafy.core.cache.service.dto.CacheStatsDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "CAFFEINE", matchIfMissing = true)
public class CaffeineCacheService implements CacheService {

    private final Cache<String, String> cache;

    public CaffeineCacheService(CacheProperties properties) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .expireAfterWrite(properties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                .expireAfterAccess(properties.getExpireAfterAccessMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.justOrEmpty(cache.getIfPresent(key));
    }

    @Override
    public Mono<Void> put(String key, String value, Duration ttl) {
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
