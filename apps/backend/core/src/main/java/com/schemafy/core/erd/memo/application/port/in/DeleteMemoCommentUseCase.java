package com.schemafy.core.erd.memo.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteMemoCommentUseCase {

  Mono<Void> deleteMemoComment(DeleteMemoCommentCommand command);

}
