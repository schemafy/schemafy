package com.schemafy.core.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.schemafy.core.cache.service.CacheService;
import com.schemafy.core.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;

@Configuration
@Profile({ "test", "dev" })
public class TestCacheConfig {

    @Bean("redisCacheService")
    public CacheService mockRedisCacheService() {
        return new CacheService() {

            @Override
            public Mono<String> get(String key) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> put(String key, String value) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> evict(String key) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> exists(String key) {
                return Mono.just(false);
            }

            @Override
            public Mono<CacheStatsDto> getStats() {
                return Mono.just(new CacheStatsDto(0L, 0L, 0.0, 0L));
            }

        };
    }

}
