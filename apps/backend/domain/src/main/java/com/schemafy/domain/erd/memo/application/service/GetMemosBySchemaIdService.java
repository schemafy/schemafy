package com.schemafy.domain.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.memo.application.port.in.GetMemosBySchemaIdQuery;
import com.schemafy.domain.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.domain.erd.memo.application.port.out.GetMemosBySchemaIdPort;
import com.schemafy.domain.erd.memo.domain.Memo;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
class GetMemosBySchemaIdService implements GetMemosBySchemaIdUseCase {

  private final GetMemosBySchemaIdPort getMemosBySchemaIdPort;

  @Override
  public Flux<Memo> getMemosBySchemaId(GetMemosBySchemaIdQuery query) {
    return getMemosBySchemaIdPort.findMemosBySchemaId(query.schemaId());
  }

}
