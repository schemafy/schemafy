package com.schemafy.core.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeColumnMetaPort {

  Mono<Void> changeColumnMeta(
      String columnId,
      Boolean autoIncrement,
      String charset,
      String collation,
      String comment);

}
