package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface UpdateMemoCommentUseCase {

  Mono<MemoComment> updateMemoComment(UpdateMemoCommentCommand command);

}
