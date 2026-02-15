package com.schemafy.domain.erd.index.application.port.out;

import com.schemafy.domain.erd.index.domain.type.SortDirection;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnSortDirectionPort {

  Mono<Void> changeIndexColumnSortDirection(String indexColumnId, SortDirection sortDirection);

}
