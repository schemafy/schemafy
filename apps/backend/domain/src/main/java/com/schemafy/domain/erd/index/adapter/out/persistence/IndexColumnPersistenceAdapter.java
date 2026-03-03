package com.schemafy.domain.erd.index.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.domain.erd.index.domain.type.SortDirection;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class IndexColumnPersistenceAdapter implements
    ChangeIndexColumnPositionPort,
    ChangeIndexColumnSortDirectionPort,
    CreateIndexColumnPort,
    GetIndexColumnByIdPort,
    GetIndexColumnsByColumnIdPort,
    GetIndexColumnsByIndexIdPort,
    DeleteIndexColumnPort,
    DeleteIndexColumnsByIndexIdPort,
    DeleteIndexColumnsByColumnIdPort {

  private final IndexColumnRepository indexColumnRepository;
  private final IndexColumnMapper indexColumnMapper;

  @Override
  public Mono<IndexColumn> createIndexColumn(IndexColumn indexColumn) {
    IndexColumnEntity entity = Objects.requireNonNull(indexColumnMapper.toEntity(indexColumn));
    return indexColumnRepository.save(entity)
        .map(indexColumnMapper::toDomain);
  }

  @Override
  public Mono<IndexColumn> findIndexColumnById(String indexColumnId) {
    return indexColumnRepository.findById(indexColumnId)
        .map(indexColumnMapper::toDomain);
  }

  @Override
  public Mono<List<IndexColumn>> findIndexColumnsByIndexId(String indexId) {
    return indexColumnRepository.findByIndexIdOrderBySeqNo(indexId)
        .map(indexColumnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<List<IndexColumn>> findIndexColumnsByColumnId(String columnId) {
    return indexColumnRepository.findByColumnId(columnId)
        .map(indexColumnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeIndexColumnPositions(String indexId, List<IndexColumn> columns) {
    if (columns == null || columns.isEmpty()) {
      return Mono.empty();
    }
    Map<String, Integer> positions = new HashMap<>(columns.size());
    for (IndexColumn column : columns) {
      positions.put(column.id(), column.seqNo());
    }
    return indexColumnRepository.findByIndexIdOrderBySeqNo(indexId)
        .map(entity -> {
          Integer seqNo = positions.get(entity.getId());
          if (seqNo != null) {
            entity.setSeqNo(seqNo);
          }
          return entity;
        })
        .collectList()
        .flatMap(entities -> indexColumnRepository.saveAll(entities).then());
  }

  @Override
  public Mono<Void> changeIndexColumnSortDirection(
      String indexColumnId,
      SortDirection sortDirection) {
    return findIndexColumnOrError(indexColumnId)
        .flatMap((@NonNull IndexColumnEntity indexColumnEntity) -> {
          indexColumnEntity.setSortDirection(sortDirection.name());
          return indexColumnRepository.save(indexColumnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteIndexColumn(String indexColumnId) {
    return indexColumnRepository.deleteById(indexColumnId);
  }

  @Override
  public Mono<Void> deleteByIndexId(String indexId) {
    return indexColumnRepository.deleteByIndexId(indexId);
  }

  @Override
  public Mono<Void> deleteByColumnId(String columnId) {
    return indexColumnRepository.deleteByColumnId(columnId);
  }

  private Mono<IndexColumnEntity> findIndexColumnOrError(String indexColumnId) {
    return indexColumnRepository.findById(indexColumnId)
        .switchIfEmpty(Mono.error(
            new DomainException(IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found: " + indexColumnId)));
  }

}
