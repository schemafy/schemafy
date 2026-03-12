package com.schemafy.core.erd.index.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetIndexColumnsByIndexIdService implements GetIndexColumnsByIndexIdUseCase {

  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Override
  public Mono<List<IndexColumn>> getIndexColumnsByIndexId(GetIndexColumnsByIndexIdQuery query) {
    return getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(query.indexId());
  }

}
