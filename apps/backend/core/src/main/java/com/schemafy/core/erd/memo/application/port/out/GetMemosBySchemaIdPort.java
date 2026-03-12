package com.schemafy.core.erd.memo.application.port.out;

import com.schemafy.core.erd.memo.domain.Memo;

import reactor.core.publisher.Flux;

public interface GetMemosBySchemaIdPort {

  Flux<Memo> findMemosBySchemaId(String schemaId);

}
