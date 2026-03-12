package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface UpdateMemoCommentUseCase {

  Mono<MemoComment> updateMemoComment(UpdateMemoCommentCommand command);

}
