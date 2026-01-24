package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.type.SortDirection;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnSortDirectionPort {

  Mono<Void> changeIndexColumnSortDirection(String indexColumnId, SortDirection sortDirection);

}
