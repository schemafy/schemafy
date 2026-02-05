package com.schemafy.domain.erd.index.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteIndexService")
class DeleteIndexServiceTest {

  @Mock
  DeleteIndexPort deleteIndexPort;

  @Mock
  DeleteIndexColumnsByIndexIdPort deleteIndexColumnsPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  DeleteIndexService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("deleteIndex 메서드는")
  class DeleteIndex {

    @Test
    @DisplayName("인덱스 컬럼을 먼저 삭제하고 인덱스를 삭제한다")
    void deletesColumnsFirstThenIndex() {
      var command = IndexFixture.deleteCommand("index1");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(deleteIndexColumnsPort.deleteByIndexId("index1"))
          .willReturn(Mono.empty());
      given(deleteIndexPort.deleteIndex("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteIndex(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteIndexColumnsPort).should().deleteByIndexId("index1");
      then(deleteIndexPort).should().deleteIndex("index1");
    }

    @Test
    @DisplayName("컬럼이 없는 인덱스도 삭제한다")
    void deletesIndexWithoutColumns() {
      var command = IndexFixture.deleteCommand("index1");
      var index = IndexFixture.indexWithId("index1");

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(deleteIndexColumnsPort.deleteByIndexId("index1"))
          .willReturn(Mono.empty());
      given(deleteIndexPort.deleteIndex("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteIndex(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteIndexPort).should().deleteIndex("index1");
    }

  }

}
