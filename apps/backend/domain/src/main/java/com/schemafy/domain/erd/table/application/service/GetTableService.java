package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetTableService {

  private final GetTableByIdPort getTableByIdPort;

  public Mono<Table> findTableById(String tableId) {
    return getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new RuntimeException("Table not found")));
  }

}
