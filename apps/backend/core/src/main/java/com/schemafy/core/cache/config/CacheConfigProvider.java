package com.schemafy.core.cache.config;

import org.springframework.cache.CacheManager;

public interface CacheConfigProvider {

    CacheManager createCacheManager();

    String getCacheType();
}
