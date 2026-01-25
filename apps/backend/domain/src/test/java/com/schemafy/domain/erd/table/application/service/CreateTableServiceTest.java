package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.table.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.erd.table.fixture.TableFixture;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTableService")
class CreateTableServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateTablePort createTablePort;

  @Mock
  TableExistsPort tableExistsPort;

  @InjectMocks
  CreateTableService sut;

  @Nested
  @DisplayName("createTable 메서드는")
  class CreateTable {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("테이블을 생성하고 결과를 반환한다")
      void returnsCreatedTable() {
        var command = TableFixture.createCommand();
        var table = TableFixture.defaultTable();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(ulidGeneratorPort.generate())
            .willReturn(TableFixture.DEFAULT_ID);
        given(createTablePort.createTable(any(Table.class)))
            .willReturn(Mono.just(table));

        StepVerifier.create(sut.createTable(command))
            .assertNext(result -> {
              assertThat(result.tableId()).isEqualTo(TableFixture.DEFAULT_ID);
              assertThat(result.name()).isEqualTo(command.name());
              assertThat(result.charset()).isEqualTo(command.charset());
              assertThat(result.collation()).isEqualTo(command.collation());
            })
            .verifyComplete();

        then(tableExistsPort).should()
            .existsBySchemaIdAndName(command.schemaId(), command.name());
        then(createTablePort).should().createTable(any(Table.class));
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("TableNameDuplicateException을 발생시킨다")
      void throwsTableNameDuplicateException() {
        var command = TableFixture.createCommand();

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.createTable(command))
            .expectError(TableNameDuplicateException.class)
            .verify();

        then(createTablePort).shouldHaveNoInteractions();
        then(ulidGeneratorPort).shouldHaveNoInteractions();
      }

    }

  }

}
