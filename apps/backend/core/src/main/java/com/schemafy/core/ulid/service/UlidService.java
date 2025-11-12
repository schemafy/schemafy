package com.schemafy.core.ulid.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.cache.service.CacheService;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UlidService {

    private final CacheService cacheService;

    private static final String ULID_PREFIX = "ulid::";

    public Mono<String> generateTemporaryUlid() {
        return Mono.fromCallable(UlidGenerator::generate)
                .flatMap(ulid -> cacheService.put(ULID_PREFIX + ulid, "valid")
                        .thenReturn(ulid));
    }

}
