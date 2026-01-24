package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.domain.erd.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.application.port.out.DeleteIndexColumnPort;
import com.schemafy.domain.erd.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.domain.IndexColumn;
import com.schemafy.domain.erd.domain.type.SortDirection;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class IndexColumnPersistenceAdapter implements
    ChangeIndexColumnPositionPort,
    ChangeIndexColumnSortDirectionPort,
    CreateIndexColumnPort,
    GetIndexColumnByIdPort,
    GetIndexColumnsByIndexIdPort,
    DeleteIndexColumnPort {

  private final IndexColumnRepository indexColumnRepository;
  private final IndexColumnMapper indexColumnMapper;

  IndexColumnPersistenceAdapter(
      IndexColumnRepository indexColumnRepository,
      IndexColumnMapper indexColumnMapper) {
    this.indexColumnRepository = indexColumnRepository;
    this.indexColumnMapper = indexColumnMapper;
  }

  @Override
  public Mono<IndexColumn> createIndexColumn(IndexColumn indexColumn) {
    IndexColumnEntity entity = Objects.requireNonNull(indexColumnMapper.toEntity(indexColumn));
    return indexColumnRepository.save(entity)
        .map(indexColumnMapper::toDomain);
  }

  @Override
  public Mono<IndexColumn> findIndexColumnById(String indexColumnId) {
    return indexColumnRepository.findByIdAndDeletedAtIsNull(indexColumnId)
        .map(indexColumnMapper::toDomain);
  }

  @Override
  public Mono<List<IndexColumn>> findIndexColumnsByIndexId(String indexId) {
    return indexColumnRepository.findByIndexIdAndDeletedAtIsNullOrderBySeqNo(indexId)
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
    return indexColumnRepository.findByIndexIdAndDeletedAtIsNullOrderBySeqNo(indexId)
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
    return findActiveIndexColumnOrError(indexColumnId)
        .flatMap((@NonNull IndexColumnEntity indexColumnEntity) -> {
          indexColumnEntity.setSortDirection(sortDirection.name());
          return indexColumnRepository.save(indexColumnEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteIndexColumn(String indexColumnId) {
    return findActiveIndexColumnOrError(indexColumnId)
        .flatMap((@NonNull IndexColumnEntity indexColumnEntity) -> {
          indexColumnEntity.setDeletedAt(Instant.now());
          return indexColumnRepository.save(indexColumnEntity);
        })
        .then();
  }

  private Mono<IndexColumnEntity> findActiveIndexColumnOrError(String indexColumnId) {
    return indexColumnRepository.findByIdAndDeletedAtIsNull(indexColumnId)
        .switchIfEmpty(Mono.error(new RuntimeException("Index column not found")));
  }
}
