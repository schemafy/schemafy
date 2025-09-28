package com.schemafy.core.cache.service;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CacheService {

    Mono<String> get(String key);

    Mono<Void> put(String key, String value, Duration ttl);

    Mono<Void> evict(String key);

    Mono<Boolean> exists(String key);

    Mono<String> getStats();
}
