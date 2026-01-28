package com.schemafy.domain.erd.constraint.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConstraintService")
class DeleteConstraintServiceTest {

  @Mock
  DeleteConstraintPort deleteConstraintPort;

  @Mock
  DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort;

  @InjectMocks
  DeleteConstraintService sut;

  @Nested
  @DisplayName("deleteConstraint 메서드는")
  class DeleteConstraint {

    @Test
    @DisplayName("제약조건과 해당 컬럼들을 삭제한다")
    void deletesConstraintAndColumns() {
      var command = ConstraintFixture.deleteCommand();

      given(deleteConstraintColumnsPort.deleteByConstraintId(any()))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      var inOrderVerifier = inOrder(deleteConstraintColumnsPort, deleteConstraintPort);
      inOrderVerifier.verify(deleteConstraintColumnsPort).deleteByConstraintId(eq(ConstraintFixture.DEFAULT_ID));
      inOrderVerifier.verify(deleteConstraintPort).deleteConstraint(eq(ConstraintFixture.DEFAULT_ID));
    }

    @Test
    @DisplayName("컬럼 삭제 후 제약조건을 삭제한다 (순서 보장)")
    void deletesColumnsBeforeConstraint() {
      var command = ConstraintFixture.deleteCommand("constraint-to-delete");

      given(deleteConstraintColumnsPort.deleteByConstraintId(any()))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      then(deleteConstraintColumnsPort).should().deleteByConstraintId(eq("constraint-to-delete"));
      then(deleteConstraintPort).should().deleteConstraint(eq("constraint-to-delete"));
    }

  }

}
