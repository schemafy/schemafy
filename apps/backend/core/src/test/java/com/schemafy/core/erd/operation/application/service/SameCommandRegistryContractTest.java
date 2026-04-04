package com.schemafy.core.erd.operation.application.service;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommand registry contract")
class SameCommandRegistryContractTest {

  @Mock
  ChangeTableNameUseCase changeTableNameUseCase;

  @Mock
  ChangeColumnNameUseCase changeColumnNameUseCase;

  @Mock
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Mock
  ChangeRelationshipNameUseCase changeRelationshipNameUseCase;

  @Mock
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Mock
  ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;

  @Mock
  ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @Mock
  ChangeIndexNameUseCase changeIndexNameUseCase;

  @Mock
  ChangeIndexTypeUseCase changeIndexTypeUseCase;

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
  SameCommandReplayRegistry replayRegistry;

  @InjectMocks
  SameCommandInversePayloadRegistry inversePayloadRegistry;

  @Test
  @DisplayName("replay와 inverse registry는 같은 same-command 연산 집합을 지원한다")
  void supportsSameOperationSet() {
    Set<ErdOperationType> expectedOperations = Set.of(
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationType.CHANGE_COLUMN_NAME,
        ErdOperationType.CHANGE_COLUMN_TYPE,
        ErdOperationType.CHANGE_RELATIONSHIP_NAME,
        ErdOperationType.CHANGE_RELATIONSHIP_KIND,
        ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY,
        ErdOperationType.CHANGE_CONSTRAINT_NAME,
        ErdOperationType.CHANGE_INDEX_NAME,
        ErdOperationType.CHANGE_INDEX_TYPE);

    assertThat(replayRegistry.supportedOperationTypes()).isEqualTo(expectedOperations);
    assertThat(inversePayloadRegistry.supportedOperationTypes()).isEqualTo(expectedOperations);
    assertThat(replayRegistry.supportedOperationTypes()).isEqualTo(inversePayloadRegistry.supportedOperationTypes());
  }

}
