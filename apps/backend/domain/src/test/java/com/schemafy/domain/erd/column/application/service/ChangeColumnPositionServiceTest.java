package com.schemafy.domain.erd.column.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPositionInvalidException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnPositionService")
class ChangeColumnPositionServiceTest {

  @Mock
  ChangeColumnPositionPort changeColumnPositionPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @InjectMocks
  ChangeColumnPositionService sut;

  @Nested
  @DisplayName("changeColumnPosition 메서드는")
  class ChangeColumnPosition {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("위치를 변경한다")
      void changesPosition() {
        var command = ColumnFixture.changePositionCommand(5);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(changeColumnPositionPort.changeColumnPosition(any(), any(int.class)))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnPositionPort).should()
            .changeColumnPosition(eq(command.columnId()), eq(5));
      }

    }

    @Nested
    @DisplayName("음수 위치가 주어지면")
    class WithNegativePosition {

      @Test
      @DisplayName("ColumnPositionInvalidException이 발생한다")
      void throwsColumnPositionInvalidException() {
        var command = ColumnFixture.changePositionCommand(-1);

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectError(ColumnPositionInvalidException.class)
            .verify();

        then(getColumnByIdPort).shouldHaveNoInteractions();
        then(changeColumnPositionPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changePositionCommand(5);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectError(ColumnNotExistException.class)
            .verify();

        then(changeColumnPositionPort).shouldHaveNoInteractions();
      }

    }

  }

}
