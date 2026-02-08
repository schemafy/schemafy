package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveIndexColumnService")
class RemoveIndexColumnServiceTest {

  @Mock
  DeleteIndexColumnPort deleteIndexColumnPort;

  @Mock
  DeleteIndexPort deleteIndexPort;

  @Mock
  ChangeIndexColumnPositionPort changeIndexColumnPositionPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  RemoveIndexColumnService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("removeIndexColumn 메서드는")
  class RemoveIndexColumn {

    @Test
    @DisplayName("유효한 요청 시 컬럼을 삭제하고 남은 컬럼들을 재정렬한다")
    void removesColumnAndReordersRemaining() {
      var command = IndexFixture.removeColumnCommand("ic1");
      var index = IndexFixture.indexWithId("index1");
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var remainingColumns = List.of(
          IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC));

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(deleteIndexColumnPort.deleteIndexColumn("ic1"))
          .willReturn(Mono.empty());
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(remainingColumns));
      given(changeIndexColumnPositionPort.changeIndexColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeIndexColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteIndexColumnPort).should().deleteIndexColumn("ic1");
      then(changeIndexColumnPositionPort).should().changeIndexColumnPositions(eq("index1"), any());
      then(deleteIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마지막 컬럼 삭제 시 인덱스도 함께 삭제한다")
    void deletesIndexWhenLastColumnRemoved() {
      var command = IndexFixture.removeColumnCommand("ic1");
      var index = IndexFixture.indexWithId("index1");
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(deleteIndexColumnPort.deleteIndexColumn("ic1"))
          .willReturn(Mono.empty());
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(List.of()));
      given(deleteIndexPort.deleteIndex("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeIndexColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteIndexColumnPort).should().deleteIndexColumn("ic1");
      then(deleteIndexPort).should().deleteIndex("index1");
      then(changeIndexColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var command = IndexFixture.removeColumnCommand("ic1");
      var indexColumn = IndexFixture.indexColumn("ic1", "nonexistent", "col1", 0, SortDirection.ASC);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeIndexColumn(command))
          .expectError(IndexNotExistException.class)
          .verify();

      then(deleteIndexColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexColumnNotExists() {
      var command = IndexFixture.removeColumnCommand("nonexistent");
      given(getIndexColumnByIdPort.findIndexColumnById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeIndexColumn(command))
          .expectError(IndexColumnNotExistException.class)
          .verify();

      then(deleteIndexColumnPort).shouldHaveNoInteractions();
    }

  }

}
