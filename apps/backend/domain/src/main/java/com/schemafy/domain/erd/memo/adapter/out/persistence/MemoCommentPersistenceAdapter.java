package com.schemafy.domain.erd.memo.adapter.out.persistence;

import java.time.Instant;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.out.ChangeMemoCommentBodyPort;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoCommentPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.domain.erd.memo.application.port.out.SoftDeleteMemoCommentPort;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class MemoCommentPersistenceAdapter implements
    CreateMemoCommentPort,
    GetMemoCommentByIdPort,
    GetMemoCommentsByMemoIdPort,
    ChangeMemoCommentBodyPort,
    SoftDeleteMemoCommentPort {

  private final MemoCommentRepository memoCommentRepository;
  private final MemoCommentMapper memoCommentMapper;

  @Override
  public Mono<MemoComment> createMemoComment(MemoComment comment) {
    return memoCommentRepository.save(memoCommentMapper.toEntity(comment))
        .map(memoCommentMapper::toDomain);
  }

  @Override
  public Mono<MemoComment> findMemoCommentById(String commentId) {
    return memoCommentRepository.findByIdAndDeletedAtIsNull(commentId)
        .map(memoCommentMapper::toDomain);
  }

  @Override
  public Flux<MemoComment> findMemoCommentsByMemoId(String memoId) {
    return memoCommentRepository.findByMemoIdAndDeletedAtIsNullOrderByIdAsc(
        memoId)
        .map(memoCommentMapper::toDomain);
  }

  @Override
  public Mono<Void> changeMemoCommentBody(String commentId, String body) {
    return findMemoCommentOrError(commentId)
        .flatMap(commentEntity -> {
          commentEntity.setBody(body);
          return memoCommentRepository.save(commentEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> softDeleteMemoComment(String commentId, Instant deletedAt) {
    return findMemoCommentOrError(commentId)
        .flatMap(commentEntity -> {
          commentEntity.setDeletedAt(deletedAt);
          return memoCommentRepository.save(commentEntity);
        })
        .then();
  }

  private Mono<MemoCommentEntity> findMemoCommentOrError(String commentId) {
    return memoCommentRepository.findByIdAndDeletedAtIsNull(commentId)
        .switchIfEmpty(Mono.error(new DomainException(
            MemoErrorCode.COMMENT_NOT_FOUND,
            "Memo comment not found: " + commentId)));
  }

}
