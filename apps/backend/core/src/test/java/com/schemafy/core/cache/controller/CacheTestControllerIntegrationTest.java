package com.schemafy.core.cache.controller;

import com.schemafy.core.cache.service.CacheService;
import com.schemafy.core.cache.config.CacheProviderSelector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "cache.type=caffeine"
})
class CacheTestControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheProviderSelector cacheProviderSelector;

    @Test
    void testCacheProviderIntegration() {
        assertEquals("caffeine", cacheProviderSelector.getCurrentCacheType());
        assertTrue(cacheProviderSelector.getAvailableProviders().contains("caffeine"));
        assertTrue(cacheProviderSelector.getAvailableProviders().contains("redis"));

        webTestClient.get()
                .uri("/api/cache/provider")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("Current Cache Provider: caffeine"));
                    assertTrue(body.contains("Available Providers"));
                });
    }

    @Test
    void testCacheServiceIntegration() {
        String key = "integration-service-test";
        String value = "integration-service-value";

        assertNotNull(cacheService);

        StepVerifier.create(
                cacheService.put(key, value, java.time.Duration.ofMinutes(5))
                        .then(cacheService.get(key)))
                .expectNext(value)
                .verifyComplete();

        webTestClient.get()
                .uri("/api/cache/{key}", key)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertEquals(value, body));
    }

    @Test
    void testCacheConsistencyBetweenServiceAndAPI() {
        String key = "consistency-test-key";
        String value = "consistency-test-value";

        StepVerifier.create(
                cacheService.put(key, value, java.time.Duration.ofMinutes(5)))
                .verifyComplete();

        webTestClient.get()
                .uri("/api/cache/{key}", key)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertEquals(value, body));

        webTestClient.get()
                .uri("/api/cache/{key}", "non-existent-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertEquals("Key not found", body));
    }

    @Test
    void testEndToEndCacheWorkflow() {
        String key = "e2e-test-key";
        String value = "e2e-test-value";

        webTestClient.post()
                .uri("/api/cache/{key}", key)
                .bodyValue(value)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("stored successfully")));

        StepVerifier.create(cacheService.exists(key))
                .expectNext(true)
                .verifyComplete();

        webTestClient.get()
                .uri("/api/cache/{key}", key)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertEquals(value, body));

        webTestClient.delete()
                .uri("/api/cache/{key}", key)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("evicted successfully")));

        StepVerifier.create(cacheService.exists(key))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testCacheStatsIntegration() {
        StepVerifier.create(cacheService.getStats())
                .expectNextMatches(stats -> stats.contains("Cache Stats") &&
                        stats.contains("Hits") &&
                        stats.contains("Misses"))
                .verifyComplete();

        webTestClient.get()
                .uri("/api/cache/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("Cache Stats"));
                    assertTrue(body.contains("Hits") || body.contains("Misses"));
                });
    }
}
