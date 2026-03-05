package com.schemafy.domain.erd.memo.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface MemoCommentRepository
    extends ReactiveCrudRepository<MemoCommentEntity, String> {

  Mono<MemoCommentEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<MemoCommentEntity> findByMemoIdAndDeletedAtIsNullOrderByIdAsc(
      String memoId);

}
