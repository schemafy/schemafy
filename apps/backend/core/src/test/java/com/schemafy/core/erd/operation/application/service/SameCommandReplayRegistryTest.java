package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandReplayRegistry")
class SameCommandReplayRegistryTest {

  @Mock
  ChangeTableNameUseCase changeTableNameUseCase;

  @Mock
  ChangeConstraintNamePort changeConstraintNamePort;

  @Mock
  ChangeRelationshipNamePort changeRelationshipNamePort;

  @Mock
  ChangeColumnNameUseCase changeColumnNameUseCase;

  @Mock
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Mock
  ChangeRelationshipNameUseCase changeRelationshipNameUseCase;

  @Mock
  ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;

  @Mock
  ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @Mock
  ChangeIndexNameUseCase changeIndexNameUseCase;

  @Mock
  ChangeIndexTypeUseCase changeIndexTypeUseCase;

  @Spy
  JsonCodec jsonCodec = new JsonCodec(new ObjectMapper());

  @InjectMocks
  SameCommandReplayRegistry sut;

  @ParameterizedTest
  @MethodSource("supportedOperations")
  @DisplayName("등록된 same-command 연산을 지원한다")
  void supportsRegisteredOperations(ErdOperationType opType, Class<?> payloadType) {
    assertThat(sut.supports(opType)).isTrue();
  }

  @Test
  @DisplayName("executePersisted는 등록된 use case에 persisted payload를 역직렬화해 위임한다")
  void delegatesPersistedExecutionToRegisteredUseCase() {
    ChangeIndexNameCommand command = new ChangeIndexNameCommand("index-1", "idx_new");

    given(changeIndexNameUseCase.changeIndexName(command))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(command)))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(changeIndexNameUseCase).should().changeIndexName(command);
  }

  @Test
  @DisplayName("table name replay는 기존 command payload도 처리한다")
  void replaysLegacyTableNameCommandPayload() {
    ChangeTableNameCommand command = new ChangeTableNameCommand("table-1", "orders_v2");

    given(changeTableNameUseCase.changeTableName(command))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.executePersisted(
        ErdOperationType.CHANGE_TABLE_NAME,
        jsonCodec.serialize(command)))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(changeTableNameUseCase).should().changeTableName(command);
    then(changeConstraintNamePort).shouldHaveNoInteractions();
    then(changeRelationshipNamePort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("table name replay는 side effect 이름 복원 payload를 적용한다")
  void restoresTableNameSideEffectNames() {
    ChangeTableNameCommand command = new ChangeTableNameCommand("table-1", "orders");
    ChangeTableNameReplayPayload payload = new ChangeTableNameReplayPayload(
        command.tableId(),
        command.newName(),
        List.of(new ChangeTableNameReplayPayload.NameRestore("constraint-1", "primary_orders")),
        List.of(new ChangeTableNameReplayPayload.NameRestore("relationship-1", "rel_orders_to_users")));

    given(changeTableNameUseCase.changeTableName(command))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));
    given(changeConstraintNamePort.changeConstraintName("constraint-1", "primary_orders"))
        .willReturn(Mono.empty());
    given(changeRelationshipNamePort.changeRelationshipName("relationship-1", "rel_orders_to_users"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.executePersisted(
        ErdOperationType.CHANGE_TABLE_NAME,
        jsonCodec.serialize(payload)))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(changeTableNameUseCase).should().changeTableName(command);
    then(changeConstraintNamePort).should().changeConstraintName("constraint-1", "primary_orders");
    then(changeRelationshipNamePort).should().changeRelationshipName("relationship-1", "rel_orders_to_users");
  }

  @Test
  @DisplayName("등록되지 않은 연산 executePersisted는 예외가 발생한다")
  void throwsWhenExecutingPersistedUnsupportedOperation() {
    StepVerifier.create(sut.executePersisted(ErdOperationType.DELETE_TABLE, "{}"))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported same-command undo/redo operation: DELETE_TABLE"))
        .verify();
  }

  @Test
  @DisplayName("relationship kind 변경은 same-command replay 대상으로 등록하지 않는다")
  void doesNotSupportRelationshipKind() {
    assertThat(sut.supports(ErdOperationType.CHANGE_RELATIONSHIP_KIND)).isFalse();
  }

  private static Stream<Arguments> supportedOperations() {
    return Stream.of(
        Arguments.of(ErdOperationType.CHANGE_TABLE_NAME, ChangeTableNameReplayPayload.class),
        Arguments.of(ErdOperationType.CHANGE_COLUMN_NAME, ChangeColumnNameCommand.class),
        Arguments.of(ErdOperationType.CHANGE_COLUMN_TYPE, ChangeColumnTypeCommand.class),
        Arguments.of(ErdOperationType.CHANGE_RELATIONSHIP_NAME, ChangeRelationshipNameCommand.class),
        Arguments.of(ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY, ChangeRelationshipCardinalityCommand.class),
        Arguments.of(ErdOperationType.CHANGE_CONSTRAINT_NAME, ChangeConstraintNameCommand.class),
        Arguments.of(ErdOperationType.CHANGE_INDEX_NAME, ChangeIndexNameCommand.class),
        Arguments.of(ErdOperationType.CHANGE_INDEX_TYPE, ChangeIndexTypeCommand.class));
  }

}
