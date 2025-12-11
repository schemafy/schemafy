package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.MemoComment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MemoCommentRepository
        extends ReactiveCrudRepository<MemoComment, String> {

    Mono<MemoComment> findByIdAndDeletedAtIsNull(String id);

    Flux<MemoComment> findByMemoIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
            String memoId);

}
