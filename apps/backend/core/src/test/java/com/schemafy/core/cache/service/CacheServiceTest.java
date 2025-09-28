package com.schemafy.core.cache.service;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Test
    void testPutAndGet() {
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key, value, ttl)
                        .then(cacheService.get(key)))
                .expectNext(value)
                .verifyComplete();
    }

    @Test
    void testGetNonExistentKey() {
        String key = "non-existent-key";

        StepVerifier.create(cacheService.get(key))
                .expectComplete()
                .verify();
    }

    @Test
    void testExists() {
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key, value, ttl)
                        .then(cacheService.exists(key)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testExistsForNonExistentKey() {
        String key = "non-existent-key";

        StepVerifier.create(cacheService.exists(key))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testEvict() {
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key, value, ttl)
                        .then(cacheService.evict(key))
                        .then(cacheService.exists(key)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testGetStats() {
        String key = "test-key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key, value, ttl)
                        .then(cacheService.getStats()))
                .expectNextMatches(stats -> stats.contains("Cache Stats") &&
                        stats.contains("Hits") &&
                        stats.contains("Misses"))
                .verifyComplete();
    }

    @Test
    void testMultipleOperations() {
        String key1 = "key1";
        String key2 = "key2";
        String value1 = "value1";
        String value2 = "value2";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key1, value1, ttl)
                        .then(cacheService.get(key1)))
                .expectNext(value1)
                .verifyComplete();

        StepVerifier.create(
                cacheService.put(key2, value2, ttl)
                        .then(cacheService.get(key2)))
                .expectNext(value2)
                .verifyComplete();
    }

    @Test
    void testCacheEvictAfterPut() {
        String key = "evict-test-key";
        String value = "evict-test-value";
        Duration ttl = Duration.ofMinutes(5);

        StepVerifier.create(
                cacheService.put(key, value, ttl)
                        .then(cacheService.exists(key)))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(
                cacheService.evict(key)
                        .then(cacheService.exists(key)))
                .expectNext(false)
                .verifyComplete();
    }
}
