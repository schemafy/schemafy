package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.Memo;

import reactor.core.publisher.Flux;

public interface GetMemosBySchemaIdUseCase {

  Flux<Memo> getMemosBySchemaId(GetMemosBySchemaIdQuery query);

}
