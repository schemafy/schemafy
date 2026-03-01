package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeIndexTypeService")
class ChangeIndexTypeServiceTest {

  @Mock
  ChangeIndexTypePort changeIndexTypePort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @InjectMocks
  ChangeIndexTypeService sut;

  @Nested
  @DisplayName("changeIndexType 메서드는")
  class ChangeIndexType {

    @Test
    @DisplayName("유효한 타입으로 변경한다")
    void changesTypeWithValidInput() {
      var command = IndexFixture.changeTypeCommand("index1", IndexType.HASH);
      var index = IndexFixture.btreeIndexWithId("index1");
      var indexColumns = List.of(IndexFixture.indexColumnWithIndexId("index1"));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(List.of(index)));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(indexColumns));
      given(changeIndexTypePort.changeIndexType("index1", IndexType.HASH))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexType(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexTypePort).should().changeIndexType("index1", IndexType.HASH);
    }

    @Test
    @DisplayName("타입이 null이면 예외가 발생한다")
    void throwsWhenTypeIsNull() {
      var command = IndexFixture.changeTypeCommand("index1", null);

      StepVerifier.create(sut.changeIndexType(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.TYPE_INVALID))
          .verify();

      then(changeIndexTypePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var command = IndexFixture.changeTypeCommand("nonexistent", IndexType.HASH);

      given(getIndexByIdPort.findIndexById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexType(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.NOT_FOUND))
          .verify();

      then(changeIndexTypePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 컬럼 조합과 타입의 인덱스가 이미 존재하면 예외가 발생한다")
    void throwsWhenDuplicateDefinition() {
      var command = IndexFixture.changeTypeCommand("index1", IndexType.HASH);
      var index = IndexFixture.btreeIndexWithId("index1");
      var existingHashIndex = IndexFixture.hashIndexWithId("index2");
      var indexes = List.of(index, existingHashIndex);

      var index1Columns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC));
      var index2Columns = List.of(
          IndexFixture.indexColumn("ic2", "index2", "col1", 0, SortDirection.ASC));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(indexes));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(index1Columns));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index2"))
          .willReturn(Mono.just(index2Columns));

      StepVerifier.create(sut.changeIndexType(command))
          .expectErrorMatches(DomainException.hasErrorCode(IndexErrorCode.DEFINITION_DUPLICATE))
          .verify();

      then(changeIndexTypePort).shouldHaveNoInteractions();
    }

  }

}
