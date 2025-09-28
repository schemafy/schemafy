package com.schemafy.core.cache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CacheProviderSelector {

    private final Map<String, CacheConfigProvider> providers;
    private final String cacheType;

    public CacheProviderSelector(List<CacheConfigProvider> cacheConfigProviders,
            @Value("${cache.type:caffeine}") String cacheType) {
        this.providers = cacheConfigProviders.stream()
                .collect(Collectors.toMap(
                        CacheConfigProvider::getCacheType,
                        Function.identity()));
        this.cacheType = cacheType;
    }

    public CacheConfigProvider getSelectedProvider() {
        CacheConfigProvider provider = providers.get(cacheType);
        if (provider == null) {
            throw new IllegalStateException(
                    String.format("Cache provider for type '%s' not found. Available types: %s",
                            cacheType, providers.keySet()));
        }
        return provider;
    }

    public List<String> getAvailableProviders() {
        return providers.keySet().stream().sorted().toList();
    }

    public String getCurrentCacheType() {
        return cacheType;
    }
}
