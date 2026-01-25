package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeTableNameService")
class ChangeTableNameServiceTest {

  @Mock
  ChangeTableNamePort changeTableNamePort;

  @Mock
  TableExistsPort tableExistsPort;

  @InjectMocks
  ChangeTableNameService sut;

  @Nested
  @DisplayName("changeTableName 메서드는")
  class ChangeTableName {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("테이블 이름을 변경한다")
      void changesTableName() {
        var command = TableFixture.changeNameCommand("new_table_name");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(changeTableNamePort.changeTableName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableName(command))
            .verifyComplete();

        then(tableExistsPort).should()
            .existsBySchemaIdAndName(command.schemaId(), command.newName());
        then(changeTableNamePort).should()
            .changeTableName(command.tableId(), command.newName());
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("IllegalArgumentException을 발생시킨다")
      void throwsIllegalArgumentException() {
        var command = TableFixture.changeNameCommand("existing_table_name");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.changeTableName(command))
            .expectError(IllegalArgumentException.class)
            .verify();

        then(changeTableNamePort).shouldHaveNoInteractions();
      }

    }

  }

}
