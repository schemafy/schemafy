package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetColumnsByTableIdService")
class GetColumnsByTableIdServiceTest {

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  GetColumnsByTableIdService sut;

  @Nested
  @DisplayName("getColumnsByTableId 메서드는")
  class GetColumnsByTableId {

    @Nested
    @DisplayName("컬럼이 있으면")
    class WhenColumnsExist {

      @Test
      @DisplayName("컬럼 목록을 반환한다")
      void returnsColumnList() {
        var query = ColumnFixture.getColumnsByTableIdQuery();
        var column1 = ColumnFixture.columnWithId("01ARZ3NDEKTSV4RRFFQ69G5CL1");
        var column2 = ColumnFixture.columnWithId("01ARZ3NDEKTSV4RRFFQ69G5CL2");
        var columns = List.of(column1, column2);

        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));

        StepVerifier.create(sut.getColumnsByTableId(query))
            .assertNext(result -> {
              assertThat(result).hasSize(2);
              assertThat(result.get(0).id()).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5CL1");
              assertThat(result.get(1).id()).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5CL2");
            })
            .verifyComplete();

        then(getColumnsByTableIdPort).should().findColumnsByTableId(query.tableId());
      }

    }

    @Nested
    @DisplayName("컬럼이 없으면")
    class WhenNoColumns {

      @Test
      @DisplayName("빈 목록을 반환한다")
      void returnsEmptyList() {
        var query = ColumnFixture.getColumnsByTableIdQuery();

        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.getColumnsByTableId(query))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        then(getColumnsByTableIdPort).should().findColumnsByTableId(query.tableId());
      }

    }

  }

}
