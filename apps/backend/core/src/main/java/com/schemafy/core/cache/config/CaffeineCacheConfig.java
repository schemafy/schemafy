package com.schemafy.core.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CaffeineCacheConfig implements CacheConfigProvider {

    @Override
    public CacheManager createCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats());
        return cacheManager;
    }

    @Override
    public String getCacheType() {
        return "caffeine";
    }
}
