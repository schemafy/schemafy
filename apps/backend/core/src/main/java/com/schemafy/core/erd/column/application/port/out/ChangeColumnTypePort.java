package com.schemafy.core.erd.column.application.port.out;

import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

import reactor.core.publisher.Mono;

public interface ChangeColumnTypePort {

  Mono<Void> changeColumnType(String columnId, String dataType, ColumnTypeArguments typeArguments);

}
