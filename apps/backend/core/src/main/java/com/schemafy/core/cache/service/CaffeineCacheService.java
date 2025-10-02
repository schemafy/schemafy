package com.schemafy.core.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "CAFFEINE", matchIfMissing = true)
public class CaffeineCacheService implements CacheService {

    private final Cache<String, String> cache;

    public CaffeineCacheService() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.fromCallable(() -> cache.getIfPresent(key))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> put(String key, String value, Duration ttl) {
        return Mono.fromRunnable(() -> cache.put(key, value))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> evict(String key) {
        return Mono.fromRunnable(() -> cache.invalidate(key))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.fromCallable(() -> cache.getIfPresent(key) != null)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> getStats() {
        return Mono.fromCallable(() -> {
            CacheStats stats = cache.stats();
            return String.format(
                    "Cache Stats - Hits: %d, Misses: %d, Hit Rate: %.2f%%, Size: %d\n",
                    stats.hitCount(),
                    stats.missCount(),
                    stats.hitRate() * 100,
                    cache.estimatedSize());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
