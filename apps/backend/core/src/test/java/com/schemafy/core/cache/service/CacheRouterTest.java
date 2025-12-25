package com.schemafy.core.cache.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.schemafy.core.cache.config.CacheType;

import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
class CacheRouterTest {

    @Autowired
    private CacheRouter cacheRouter;

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
    void testPutAndGetWithRedis_ShouldFallbackToCaffeine() {
        // Redis is disabled in test, should fallback to Caffeine
        String key = "test-redis-key";
        String value = "test-redis-value";

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

}
