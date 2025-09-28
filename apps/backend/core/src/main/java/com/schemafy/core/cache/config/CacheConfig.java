package com.schemafy.core.cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    private final CacheProviderSelector cacheProviderSelector;

    public CacheConfig(CacheProviderSelector cacheProviderSelector) {
        this.cacheProviderSelector = cacheProviderSelector;
    }

    @Bean
    public CacheManager cacheManager() {
        return cacheProviderSelector.getSelectedProvider().createCacheManager();
    }
}
