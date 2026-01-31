package com.schemafy.domain.erd.table.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.CascadeDeleteConstraintsByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.CascadeDeleteIndexesByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTableService")
class DeleteTableServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteTablePort deleteTablePort;

  @Mock
  DeleteColumnsByTableIdPort deleteColumnsByTableIdPort;

  @Mock
  CascadeDeleteConstraintsByTableIdPort cascadeDeleteConstraintsPort;

  @Mock
  CascadeDeleteIndexesByTableIdPort cascadeDeleteIndexesPort;

  @Mock
  CascadeDeleteRelationshipsByTableIdPort cascadeDeleteRelationshipsPort;

  @InjectMocks
  DeleteTableService sut;

  @Nested
  @DisplayName("deleteTable 메서드는")
  class DeleteTable {

    @Nested
    @DisplayName("삭제 요청이 주어지면")
    class WithDeleteRequest {

      @Test
      @DisplayName("관련 엔티티들을 순서대로 삭제한다")
      void deletesRelatedEntitiesInOrder() {
        var command = TableFixture.deleteCommand();

        given(cascadeDeleteConstraintsPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(cascadeDeleteIndexesPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(deleteColumnsByTableIdPort.deleteColumnsByTableId(any()))
            .willReturn(Mono.empty());
        given(deleteTablePort.deleteTable(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .verifyComplete();

        then(cascadeDeleteConstraintsPort).should()
            .cascadeDeleteByTableId(command.tableId());
        then(cascadeDeleteIndexesPort).should()
            .cascadeDeleteByTableId(command.tableId());
        then(cascadeDeleteRelationshipsPort).should()
            .cascadeDeleteByTableId(command.tableId());
        then(deleteColumnsByTableIdPort).should()
            .deleteColumnsByTableId(command.tableId());
        then(deleteTablePort).should()
            .deleteTable(command.tableId());
      }

    }

  }

}
