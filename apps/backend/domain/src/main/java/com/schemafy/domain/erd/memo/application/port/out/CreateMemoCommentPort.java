package com.schemafy.domain.erd.memo.application.port.out;

import com.schemafy.domain.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface CreateMemoCommentPort {

  Mono<MemoComment> createMemoComment(MemoComment comment);

}
