package com.schemafy.api.cache.service;

import com.schemafy.api.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;

public interface CacheService {

  Mono<String> get(String key);

  Mono<Void> put(String key, String value);

  Mono<Void> evict(String key);

  Mono<Boolean> exists(String key);

  Mono<CacheStatsDto> getStats();

}
