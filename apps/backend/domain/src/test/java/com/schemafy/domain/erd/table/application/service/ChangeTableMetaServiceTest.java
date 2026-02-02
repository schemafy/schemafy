package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeTableMetaService")
class ChangeTableMetaServiceTest {

  @Mock
  ChangeTableMetaPort changeTableMetaPort;

  @InjectMocks
  ChangeTableMetaService sut;

  @Nested
  @DisplayName("changeTableMeta 메서드는")
  class ChangeTableMeta {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("charset과 collation을 변경한다")
      void changesCharsetAndCollation() {
        var command = TableFixture.changeMetaCommand("utf8", "utf8_general_ci");

        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(command.tableId(), command.charset(), command.collation());
      }

    }

  }

}
