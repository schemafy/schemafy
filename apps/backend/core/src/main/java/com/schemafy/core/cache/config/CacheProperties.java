package com.schemafy.core.cache.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private CacheType type = CacheType.CAFFEINE;
    private long maximumSize = 10000;
    private int expireAfterWriteMinutes = 30;
    private int expireAfterAccessMinutes = 10;
}
