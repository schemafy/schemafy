package com.schemafy.api.cache.service.dto;

public record CacheStatsDto(
    long hits,
    long misses,
    double hitRate,
    long size) {
}
