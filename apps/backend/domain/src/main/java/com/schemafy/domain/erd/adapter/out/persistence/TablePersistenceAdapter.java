package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.application.port.out.ChangeTableExtraPort;
import com.schemafy.domain.erd.application.port.out.ChangeTableMetaPort;
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
    ChangeTableExtraPort,
    ChangeTableMetaPort,
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
    return findActiveTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          tableEntity.setName(newName);
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeTableExtra(String tableId, String extra) {
    return findActiveTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          tableEntity.setExtra(hasText(extra) ? extra : null);
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeTableMeta(String tableId, String charset, String collation) {
    return findActiveTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          if (hasText(charset)) {
            tableEntity.setCharset(charset);
          }
          if (hasText(collation)) {
            tableEntity.setCollation(collation);
          }
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteTable(String tableId) {
    return findActiveTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          tableEntity.setDeletedAt(Instant.now());
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  private Mono<TableEntity> findActiveTableOrError(String tableId) {
    return tableRepository.findByIdAndDeletedAtIsNull(tableId)
        .switchIfEmpty(Mono.error(new RuntimeException("Table not found")));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
