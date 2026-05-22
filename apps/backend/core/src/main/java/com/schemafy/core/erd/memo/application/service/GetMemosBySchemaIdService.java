package com.schemafy.core.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.core.erd.memo.application.port.out.GetMemosBySchemaIdPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
class GetMemosBySchemaIdService implements GetMemosBySchemaIdUseCase {

  private final GetMemosBySchemaIdPort getMemosBySchemaIdPort;

  @Override
  public Flux<Memo> getMemosBySchemaId(GetMemosBySchemaIdQuery query) {
    return getMemosBySchemaIdPort.findMemosBySchemaId(query.schemaId());
  }

}
