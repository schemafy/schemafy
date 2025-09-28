package com.schemafy.core.cache.controller;

import com.schemafy.core.cache.service.CacheService;
import com.schemafy.core.cache.config.CacheProviderSelector;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cache")
public class CacheTestController {

    private final CacheService cacheService;
    private final CacheProviderSelector cacheProviderSelector;

    public CacheTestController(CacheService cacheService, CacheProviderSelector cacheProviderSelector) {
        this.cacheService = cacheService;
        this.cacheProviderSelector = cacheProviderSelector;
    }

    @PostMapping("/{key}")
    public Mono<String> put(@PathVariable String key, @RequestBody String value) {
        return cacheService.put(key, value, java.time.Duration.ofMinutes(30))
                .thenReturn("Value stored successfully");
    }

    @GetMapping("/{key}")
    public Mono<String> get(@PathVariable String key) {
        return cacheService.get(key)
                .switchIfEmpty(Mono.just("Key not found"));
    }

    @DeleteMapping("/{key}")
    public Mono<String> evict(@PathVariable String key) {
        return cacheService.evict(key)
                .thenReturn("Key evicted successfully");
    }

    @GetMapping("/{key}/exists")
    public Mono<Boolean> exists(@PathVariable String key) {
        return cacheService.exists(key);
    }

    @GetMapping("/stats")
    public Mono<String> getStats() {
        return cacheService.getStats();
    }

    @GetMapping("/provider")
    public Mono<String> getProviderInfo() {
        return Mono.just(String.format(
                "Current Cache Provider: %s\nAvailable Providers: %s\n",
                cacheProviderSelector.getCurrentCacheType(),
                cacheProviderSelector.getAvailableProviders()));
    }
}
