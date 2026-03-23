package com.schemafy.api.erd.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.user.controller.dto.response.UserSummaryResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoDetailResponse {

  private String id;
  private String schemaId;
  private UserSummaryResponse author;
  private JsonNode positions;
  private Instant createdAt;
  private Instant updatedAt;
  private List<MemoCommentResponse> comments;

}
