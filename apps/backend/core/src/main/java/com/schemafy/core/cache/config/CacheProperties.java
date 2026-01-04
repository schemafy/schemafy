package com.schemafy.core.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

  private CaffeineProperties caffeine = new CaffeineProperties();
  private RedisProperties redis = new RedisProperties();

  @Getter
  @Setter
  public static class CaffeineProperties {

    private boolean enabled = true;
    private long maximumSize = 10000;
    private int expireAfterWriteMinutes = 30;
    private int expireAfterAccessMinutes = 10;

  }

  @Getter
  @Setter
  public static class RedisProperties {

    private boolean enabled = true;
    private int defaultTtlMinutes = 30;
    private String keyPrefix = "cache::";

  }

}
