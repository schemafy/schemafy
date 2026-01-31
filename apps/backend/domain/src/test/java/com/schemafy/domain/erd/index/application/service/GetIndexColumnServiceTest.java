package com.schemafy.domain.erd.index.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetIndexColumnService")
class GetIndexColumnServiceTest {

  @Mock
  GetIndexColumnByIdPort getIndexColumnByIdPort;

  @InjectMocks
  GetIndexColumnService sut;

  @Nested
  @DisplayName("getIndexColumn 메서드는")
  class GetIndexColumn {

    @Test
    @DisplayName("인덱스 컬럼이 존재하면 반환한다")
    void returnsIndexColumnWhenExists() {
      var query = IndexFixture.getIndexColumnQuery("ic1");
      var indexColumn = IndexFixture.indexColumn("ic1", "idx1", "col1", 0, SortDirection.ASC);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));

      StepVerifier.create(sut.getIndexColumn(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo("ic1");
            assertThat(result.indexId()).isEqualTo("idx1");
            assertThat(result.columnId()).isEqualTo("col1");
            assertThat(result.sortDirection()).isEqualTo(SortDirection.ASC);
          })
          .verifyComplete();

      then(getIndexColumnByIdPort).should().findIndexColumnById("ic1");
    }

    @Test
    @DisplayName("인덱스 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexColumnNotExists() {
      var query = IndexFixture.getIndexColumnQuery("nonexistent");

      given(getIndexColumnByIdPort.findIndexColumnById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getIndexColumn(query))
          .expectError(IndexColumnNotExistException.class)
          .verify();
    }

  }

}
