package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.Index;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetIndexesByTableIdService implements GetIndexesByTableIdUseCase {

  private final GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Override
  public Mono<List<Index>> getIndexesByTableId(GetIndexesByTableIdQuery query) {
    return getIndexesByTableIdPort.findIndexesByTableId(query.tableId());
  }

}
