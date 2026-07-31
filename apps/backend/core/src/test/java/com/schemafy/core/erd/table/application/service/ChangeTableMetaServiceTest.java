package com.schemafy.core.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.PatchField;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableMetaInverse;
import com.schemafy.core.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Mock
  GetTableByIdPort getTableByIdPort;

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

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));
        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .assertNext(result -> assertThat(result.inversePayload()).isEqualTo(
                new ChangeTableMetaInverse(
                    command.tableId(),
                    TableFixture.DEFAULT_CHARSET,
                    TableFixture.DEFAULT_COLLATION)))
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(eq(command.tableId()), eq("utf8"), eq("utf8_general_ci"));
      }

      @Test
      @DisplayName("현재 meta와 같으면 변경 없이 성공한다")
      void returnsSuccessWithoutMutationWhenMetaIsUnchanged() {
        var command = TableFixture.changeMetaCommand(
            PatchField.of(TableFixture.DEFAULT_CHARSET),
            PatchField.of(TableFixture.DEFAULT_COLLATION));

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(changeTableMetaPort).shouldHaveNoInteractions();
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

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));
        given(changeTableMetaPort.changeTableMeta(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeTableMetaPort).should()
            .changeTableMeta(eq(command.tableId()), eq("utf8"), isNull());
      }

      @Test
      @DisplayName("모든 필드가 생략되면 변경 없이 성공한다")
      void returnsSuccessWithoutMutationWhenAllFieldsAreAbsent() {
        var command = TableFixture.changeMetaCommand(
            PatchField.absent(), PatchField.absent());

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));

        StepVerifier.create(sut.changeTableMeta(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(changeTableMetaPort).shouldHaveNoInteractions();
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

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));
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
