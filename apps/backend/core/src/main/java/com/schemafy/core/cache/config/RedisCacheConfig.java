package com.schemafy.core.cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheConfig implements CacheConfigProvider {

    @Override
    public CacheManager createCacheManager() {
        throw new UnsupportedOperationException("Redis cache not implemented yet. Add Redis dependency first.");
    }

    @Override
    public String getCacheType() {
        return "redis";
    }
}
