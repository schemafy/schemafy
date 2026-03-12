package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommand;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteMemoService")
class DeleteMemoServiceTest {

  @Mock
  GetMemoByIdPort getMemoByIdPort;

  @Mock
  SoftDeleteMemoPort softDeleteMemoPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  DeleteMemoService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("작성자는 메모를 soft delete할 수 있다")
  void deleteMemo_author_success() {
    Memo memo = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));
    given(softDeleteMemoPort.softDeleteMemo(any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.deleteMemo(new DeleteMemoCommand(
        "memo-1",
        "author-1",
        false)))
        .verifyComplete();

    then(softDeleteMemoPort).should().softDeleteMemo(any(), any());
  }

  @Test
  @DisplayName("권한이 없으면 ACCESS_DENIED를 반환한다")
  void deleteMemo_accessDenied() {
    Memo memo = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));

    StepVerifier.create(sut.deleteMemo(new DeleteMemoCommand(
        "memo-1",
        "other-user",
        false)))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.ACCESS_DENIED))
        .verify();
  }

}
