package com.schemafy.core.erd.index.application.port.out;

import java.util.List;

import com.schemafy.core.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexesByTableIdPort {

  Mono<List<Index>> findIndexesByTableId(String tableId);

}
