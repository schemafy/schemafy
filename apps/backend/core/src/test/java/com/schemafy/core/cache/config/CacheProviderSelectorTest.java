package com.schemafy.core.cache.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CacheProviderSelectorTest {

    private CacheProviderSelector cacheProviderSelector;
    private List<CacheConfigProvider> providers;

    @BeforeEach
    void setUp() {
        providers = List.of(
                new CaffeineCacheConfig(),
                new RedisCacheConfig());
        cacheProviderSelector = new CacheProviderSelector(providers, "caffeine");
    }

    @Test
    void testGetSelectedProvider() {
        CacheConfigProvider provider = cacheProviderSelector.getSelectedProvider();

        assertNotNull(provider);
        assertEquals("caffeine", provider.getCacheType());
    }

    @Test
    void testGetAvailableProviders() {
        List<String> availableProviders = cacheProviderSelector.getAvailableProviders();

        assertNotNull(availableProviders);
        assertTrue(availableProviders.contains("caffeine"));
        assertTrue(availableProviders.contains("redis"));
        assertEquals(2, availableProviders.size());
    }

    @Test
    void testGetCurrentCacheType() {
        String currentType = cacheProviderSelector.getCurrentCacheType();

        assertEquals("caffeine", currentType);
    }

    @Test
    void testGetSelectedProviderWithRedis() {
        CacheProviderSelector redisSelector = new CacheProviderSelector(providers, "redis");

        CacheConfigProvider provider = redisSelector.getSelectedProvider();

        assertNotNull(provider);
        assertEquals("redis", provider.getCacheType());
    }

    @Test
    void testGetSelectedProviderWithInvalidType() {
        CacheProviderSelector invalidSelector = new CacheProviderSelector(providers, "invalid");

        assertThrows(IllegalStateException.class, () -> {
            invalidSelector.getSelectedProvider();
        });
    }
}
