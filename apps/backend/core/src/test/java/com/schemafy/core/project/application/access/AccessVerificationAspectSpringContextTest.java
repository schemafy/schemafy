package com.schemafy.core.project.application.access;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = AccessVerificationAspectSpringContextTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AccessVerificationAspect Spring context")
class AccessVerificationAspectSpringContextTest {

  @jakarta.annotation.Resource
  private AnnotatedService annotatedService;

  @jakarta.annotation.Resource
  private AccessVerifier accessVerifier;

  @Test
  @DisplayName("프로젝트 어노테이션은 Spring 프록시 경유 호출에서도 동작한다")
  void projectAccess_worksThroughSpringProxy() {
    reset(accessVerifier);
    annotatedService.reset();
    when(accessVerifier.requireProjectAccess(
        eq("project-id"), eq("requester-id"), eq(ProjectRole.EDITOR)))
        .thenReturn(Mono.empty());

    StepVerifier.create(
        annotatedService.project(new ProjectCommand("project-id", "requester-id")))
        .expectNext("project-ok")
        .verifyComplete();

    verify(accessVerifier).requireProjectAccess(
        eq("project-id"), eq("requester-id"), eq(ProjectRole.EDITOR));
    assertThat(annotatedService.invocationCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 어노테이션은 Spring 프록시 경유 호출에서 차단을 적용한다")
  void workspaceAccess_blocksThroughSpringProxy() {
    reset(accessVerifier);
    annotatedService.reset();
    when(accessVerifier.requireWorkspaceAccess(
        eq("workspace-id"), eq("requester-id"), eq(WorkspaceRole.MEMBER)))
        .thenReturn(Mono.error(new DomainException(
            WorkspaceErrorCode.ACCESS_DENIED)));

    StepVerifier.create(
        annotatedService.workspace(new WorkspaceCommand("workspace-id", "requester-id")))
        .expectErrorMatches(
            DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    verify(accessVerifier).requireWorkspaceAccess(
        eq("workspace-id"), eq("requester-id"), eq(WorkspaceRole.MEMBER));
    assertThat(annotatedService.invocationCount()).isZero();
  }

  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  static class TestConfig {

    @Bean
    AccessVerifier accessVerifier() {
      return mock(AccessVerifier.class);
    }

    @Bean
    AccessVerificationAspect accessVerificationAspect(AccessVerifier accessVerifier) {
      return new AccessVerificationAspect(accessVerifier);
    }

    @Bean
    AnnotatedService annotatedService() {
      return new AnnotatedService();
    }

  }

  static class AnnotatedService {

    private final AtomicInteger invocations = new AtomicInteger();

    @RequireProjectAccess(role = ProjectRole.EDITOR)
    public Mono<String> project(ProjectCommand command) {
      invocations.incrementAndGet();
      return Mono.just("project-ok");
    }

    @RequireWorkspaceAccess
    public Mono<String> workspace(WorkspaceCommand command) {
      invocations.incrementAndGet();
      return Mono.just("workspace-ok");
    }

    public void reset() {
      invocations.set(0);
    }

    public int invocationCount() {
      return invocations.get();
    }

  }

  record ProjectCommand(String projectId, String requesterId) {
  }

  record WorkspaceCommand(String workspaceId, String requesterId) {
  }

}
