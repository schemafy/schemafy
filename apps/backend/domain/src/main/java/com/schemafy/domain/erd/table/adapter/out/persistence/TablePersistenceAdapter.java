package com.schemafy.domain.erd.table.adapter.out.persistence;

import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.table.application.port.out.CascadeDeleteTablePort;
import com.schemafy.domain.erd.table.application.port.out.CascadeDeleteTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableExtraPort;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.table.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class TablePersistenceAdapter implements
    CreateTablePort,
    TableExistsPort,
    GetTableByIdPort,
    GetTablesBySchemaIdPort,
    ChangeTableNamePort,
    ChangeTableExtraPort,
    ChangeTableMetaPort,
    DeleteTablePort,
    CascadeDeleteTablesBySchemaIdPort,
    CascadeDeleteTablePort {

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
    return tableRepository.findById(tableId)
        .map(tableMapper::toDomain);
  }

  @Override
  public Flux<Table> findTablesBySchemaId(String schemaId) {
    return tableRepository.findBySchemaId(schemaId)
        .map(tableMapper::toDomain);
  }

  @Override
  public Mono<Void> changeTableName(String tableId, String newName) {
    return findTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          tableEntity.setName(newName);
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeTableExtra(String tableId, String extra) {
    return findTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          tableEntity.setExtra(hasText(extra) ? extra : null);
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeTableMeta(String tableId, String charset, String collation) {
    return findTableOrError(tableId)
        .flatMap((@NonNull TableEntity tableEntity) -> {
          if (charset != null) {
            tableEntity.setCharset(hasText(charset) ? charset : null);
          }
          if (collation != null) {
            tableEntity.setCollation(hasText(collation) ? collation : null);
          }
          return tableRepository.save(tableEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteTable(String tableId) {
    return tableRepository.deleteById(tableId);
  }

  @Override
  public Mono<Void> cascadeDeleteBySchemaId(String schemaId) {
    return tableRepository.deleteBySchemaId(schemaId);
  }

  @Override
  public Mono<Void> cascadeDelete(String tableId) {
    return tableRepository.deleteById(tableId);
  }

  private Mono<TableEntity> findTableOrError(String tableId) {
    return tableRepository.findById(tableId)
        .switchIfEmpty(Mono.error(new TableNotExistException("Table not found: " + tableId)));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
