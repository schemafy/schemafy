package com.schemafy.domain.erd.column.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class ColumnPersistenceAdapter implements
    CreateColumnPort,
    GetColumnByIdPort,
    GetColumnsByTableIdPort,
    ChangeColumnNamePort,
    ChangeColumnTypePort,
    ChangeColumnMetaPort,
    ChangeColumnPositionPort,
    DeleteColumnPort {

  private final ColumnRepository columnRepository;
  private final ColumnMapper columnMapper;

  ColumnPersistenceAdapter(ColumnRepository columnRepository, ColumnMapper columnMapper) {
    this.columnRepository = columnRepository;
    this.columnMapper = columnMapper;
  }

  @Override
  public Mono<Column> createColumn(Column column) {
    ColumnEntity entity = Objects.requireNonNull(columnMapper.toEntity(column));
    return columnRepository.save(entity)
        .map(columnMapper::toDomain);
  }

  @Override
  public Mono<Column> findColumnById(String columnId) {
    return columnRepository.findByIdAndDeletedAtIsNull(columnId)
        .map(columnMapper::toDomain);
  }

  @Override
  public Mono<List<Column>> findColumnsByTableId(String tableId) {
    return columnRepository.findByTableIdAndDeletedAtIsNullOrderBySeqNo(tableId)
        .map(columnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeColumnName(String columnId, String newName) {
    return findActiveColumnOrError(columnId)
        .flatMap((@NonNull ColumnEntity columnEntity) -> {
          columnEntity.setName(newName);
          return columnRepository.save(columnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeColumnType(
      String columnId,
      String dataType,
      ColumnLengthScale lengthScale) {
    return findActiveColumnOrError(columnId)
        .flatMap((@NonNull ColumnEntity columnEntity) -> {
          columnEntity.setDataType(dataType);
          columnEntity.setLengthScale(columnMapper.toLengthScaleJson(lengthScale));
          return columnRepository.save(columnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeColumnMeta(
      String columnId,
      Boolean autoIncrement,
      String charset,
      String collation,
      String comment) {
    return findActiveColumnOrError(columnId)
        .flatMap((@NonNull ColumnEntity columnEntity) -> {
          if (autoIncrement != null) {
            columnEntity.setAutoIncrement(autoIncrement);
          }
          if (charset != null) {
            columnEntity.setCharset(hasText(charset) ? charset : null);
          }
          if (collation != null) {
            columnEntity.setCollation(hasText(collation) ? collation : null);
          }
          if (comment != null) {
            columnEntity.setComment(hasText(comment) ? comment : null);
          }
          return columnRepository.save(columnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeColumnPosition(String columnId, int seqNo) {
    return findActiveColumnOrError(columnId)
        .flatMap((@NonNull ColumnEntity columnEntity) -> {
          columnEntity.setSeqNo(seqNo);
          return columnRepository.save(columnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteColumn(String columnId) {
    return findActiveColumnOrError(columnId)
        .flatMap((@NonNull ColumnEntity columnEntity) -> {
          columnEntity.setDeletedAt(Instant.now());
          return columnRepository.save(columnEntity);
        })
        .then();
  }

  private Mono<ColumnEntity> findActiveColumnOrError(String columnId) {
    return columnRepository.findByIdAndDeletedAtIsNull(columnId)
        .switchIfEmpty(Mono.error(new RuntimeException("Column not found")));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
