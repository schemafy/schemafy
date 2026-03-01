package com.schemafy.domain.erd.memo.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommand;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoCommentPort;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoPort;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMemoService")
class CreateMemoServiceTest {

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateMemoPort createMemoPort;

  @Mock
  CreateMemoCommentPort createMemoCommentPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  CreateMemoService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("createMemo: 메모와 초기 댓글을 함께 생성한다")
  void createMemo_success() {
    CreateMemoCommand command = new CreateMemoCommand(
        "schema-1",
        "{}",
        "body",
        "author-1");

    Schema schema = new Schema(
        "schema-1",
        "project-1",
        "mysql",
        "schema",
        "utf8mb4",
        "utf8mb4_unicode_ci");

    given(getSchemaByIdPort.findSchemaById("schema-1"))
        .willReturn(Mono.just(schema));
    given(ulidGeneratorPort.generate())
        .willReturn("memo-1", "comment-1");
    given(createMemoPort.createMemo(any(Memo.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(createMemoCommentPort.createMemoComment(any(MemoComment.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.createMemo(command))
        .assertNext(result -> {
          assertThat(result.memo().id()).isEqualTo("memo-1");
          assertThat(result.memo().schemaId()).isEqualTo("schema-1");
          assertThat(result.memo().authorId()).isEqualTo("author-1");
          assertThat(result.comments()).hasSize(1);
          assertThat(result.comments().get(0).id()).isEqualTo("comment-1");
          assertThat(result.comments().get(0).memoId()).isEqualTo("memo-1");
          assertThat(result.comments().get(0).body()).isEqualTo("body");
        })
        .verifyComplete();

    then(createMemoPort).should().createMemo(any(Memo.class));
    then(createMemoCommentPort).should().createMemoComment(any(MemoComment.class));
  }

  @Test
  @DisplayName("createMemo: 스키마가 없으면 NOT_FOUND를 반환한다")
  void createMemo_schemaNotFound() {
    CreateMemoCommand command = new CreateMemoCommand(
        "schema-1",
        "{}",
        "body",
        "author-1");

    given(getSchemaByIdPort.findSchemaById("schema-1"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.createMemo(command))
        .expectErrorMatches(DomainException.hasErrorCode(
            SchemaErrorCode.NOT_FOUND))
        .verify();

    then(createMemoPort).shouldHaveNoInteractions();
    then(createMemoCommentPort).shouldHaveNoInteractions();
  }

}
