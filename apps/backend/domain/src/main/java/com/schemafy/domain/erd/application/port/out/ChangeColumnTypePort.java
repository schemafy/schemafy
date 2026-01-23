package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.ColumnLengthScale;

import reactor.core.publisher.Mono;

public interface ChangeColumnTypePort {

  Mono<Void> changeColumnType(String columnId, String dataType, ColumnLengthScale lengthScale);

}
