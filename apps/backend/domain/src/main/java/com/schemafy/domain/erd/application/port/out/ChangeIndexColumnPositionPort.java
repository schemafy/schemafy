package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnPositionPort {

  Mono<Void> changeIndexColumnPositions(String indexId, List<IndexColumn> columns);

}
