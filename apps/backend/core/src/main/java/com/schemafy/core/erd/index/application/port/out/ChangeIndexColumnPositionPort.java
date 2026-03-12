package com.schemafy.core.erd.index.application.port.out;

import java.util.List;

import com.schemafy.core.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnPositionPort {

  Mono<Void> changeIndexColumnPositions(String indexId, List<IndexColumn> columns);

}
