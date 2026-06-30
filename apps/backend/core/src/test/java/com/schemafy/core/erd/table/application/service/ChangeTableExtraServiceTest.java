package com.schemafy.core.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonMetadataCanonicalizer;
import com.schemafy.core.erd.table.application.port.out.ChangeTableExtraPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.fixture.TableFixture;

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

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Spy
  JsonMetadataCanonicalizer jsonMetadataCanonicalizer = new JsonMetadataCanonicalizer(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

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
        var command = TableFixture.changeExtraCommand(
            TableFixture.jsonObject("{\"ui\": {\"x\": 100, \"y\": 200}}"));

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));
        given(changeTableExtraPort.changeTableExtra(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableExtra(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeTableExtraPort).should()
            .changeTableExtra(command.tableId(), "{\"ui\":{\"x\":100,\"y\":200}}");
      }

      @Test
      @DisplayName("extra 값이 이미 null이면 변경 없이 성공한다")
      void returnsSuccessWithoutMutationWhenExtraIsAlreadyNull() {
        var command = TableFixture.changeExtraCommand(null);

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));

        StepVerifier.create(sut.changeTableExtra(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(changeTableExtraPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("JSON null이 현재 null로 정규화되면 변경 없이 성공한다")
      void returnsSuccessWithoutMutationWhenJsonNullExtraNormalizesToCurrentNull() {
        var command = TableFixture.changeExtraCommand(NullNode.getInstance());

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));

        StepVerifier.create(sut.changeTableExtra(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(changeTableExtraPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("JSON object가 아니면 error signal로 반환한다")
      void returnsErrorSignalWhenExtraIsNotObject() {
        var command = TableFixture.changeExtraCommand(TextNode.valueOf("invalid"));

        StepVerifier.create(sut.changeTableExtra(command))
            .expectError(IllegalArgumentException.class)
            .verify();

        then(getTableByIdPort).shouldHaveNoInteractions();
        then(changeTableExtraPort).shouldHaveNoInteractions();
      }

    }

  }

}
