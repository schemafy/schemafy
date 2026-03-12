package com.schemafy.api.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.api.erd.controller.dto.response.MemoResponse;
import com.schemafy.api.user.controller.dto.response.UserSummaryResponse;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;

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
