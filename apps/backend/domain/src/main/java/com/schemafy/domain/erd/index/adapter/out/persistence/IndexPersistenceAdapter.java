package com.schemafy.domain.erd.index.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.type.IndexType;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class IndexPersistenceAdapter implements
    CreateIndexPort,
    GetIndexByIdPort,
    GetIndexesByTableIdPort,
    ChangeIndexNamePort,
    ChangeIndexTypePort,
    DeleteIndexPort,
    IndexExistsPort {

  private final IndexRepository indexRepository;
  private final IndexMapper indexMapper;

  IndexPersistenceAdapter(IndexRepository indexRepository, IndexMapper indexMapper) {
    this.indexRepository = indexRepository;
    this.indexMapper = indexMapper;
  }

  @Override
  public Mono<Index> createIndex(Index index) {
    IndexEntity entity = Objects.requireNonNull(indexMapper.toEntity(index));
    return indexRepository.save(entity)
        .map(indexMapper::toDomain);
  }

  @Override
  public Mono<Index> findIndexById(String indexId) {
    return indexRepository.findByIdAndDeletedAtIsNull(indexId)
        .map(indexMapper::toDomain);
  }

  @Override
  public Mono<List<Index>> findIndexesByTableId(String tableId) {
    return indexRepository.findByTableIdAndDeletedAtIsNull(tableId)
        .map(indexMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeIndexName(String indexId, String newName) {
    return findActiveIndexOrError(indexId)
        .flatMap((@NonNull IndexEntity indexEntity) -> {
          indexEntity.setName(newName);
          return indexRepository.save(indexEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeIndexType(String indexId, IndexType type) {
    return findActiveIndexOrError(indexId)
        .flatMap((@NonNull IndexEntity indexEntity) -> {
          indexEntity.setType(type.name());
          return indexRepository.save(indexEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteIndex(String indexId) {
    return findActiveIndexOrError(indexId)
        .flatMap((@NonNull IndexEntity indexEntity) -> {
          indexEntity.setDeletedAt(Instant.now());
          return indexRepository.save(indexEntity);
        })
        .then();
  }

  @Override
  public Mono<Boolean> existsByTableIdAndName(String tableId, String name) {
    return indexRepository.existsByTableIdAndName(tableId, name);
  }

  @Override
  public Mono<Boolean> existsByTableIdAndNameExcludingId(
      String tableId,
      String name,
      String indexId) {
    return indexRepository.existsByTableIdAndNameExcludingId(tableId, name, indexId);
  }

  private Mono<IndexEntity> findActiveIndexOrError(String indexId) {
    return indexRepository.findByIdAndDeletedAtIsNull(indexId)
        .switchIfEmpty(Mono.error(new RuntimeException("Index not found")));
  }

}
