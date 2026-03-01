package com.schemafy.core.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.user.controller.dto.response.UserSummaryResponse;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.MemoComment;

@Component
public class MemoApiResponseMapper {

  public MemoResponse toMemoResponse(Memo memo, UserSummaryResponse author) {
    return new MemoResponse(
        memo.id(),
        memo.schemaId(),
        author,
        memo.positions(),
        memo.createdAt(),
        memo.updatedAt());
  }

  public MemoCommentResponse toMemoCommentResponse(
      MemoComment comment,
      UserSummaryResponse author) {
    return new MemoCommentResponse(
        comment.id(),
        comment.memoId(),
        author,
        comment.body(),
        comment.createdAt(),
        comment.updatedAt());
  }

}
