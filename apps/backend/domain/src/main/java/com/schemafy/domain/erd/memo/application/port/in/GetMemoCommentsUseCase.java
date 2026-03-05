package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.MemoComment;

import reactor.core.publisher.Flux;

public interface GetMemoCommentsUseCase {

  Flux<MemoComment> getMemoComments(GetMemoCommentsQuery query);

}
