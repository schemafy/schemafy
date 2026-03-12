package com.schemafy.core.erd.index.application.port.in;

import java.util.List;

import com.schemafy.core.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexesByTableIdUseCase {

  Mono<List<Index>> getIndexesByTableId(GetIndexesByTableIdQuery query);

}
