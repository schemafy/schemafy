package com.schemafy.domain.erd.column.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByColumnIdPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteColumnService")
class DeleteColumnServiceTest {

  @Mock
  DeleteColumnPort deleteColumnPort;

  @Mock
  DeleteConstraintColumnsByColumnIdPort deleteConstraintColumnsPort;

  @Mock
  DeleteIndexColumnsByColumnIdPort deleteIndexColumnsPort;

  @Mock
  DeleteRelationshipColumnsByColumnIdPort deleteRelationshipColumnsPort;

  @InjectMocks
  DeleteColumnService sut;

  @Nested
  @DisplayName("deleteColumn 메서드는")
  class DeleteColumn {

    @Test
    @DisplayName("관련 엔티티들을 삭제하고 컬럼을 삭제한다")
    void deletesRelatedEntitiesAndColumn() {
      var command = ColumnFixture.deleteCommand();

      given(deleteConstraintColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteIndexColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteRelationshipColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteConstraintColumnsPort).should()
          .deleteByColumnId(command.columnId());
      then(deleteIndexColumnsPort).should()
          .deleteByColumnId(command.columnId());
      then(deleteRelationshipColumnsPort).should()
          .deleteByColumnId(command.columnId());
      then(deleteColumnPort).should()
          .deleteColumn(command.columnId());
    }

    @Test
    @DisplayName("cascade 삭제가 먼저 실행된다")
    void cascadeDeletesBeforeColumnDelete() {
      var command = ColumnFixture.deleteCommand();

      given(deleteConstraintColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteIndexColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteRelationshipColumnsPort.deleteByColumnId(any()))
          .willReturn(Mono.empty());
      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      var inOrder = org.mockito.Mockito.inOrder(
          deleteConstraintColumnsPort,
          deleteIndexColumnsPort,
          deleteRelationshipColumnsPort,
          deleteColumnPort);

      inOrder.verify(deleteColumnPort).deleteColumn(command.columnId());
    }

  }

}
