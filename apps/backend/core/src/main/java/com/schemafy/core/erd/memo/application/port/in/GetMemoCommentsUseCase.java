package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Flux;

public interface GetMemoCommentsUseCase {

  Flux<MemoComment> getMemoComments(GetMemoCommentsQuery query);

}
