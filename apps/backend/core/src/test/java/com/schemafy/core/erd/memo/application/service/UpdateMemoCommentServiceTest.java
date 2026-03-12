package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.out.ChangeMemoCommentBodyPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMemoCommentService")
class UpdateMemoCommentServiceTest {

  @Mock
  GetMemoCommentByIdPort getMemoCommentByIdPort;

  @Mock
  GetMemoByIdPort getMemoByIdPort;

  @Mock
  ChangeMemoCommentBodyPort changeMemoCommentBodyPort;

  @InjectMocks
  UpdateMemoCommentService sut;

  @Test
  @DisplayName("작성자 본인은 댓글 본문을 수정할 수 있다")
  void updateComment_success() {
    Memo memo = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment original = new MemoComment(
        "comment-1",
        "memo-1",
        "author-1",
        "old",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment updated = new MemoComment(
        "comment-1",
        "memo-1",
        "author-1",
        "new",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:01Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(original), Mono.just(updated));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(changeMemoCommentBodyPort.changeMemoCommentBody("comment-1", "new"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.updateMemoComment(new UpdateMemoCommentCommand(
        "comment-1",
        "new",
        "author-1")))
        .assertNext(result -> assertThat(result.body()).isEqualTo("new"))
        .verifyComplete();
  }

  @Test
  @DisplayName("작성자가 아니면 ACCESS_DENIED를 반환한다")
  void updateComment_accessDenied() {
    Memo memo = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    MemoComment comment = new MemoComment(
        "comment-1",
        "memo-1",
        "author-1",
        "old",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(comment));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));

    StepVerifier.create(sut.updateMemoComment(new UpdateMemoCommentCommand(
        "comment-1",
        "new",
        "other-user")))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("댓글은 존재하지만 부모 메모가 없으면 NOT_FOUND를 반환한다")
  void updateComment_parentMemoNotFound() {
    MemoComment comment = new MemoComment(
        "comment-1",
        "memo-1",
        "author-1",
        "old",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoCommentByIdPort.findMemoCommentById("comment-1"))
        .willReturn(Mono.just(comment));
    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.updateMemoComment(new UpdateMemoCommentCommand(
        "comment-1",
        "new",
        "author-1")))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.NOT_FOUND))
        .verify();
  }

}
