package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.table.application.port.out.ChangeTableExtraPort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeTableExtraService")
class ChangeTableExtraServiceTest {

  @Mock
  ChangeTableExtraPort changeTableExtraPort;

  @InjectMocks
  ChangeTableExtraService sut;

  @Nested
  @DisplayName("changeTableExtra 메서드는")
  class ChangeTableExtra {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("extra 필드를 변경한다")
      void changesExtra() {
        var command = TableFixture.changeExtraCommand("ENGINE=InnoDB");

        given(changeTableExtraPort.changeTableExtra(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableExtra(command))
            .verifyComplete();

        then(changeTableExtraPort).should()
            .changeTableExtra(command.tableId(), command.extra());
      }

    }

  }

}
