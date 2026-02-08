package com.schemafy.domain.erd.column.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface ChangeColumnPositionPort {

  Mono<Void> changeColumnPositions(String tableId, List<Column> columns);

}
