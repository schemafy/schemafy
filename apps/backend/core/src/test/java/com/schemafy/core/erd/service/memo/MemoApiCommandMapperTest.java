package com.schemafy.core.erd.service.memo;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.project.repository.vo.ProjectRole;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemoApiCommandMapper")
class MemoApiCommandMapperTest {

  private final MemoApiCommandMapper sut = new MemoApiCommandMapper(
      new MemoDeletePermissionPolicy());

  @Test
  @DisplayName("createMemo: request/user를 도메인 커맨드로 매핑한다")
  void createMemoMapping() {
    var user = AuthenticatedUser.of("user-1");
    var request = new CreateMemoRequest("schema-1", "{}", "body");

    var command = sut.toCreateMemoCommand(request, user);

    assertThat(command.schemaId()).isEqualTo("schema-1");
    assertThat(command.positions()).isEqualTo("{}");
    assertThat(command.body()).isEqualTo("body");
    assertThat(command.authorId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("deleteMemo: OWNER/ADMIN은 canDeleteOthers=true")
  void deleteMemo_ownerCanDeleteOthers() {
    var owner = AuthenticatedUser.withRoles("owner-1",
        Set.of(ProjectRole.OWNER));

    var command = sut.toDeleteMemoCommand("memo-1", owner);

    assertThat(command.memoId()).isEqualTo("memo-1");
    assertThat(command.requesterId()).isEqualTo("owner-1");
    assertThat(command.canDeleteOthers()).isTrue();
  }

  @Test
  @DisplayName("deleteMemoComment: 일반 댓글 권한은 canDeleteOthers=false")
  void deleteMemoComment_commenterCannotDeleteOthers() {
    var commenter = AuthenticatedUser.withRoles("commenter-1",
        Set.of(ProjectRole.COMMENTER));

    var command = sut.toDeleteMemoCommentCommand("memo-1", "comment-1",
        commenter);

    assertThat(command.memoId()).isEqualTo("memo-1");
    assertThat(command.commentId()).isEqualTo("comment-1");
    assertThat(command.requesterId()).isEqualTo("commenter-1");
    assertThat(command.canDeleteOthers()).isFalse();
  }

  @Test
  @DisplayName("update 커맨드들도 필드를 그대로 매핑한다")
  void updateMappings() {
    var user = AuthenticatedUser.of("user-1");

    var memoCommand = sut.toUpdateMemoPositionCommand(
        "memo-1",
        new UpdateMemoRequest("{\"x\":10}"),
        user);
    var commentCommand = sut.toUpdateMemoCommentCommand(
        "memo-1",
        "comment-1",
        new UpdateMemoCommentRequest("updated"),
        user);
    var createCommentCommand = sut.toCreateMemoCommentCommand(
        "memo-1",
        new CreateMemoCommentRequest("new"),
        user);

    assertThat(memoCommand.memoId()).isEqualTo("memo-1");
    assertThat(memoCommand.positions()).isEqualTo("{\"x\":10}");
    assertThat(commentCommand.commentId()).isEqualTo("comment-1");
    assertThat(commentCommand.body()).isEqualTo("updated");
    assertThat(createCommentCommand.body()).isEqualTo("new");
  }

}
