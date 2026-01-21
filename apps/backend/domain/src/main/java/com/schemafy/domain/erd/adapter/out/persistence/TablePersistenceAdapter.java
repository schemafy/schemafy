package com.schemafy.domain.erd.adapter.out.persistence;

import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.domain.Table;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class TablePersistenceAdapter implements CreateTablePort, TableExistsPort {

  private final TableRepository tableRepository;
  private final TableMapper tableMapper;

  @Override
  public Mono<Table> createTable(Table table) {
    TableEntity entity = Objects.requireNonNull(tableMapper.toEntity(table));
    return tableRepository.save(entity)
        .map(tableMapper::toDomain);
  }

  @Override
  public Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name) {
    return tableRepository.existsBySchemaIdAndName(schemaId, name);
  }

}
