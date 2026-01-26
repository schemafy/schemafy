package com.schemafy.domain.erd.column.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetColumnService")
class GetColumnServiceTest {

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @InjectMocks
  GetColumnService sut;

  @Nested
  @DisplayName("getColumn 메서드는")
  class GetColumn {

    @Nested
    @DisplayName("컬럼이 존재하면")
    class WhenColumnExists {

      @Test
      @DisplayName("컬럼을 반환한다")
      void returnsColumn() {
        var query = ColumnFixture.getColumnQuery();
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.getColumn(query))
            .assertNext(result -> {
              assertThat(result.id()).isEqualTo(query.columnId());
              assertThat(result.name()).isEqualTo(ColumnFixture.DEFAULT_NAME);
            })
            .verifyComplete();

        then(getColumnByIdPort).should().findColumnById(query.columnId());
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("ColumnNotExistException을 발생시킨다")
      void throwsColumnNotExistException() {
        var query = ColumnFixture.getColumnQuery("non-existent-id");

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.getColumn(query))
            .expectError(ColumnNotExistException.class)
            .verify();

        then(getColumnByIdPort).should().findColumnById(query.columnId());
      }

    }

  }

}
