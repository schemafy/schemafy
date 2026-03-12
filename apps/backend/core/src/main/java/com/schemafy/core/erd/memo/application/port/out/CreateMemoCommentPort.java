package com.schemafy.core.erd.memo.application.port.out;

import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface CreateMemoCommentPort {

  Mono<MemoComment> createMemoComment(MemoComment comment);

}
