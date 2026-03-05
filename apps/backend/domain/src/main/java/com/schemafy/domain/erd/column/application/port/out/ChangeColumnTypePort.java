package com.schemafy.domain.erd.column.application.port.out;

import com.schemafy.domain.erd.column.domain.ColumnTypeArguments;

import reactor.core.publisher.Mono;

public interface ChangeColumnTypePort {

  Mono<Void> changeColumnType(String columnId, String dataType, ColumnTypeArguments typeArguments);

}
