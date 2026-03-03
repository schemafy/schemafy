package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTableService")
class GetTableServiceTest {

  @Mock
  GetTableByIdPort getTableByIdPort;

  @InjectMocks
  GetTableService sut;

  @Nested
  @DisplayName("getTable 메서드는")
  class GetTable {

    @Nested
    @DisplayName("테이블이 존재하면")
    class WhenTableExists {

      @Test
      @DisplayName("테이블을 반환한다")
      void returnsTable() {
        var query = TableFixture.getTableQuery();
        var table = TableFixture.defaultTable();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));

        StepVerifier.create(sut.getTable(query))
            .assertNext(found -> {
              assertThat(found.id()).isEqualTo(table.id());
              assertThat(found.schemaId()).isEqualTo(table.schemaId());
              assertThat(found.name()).isEqualTo(table.name());
              assertThat(found.charset()).isEqualTo(table.charset());
              assertThat(found.collation()).isEqualTo(table.collation());
            })
            .verifyComplete();

        then(getTableByIdPort).should().findTableById(query.tableId());
      }

    }

    @Nested
    @DisplayName("테이블이 존재하지 않으면")
    class WhenTableNotExists {

      @Test
      @DisplayName("TableNotExistException을 발생시킨다")
      void throwsTableNotExistException() {
        var query = TableFixture.getTableQuery();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.getTable(query))
            .expectErrorMatches(DomainException.hasErrorCode(TableErrorCode.NOT_FOUND))
            .verify();
      }

    }

  }

}
