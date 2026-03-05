package com.schemafy.domain.erd.memo.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteMemoCommentUseCase {

  Mono<Void> deleteMemoComment(DeleteMemoCommentCommand command);

}
