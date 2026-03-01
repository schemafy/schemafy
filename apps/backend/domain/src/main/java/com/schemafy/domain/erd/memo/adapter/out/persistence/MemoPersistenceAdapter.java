package com.schemafy.domain.erd.memo.adapter.out.persistence;

import java.time.Instant;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.out.ChangeMemoPositionPort;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemosBySchemaIdPort;
import com.schemafy.domain.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class MemoPersistenceAdapter implements
    CreateMemoPort,
    GetMemoByIdPort,
    GetMemosBySchemaIdPort,
    ChangeMemoPositionPort,
    SoftDeleteMemoPort {

  private final MemoRepository memoRepository;
  private final MemoMapper memoMapper;

  @Override
  public Mono<Memo> createMemo(Memo memo) {
    return memoRepository.save(memoMapper.toEntity(memo))
        .map(memoMapper::toDomain);
  }

  @Override
  public Mono<Memo> findMemoById(String memoId) {
    return memoRepository.findByIdAndDeletedAtIsNull(memoId)
        .map(memoMapper::toDomain);
  }

  @Override
  public Flux<Memo> findMemosBySchemaId(String schemaId) {
    return memoRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
        .map(memoMapper::toDomain);
  }

  @Override
  public Mono<Void> changeMemoPosition(String memoId, String positions) {
    return findMemoOrError(memoId)
        .flatMap(memoEntity -> {
          memoEntity.setPositions(positions);
          return memoRepository.save(memoEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> softDeleteMemo(String memoId, Instant deletedAt) {
    return findMemoOrError(memoId)
        .flatMap(memoEntity -> {
          memoEntity.setDeletedAt(deletedAt);
          return memoRepository.save(memoEntity);
        })
        .then();
  }

  private Mono<MemoEntity> findMemoOrError(String memoId) {
    return memoRepository.findByIdAndDeletedAtIsNull(memoId)
        .switchIfEmpty(Mono.error(
            new DomainException(MemoErrorCode.NOT_FOUND, "Memo not found: " + memoId)));
  }

}
