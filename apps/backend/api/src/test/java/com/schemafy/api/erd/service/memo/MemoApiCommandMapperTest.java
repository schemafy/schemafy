package com.schemafy.api.erd.service.memo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.api.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemoApiCommandMapper")
class MemoApiCommandMapperTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private final MemoApiCommandMapper sut = new MemoApiCommandMapper();

  @Test
  @DisplayName("createMemo: request/user를 도메인 커맨드로 매핑한다")
  void createMemoMapping() throws Exception {
    var user = AuthenticatedUser.of("user-1");
    var request = new CreateMemoRequest(
        "schema-1",
        objectMapper.readTree("{\"x\":0,\"y\":0}"),
        "body");

    var command = sut.toCreateMemoCommand(request, user);

    assertThat(command.schemaId()).isEqualTo("schema-1");
    assertThat(command.positions()).isEqualTo(request.positions());
    assertThat(command.body()).isEqualTo("body");
    assertThat(command.authorId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("deleteMemo: requester를 도메인 커맨드로 전달한다")
  void deleteMemoMapping() {
    var user = AuthenticatedUser.of("user-1");

    var command = sut.toDeleteMemoCommand("memo-1", user);

    assertThat(command.memoId()).isEqualTo("memo-1");
    assertThat(command.requesterId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("deleteMemoComment: requester를 도메인 커맨드로 전달한다")
  void deleteMemoCommentMapping() {
    var user = AuthenticatedUser.of("user-1");

    var command = sut.toDeleteMemoCommentCommand("comment-1",
        user);

    assertThat(command.commentId()).isEqualTo("comment-1");
    assertThat(command.requesterId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("update 커맨드들도 필드를 그대로 매핑한다")
  void updateMappings() throws Exception {
    var user = AuthenticatedUser.of("user-1");

    var memoCommand = sut.toUpdateMemoPositionCommand(
        "memo-1",
        new UpdateMemoRequest(objectMapper.readTree("{\"x\":10,\"y\":0}")),
        user);
    var commentCommand = sut.toUpdateMemoCommentCommand(
        "comment-1",
        new UpdateMemoCommentRequest("updated"),
        user);
    var createCommentCommand = sut.toCreateMemoCommentCommand(
        "memo-1",
        new CreateMemoCommentRequest("new"),
        user);

    assertThat(memoCommand.memoId()).isEqualTo("memo-1");
    assertThat(memoCommand.positions()).isEqualTo(
        objectMapper.readTree("{\"x\":10,\"y\":0}"));
    assertThat(commentCommand.commentId()).isEqualTo("comment-1");
    assertThat(commentCommand.body()).isEqualTo("updated");
    assertThat(createCommentCommand.body()).isEqualTo("new");
  }

}
