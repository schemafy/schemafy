package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdOperationInversePayloadResolver")
class ErdOperationInversePayloadResolverTest {

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @InjectMocks
  ErdOperationInversePayloadResolver sut;

  @Test
  @DisplayName("table name 변경의 inverse payload는 기존 이름을 사용한다")
  void resolvesTableNameInversePayload() {
    var table = TableFixture.tableWithIdAndName("table-1", "orders");
    var payload = new ChangeTableNameCommand("table-1", "orders_v2");

    given(getTableByIdPort.findTableById("table-1"))
        .willReturn(Mono.just(table));

    StepVerifier.create(sut.resolveBefore(ErdOperationType.CHANGE_TABLE_NAME, payload))
        .assertNext(inverse -> assertThat(inverse).isEqualTo(new ChangeTableNameCommand("table-1", "orders")))
        .verifyComplete();
  }

  @Test
  @DisplayName("column type 변경의 inverse payload는 기존 type arguments를 모두 복원한다")
  void resolvesColumnTypeInversePayload() {
    Column column = new Column(
        "column-1",
        "table-1",
        "status",
        "ENUM",
        new ColumnTypeArguments(null, null, null, List.of("READY", "DONE")),
        0,
        false,
        null,
        null,
        null);

    given(getColumnByIdPort.findColumnById("column-1"))
        .willReturn(Mono.just(column));

    StepVerifier.create(sut.resolveBefore(
        ErdOperationType.CHANGE_COLUMN_TYPE,
        new ChangeColumnTypeCommand("column-1", "INT", null, null, null)))
        .assertNext(inverse -> assertThat(inverse).isEqualTo(new ChangeColumnTypeCommand(
            "column-1",
            "ENUM",
            null,
            null,
            null,
            List.of("READY", "DONE"))))
        .verifyComplete();
  }

  @Test
  @DisplayName("지원하지 않는 연산은 inverse payload를 생성하지 않는다")
  void returnsEmptyForUnsupportedOperation() {
    StepVerifier.create(sut.resolveBefore(ErdOperationType.CHANGE_TABLE_EXTRA, new Object()))
        .verifyComplete();
  }

}
