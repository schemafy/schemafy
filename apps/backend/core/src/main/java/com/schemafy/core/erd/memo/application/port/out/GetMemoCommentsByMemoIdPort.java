package com.schemafy.core.erd.memo.application.port.out;

import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Flux;

public interface GetMemoCommentsByMemoIdPort {

  Flux<MemoComment> findMemoCommentsByMemoId(String memoId);

}
