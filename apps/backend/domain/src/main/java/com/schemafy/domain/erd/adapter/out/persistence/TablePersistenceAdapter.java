package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.domain.Table;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class TablePersistenceAdapter implements
    CreateTablePort,
    TableExistsPort,
    GetTableByIdPort,
    ChangeTableNamePort,
    DeleteTablePort {

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

  @Override
  public Mono<Table> findTableById(String tableId) {
    return tableRepository.findByIdAndDeletedAtIsNull(tableId)
        .map(tableMapper::toDomain);
  }

  @Override
  public Mono<Void> changeTableName(String tableId, String newName) {
    return tableRepository.findByIdAndDeletedAtIsNull(tableId)
        .flatMap(tableEntity -> {
          tableEntity.setName(newName);
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteTable(String tableId) {
    return tableRepository.findByIdAndDeletedAtIsNull(tableId)
        .flatMap(tableEntity -> {
          tableEntity.setDeletedAt(Instant.now());
          return tableRepository.save(tableEntity);
        })
        .then();
  }

}
