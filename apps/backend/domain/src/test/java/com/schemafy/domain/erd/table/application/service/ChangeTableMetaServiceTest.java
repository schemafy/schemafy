package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.PatchField;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        var command = TableFixture.changeMetaCommand(
            PatchField.of("utf8"), PatchField.of("utf8_general_ci"));

        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(eq(command.tableId()), eq("utf8"), eq("utf8_general_ci"));
      }

    }

    @Nested
    @DisplayName("필드가 생략되면")
    class WithAbsentFields {

      @Test
      @DisplayName("생략된 필드는 포트에 null로 전달된다")
      void skipsAbsentFields() {
        var command = TableFixture.changeMetaCommand(
            PatchField.of("utf8"), PatchField.absent());

        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(eq(command.tableId()), eq("utf8"), isNull());
      }

    }

    @Nested
    @DisplayName("명시적 null로 클리어 시")
    class WithExplicitNullClear {

      @Test
      @DisplayName("charset을 null로 설정하면 빈 문자열이 포트에 전달된다")
      void clearsCharsetToNull() {
        var command = TableFixture.changeMetaCommand(
            PatchField.of(null), PatchField.absent());

        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(eq(command.tableId()), eq(""), isNull());
      }

    }

  }

}
