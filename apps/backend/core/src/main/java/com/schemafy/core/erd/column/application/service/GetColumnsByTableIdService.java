package com.schemafy.core.erd.column.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetColumnsByTableIdService implements GetColumnsByTableIdUseCase {

  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Override
  public Mono<List<Column>> getColumnsByTableId(GetColumnsByTableIdQuery query) {
    return getColumnsByTableIdPort.findColumnsByTableId(query.tableId());
  }

}
