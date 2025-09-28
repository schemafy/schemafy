package com.schemafy.core.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CacheProviderSelectorTest {

    @Autowired
    private CacheProviderSelector cacheProviderSelector;

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
        CacheConfigProvider provider = cacheProviderSelector.getSelectedProvider();

        assertNotNull(provider);
        assertEquals("caffeine", provider.getCacheType());
    }

    @Test
    void testGetSelectedProviderWithInvalidType() {
        assertNotNull(cacheProviderSelector.getSelectedProvider());
    }
}
