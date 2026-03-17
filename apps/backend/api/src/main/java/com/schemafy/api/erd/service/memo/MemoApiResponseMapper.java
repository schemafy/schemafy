package com.schemafy.api.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.api.erd.controller.dto.response.MemoResponse;
import com.schemafy.api.user.controller.dto.response.UserSummaryResponse;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemoApiResponseMapper {

  private final JsonCodec jsonCodec;

  public MemoResponse toMemoResponse(Memo memo, UserSummaryResponse author) {
    return new MemoResponse(
        memo.id(),
        memo.schemaId(),
        author,
        jsonCodec.parseNode(memo.positions()),
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
