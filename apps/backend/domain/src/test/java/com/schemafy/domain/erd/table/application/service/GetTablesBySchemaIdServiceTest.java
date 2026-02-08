package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTablesBySchemaIdService")
class GetTablesBySchemaIdServiceTest {

  @Mock
  GetTablesBySchemaIdPort getTablesBySchemaIdPort;

  @InjectMocks
  GetTablesBySchemaIdService sut;

  @Nested
  @DisplayName("getTablesBySchemaId 메서드는")
  class GetTablesBySchemaId {

    @Nested
    @DisplayName("테이블이 존재하면")
    class WhenTablesExist {

      @Test
      @DisplayName("테이블 목록을 반환한다")
      void returnsTables() {
        var query = TableFixture.getTablesBySchemaIdQuery();
        var table1 = TableFixture.tableWithId("01ARZ3NDEKTSV4RRFFQ69G5TA1");
        var table2 = TableFixture.tableWithId("01ARZ3NDEKTSV4RRFFQ69G5TA2");

        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.just(table1, table2));

        StepVerifier.create(sut.getTablesBySchemaId(query))
            .assertNext(table -> assertThat(table.id()).isEqualTo(table1.id()))
            .assertNext(table -> assertThat(table.id()).isEqualTo(table2.id()))
            .verifyComplete();

        then(getTablesBySchemaIdPort).should().findTablesBySchemaId(query.schemaId());
      }

    }

    @Nested
    @DisplayName("테이블이 존재하지 않으면")
    class WhenNoTablesExist {

      @Test
      @DisplayName("빈 Flux를 반환한다")
      void returnsEmptyFlux() {
        var query = TableFixture.getTablesBySchemaIdQuery();

        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.empty());

        StepVerifier.create(sut.getTablesBySchemaId(query))
            .verifyComplete();

        then(getTablesBySchemaIdPort).should().findTablesBySchemaId(query.schemaId());
      }

    }

  }

}
