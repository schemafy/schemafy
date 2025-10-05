package com.schemafy.core.cache.service.dto;

public record CacheStatsDto(
        long hits,
        long misses,
        double hitRate,
        long size) {
}
