package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface CreateMemoCommentUseCase {

  Mono<MemoComment> createMemoComment(CreateMemoCommentCommand command);

}
