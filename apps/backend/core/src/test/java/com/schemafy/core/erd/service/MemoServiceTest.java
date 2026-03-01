package com.schemafy.core.erd.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.service.memo.MemoApiCommandMapper;
import com.schemafy.core.erd.service.memo.MemoApiResponseMapper;
import com.schemafy.core.erd.service.memo.MemoDeletePermissionPolicy;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.in.DeleteMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoCommentsUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoPositionUseCase;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.MemoDetail;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoService")
class MemoServiceTest {

  @Mock
  CreateMemoUseCase createMemoUseCase;

  @Mock
  GetMemoUseCase getMemoUseCase;

  @Mock
  GetMemosBySchemaIdUseCase getMemosBySchemaIdUseCase;

  @Mock
  UpdateMemoPositionUseCase updateMemoPositionUseCase;

  @Mock
  DeleteMemoUseCase deleteMemoUseCase;

  @Mock
  CreateMemoCommentUseCase createMemoCommentUseCase;

  @Mock
  GetMemoCommentsUseCase getMemoCommentsUseCase;

  @Mock
  UpdateMemoCommentUseCase updateMemoCommentUseCase;

  @Mock
  DeleteMemoCommentUseCase deleteMemoCommentUseCase;

  @Mock
  UserRepository userRepository;

  MemoService sut;

  @BeforeEach
  void setUp() {
    sut = new MemoService(
        createMemoUseCase,
        getMemoUseCase,
        getMemosBySchemaIdUseCase,
        updateMemoPositionUseCase,
        deleteMemoUseCase,
        createMemoCommentUseCase,
        getMemoCommentsUseCase,
        updateMemoCommentUseCase,
        deleteMemoCommentUseCase,
        userRepository,
        new MemoApiCommandMapper(new MemoDeletePermissionPolicy()),
        new MemoApiResponseMapper());
  }

  @Test
  @DisplayName("getMemo: 작성자 정보가 없으면 Unknown으로 fallback 한다")
  void getMemo_unknownUserFallback() {
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
        "author-2",
        "hello",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    when(getMemoUseCase.getMemo(any()))
        .thenReturn(Mono.just(new MemoDetail(memo, List.of(comment))));

    User user = mock(User.class);
    when(user.getId()).thenReturn("author-1");
    when(user.getName()).thenReturn("작성자");
    when(userRepository.findAllById(any(Iterable.class)))
        .thenReturn(Flux.just(user));

    StepVerifier.create(sut.getMemo("memo-1"))
        .assertNext(response -> {
          assertThat(response.getAuthor().id()).isEqualTo("author-1");
          assertThat(response.getAuthor().name()).isEqualTo("작성자");
          assertThat(response.getComments()).hasSize(1);
          assertThat(response.getComments().get(0).author().id())
              .isEqualTo("author-2");
          assertThat(response.getComments().get(0).author().name())
              .isEqualTo("Unknown");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("updateMemo: 도메인 결과를 기존 응답 형태로 반환한다")
  void updateMemo_responseShape() {
    Memo updated = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{\"x\":100}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-02T00:00:00Z"),
        null);

    when(updateMemoPositionUseCase.updateMemoPosition(any()))
        .thenReturn(Mono.just(updated));

    User user = mock(User.class);
    when(user.getId()).thenReturn("author-1");
    when(user.getName()).thenReturn("작성자");
    when(userRepository.findById("author-1"))
        .thenReturn(Mono.just(user));

    StepVerifier.create(sut.updateMemo(
        "memo-1",
        new UpdateMemoRequest("{\"x\":100}"),
        AuthenticatedUser.of("author-1")))
        .assertNext(response -> {
          assertThat(response.id()).isEqualTo("memo-1");
          assertThat(response.schemaId()).isEqualTo("schema-1");
          assertThat(response.positions()).isEqualTo("{\"x\":100}");
          assertThat(response.author().id()).isEqualTo("author-1");
          assertThat(response.author().name()).isEqualTo("작성자");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteComment: 도메인 ACCESS_DENIED를 그대로 전달한다")
  void deleteComment_accessDenied_passthrough() {
    when(deleteMemoCommentUseCase.deleteMemoComment(any()))
        .thenReturn(Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED)));

    StepVerifier.create(sut.deleteComment(
        "memo-1",
        "comment-1",
        AuthenticatedUser.withRoles("user-1", Set.of(ProjectRole.COMMENTER))))
        .expectErrorMatches(error -> error instanceof DomainException
            && ((DomainException) error)
                .getErrorCode() == MemoErrorCode.ACCESS_DENIED)
        .verify();
  }

  @Test
  @DisplayName("updateComment: 도메인 INVALID_PARAMETER를 그대로 전달한다")
  void updateComment_invalidParameter_passthrough() {
    when(updateMemoCommentUseCase.updateMemoComment(any()))
        .thenReturn(Mono.error(new DomainException(MemoErrorCode.INVALID_PARAMETER)));

    StepVerifier.create(sut.updateComment(
        "memo-1",
        "comment-1",
        new UpdateMemoCommentRequest("body"),
        AuthenticatedUser.of("user-1")))
        .expectErrorMatches(error -> error instanceof DomainException
            && ((DomainException) error)
                .getErrorCode() == MemoErrorCode.INVALID_PARAMETER)
        .verify();
  }

}
