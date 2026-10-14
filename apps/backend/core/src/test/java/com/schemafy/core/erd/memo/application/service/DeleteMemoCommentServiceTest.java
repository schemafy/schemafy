package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoCommentPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteMemoCommentService")
class DeleteMemoCommentServiceTest {

  @Mock
  GetMemoCommentByIdPort getMemoCommentByIdPort;

  @Mock
  GetMemoByIdPort getMemoByIdPort;

  @Mock
  GetMemoCommentsByMemoIdPort getMemoCommentsByMemoIdPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  SoftDeleteMemoCommentPort softDeleteMemoCommentPort;

  @Mock
  SoftDeleteMemoPort softDeleteMemoPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  DeleteMemoCommentService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("마지막 댓글 삭제 요청이면 메모만 soft delete 한다")
  void deleteComment_lastComment_softDeleteMemoOnly() {
    Memo memo = new Memo(
        "memo-1", "schema-1", "author-1", "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment comment = new MemoComment(
        "comment-1", "memo-1", "author-1", "body",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(comment));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId("memo-1"))
        .willReturn(Flux.just(comment));
    given(softDeleteMemoPort.softDeleteMemo(any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemoComment(new DeleteMemoCommentCommand(
        "comment-1",
        "author-1")))
        .verifyComplete();

    then(softDeleteMemoPort).should().softDeleteMemo(any(), any());
    then(softDeleteMemoCommentPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("댓글이 더 남아 있으면 댓글만 soft delete 한다")
  void deleteComment_notLast_softDeleteCommentOnly() {
    Memo memo = new Memo(
        "memo-1", "schema-1", "author-1", "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment target = new MemoComment(
        "comment-2", "memo-1", "author-2", "reply",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment first = new MemoComment(
        "comment-1", "memo-1", "author-1", "body",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-2"))
        .willReturn(Mono.just(target));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId("memo-1"))
        .willReturn(Flux.fromIterable(List.of(first, target)));
    given(softDeleteMemoCommentPort.softDeleteMemoComment(any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemoComment(new DeleteMemoCommentCommand(
        "comment-2",
        "author-2")))
        .verifyComplete();

    then(softDeleteMemoCommentPort).should().softDeleteMemoComment(any(),
        any());
    then(softDeleteMemoPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("작성자가 아니고 삭제 권한도 없으면 ACCESS_DENIED를 반환한다")
  void deleteComment_accessDenied() {
    MemoComment comment = new MemoComment(
        "comment-1", "memo-1", "author-1", "body",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    Memo memo = new Memo(
        "memo-1", "schema-1", "memo-author", "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(comment));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(getSchemaByIdPort.findSchemaById("schema-1"))
        .willReturn(Mono.just(new Schema(
            "schema-1",
            "project-1",
            "schema",
            "utf8mb4",
            "utf8mb4_general_ci")));
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted("project-1", "other-user"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemoComment(new DeleteMemoCommentCommand(
        "comment-1",
        "other-user")))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("댓글은 존재하지만 부모 메모가 없으면 NOT_FOUND를 반환한다")
  void deleteComment_parentMemoNotFound() {
    MemoComment comment = new MemoComment(
        "comment-1", "memo-1", "author-1", "body",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(comment));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemoComment(new DeleteMemoCommentCommand(
        "comment-1",
        "author-1")))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.NOT_FOUND))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 ADMIN은 다른 사용자의 댓글을 삭제할 수 있다")
  void deleteComment_projectAdminDeletesOthersComment() {
    Memo memo = new Memo(
        "memo-1", "schema-1", "memo-author", "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment target = new MemoComment(
        "comment-2", "memo-1", "author-2", "reply",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment first = new MemoComment(
        "comment-1", "memo-1", "author-1", "body",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    Schema schema = new Schema(
        "schema-1",
        "project-1",
        "schema",
        "utf8mb4",
        "utf8mb4_general_ci");
    ProjectMember adminMember = ProjectMember.create(
        "member-1",
        "project-1",
        "admin-1",
        ProjectRole.ADMIN);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-2"))
        .willReturn(Mono.just(target));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(getSchemaByIdPort.findSchemaById("schema-1"))
        .willReturn(Mono.just(schema));
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted("project-1", "admin-1"))
        .willReturn(Mono.just(adminMember));
    given(getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId("memo-1"))
        .willReturn(Flux.fromIterable(List.of(first, target)));
    given(softDeleteMemoCommentPort.softDeleteMemoComment(any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemoComment(new DeleteMemoCommentCommand(
        "comment-2",
        "admin-1")))
        .verifyComplete();

    then(softDeleteMemoCommentPort).should().softDeleteMemoComment(any(), any());
  }

}
