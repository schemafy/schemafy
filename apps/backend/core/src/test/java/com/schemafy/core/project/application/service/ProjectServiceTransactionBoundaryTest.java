package com.schemafy.core.project.application.service;

import java.util.stream.Stream;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 서비스 트랜잭션 경계")
class ProjectServiceTransactionBoundaryTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("servicesUsingLockTransaction")
  @DisplayName("잠금 트랜잭션과 별도의 트랜잭션 경계를 만들지 않는다")
  void doesNotCreateSeparateTransactionBoundary(Class<?> serviceType) {
    assertThat(serviceType.getDeclaredConstructors())
        .allSatisfy(constructor -> assertThat(constructor.getParameterTypes())
            .doesNotContain(TransactionalOperator.class));
  }

  private static Stream<Class<?>> servicesUsingLockTransaction() {
    return Stream.of(
        AcceptProjectInvitationService.class,
        AcceptWorkspaceInvitationService.class,
        CreateProjectInvitationService.class,
        CreateWorkspaceInvitationService.class,
        DeleteWorkspaceService.class,
        LeaveWorkspaceService.class,
        RemoveProjectMemberService.class);
  }

}
