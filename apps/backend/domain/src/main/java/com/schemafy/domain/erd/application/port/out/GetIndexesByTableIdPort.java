package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexesByTableIdPort {

  Mono<List<Index>> findIndexesByTableId(String tableId);

}
