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
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandInversePayloadRegistry")
class SameCommandInversePayloadRegistryTest {

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
  SameCommandInversePayloadRegistry sut;

  @Test
  @DisplayName("column type inverse payload는 기존 type arguments를 모두 복원한다")
  void resolvesInversePayloadForColumnType() {
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

    StepVerifier.create(sut.resolve(
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
  @DisplayName("등록되지 않은 연산 inverse payload는 비어 있다")
  void returnsEmptyForUnsupportedOperation() {
    StepVerifier.create(sut.resolve(ErdOperationType.DELETE_TABLE, new Object()))
        .verifyComplete();
  }

}
