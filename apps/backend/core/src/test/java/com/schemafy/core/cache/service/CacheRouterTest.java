package com.schemafy.core.cache.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.Test;

import com.schemafy.core.cache.config.CacheType;
import com.schemafy.core.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CacheRouterTest {

    @Autowired
    private CacheRouter cacheRouter;

    @MockitoBean
    @Qualifier("redisCacheService")
    private CacheService redisCacheService;

    @Test
    void testPutAndGetWithCaffeine() {
        String key = "test-caffeine-key";
        String value = "test-caffeine-value";

        StepVerifier.create(
                cacheRouter.put(key, value, CacheType.CAFFEINE)
                        .then(cacheRouter.get(key, CacheType.CAFFEINE)))
                .expectNext(value)
                .verifyComplete();
    }

    @Test
    void testPutAndGetWithRedis() {
        String key = "test-redis-key";
        String value = "test-redis-value";

        // Mock Redis 동작
        when(redisCacheService.put(anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(redisCacheService.get(anyString())).thenReturn(Mono.just(value));

        StepVerifier.create(
                cacheRouter.put(key, value, CacheType.REDIS)
                        .then(cacheRouter.get(key, CacheType.REDIS)))
                .expectNext(value)
                .verifyComplete();
    }

    @Test
    void testEvictWithCaffeine() {
        String key = "test-evict-key";
        String value = "test-evict-value";

        StepVerifier.create(
                cacheRouter.put(key, value, CacheType.CAFFEINE)
                        .then(cacheRouter.evict(key, CacheType.CAFFEINE))
                        .then(cacheRouter.exists(key, CacheType.CAFFEINE)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testExistsWithCaffeine() {
        String key = "test-exists-key";
        String value = "test-exists-value";

        StepVerifier.create(
                cacheRouter.put(key, value, CacheType.CAFFEINE)
                        .then(cacheRouter.exists(key, CacheType.CAFFEINE)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testGetStats() {
        StepVerifier.create(cacheRouter.getStats(CacheType.CAFFEINE))
                .expectNextMatches(stats -> stats != null)
                .verifyComplete();
    }

    @Test
    void testGetStatsWithRedis() {
        CacheStatsDto mockStats = new CacheStatsDto(100L, 10L, 0.9, 50L);
        when(redisCacheService.getStats()).thenReturn(Mono.just(mockStats));

        StepVerifier.create(cacheRouter.getStats(CacheType.REDIS))
                .expectNext(mockStats)
                .verifyComplete();
    }

}
