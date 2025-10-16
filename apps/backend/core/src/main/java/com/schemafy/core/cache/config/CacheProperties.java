package com.schemafy.core.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private CacheType type = CacheType.CAFFEINE;
    private long maximumSize = 10000;
    private int expireAfterWriteMinutes = 30;
    private int expireAfterAccessMinutes = 10;
}
