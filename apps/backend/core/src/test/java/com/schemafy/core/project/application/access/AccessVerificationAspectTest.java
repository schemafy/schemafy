package com.schemafy.core.project.application.access;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccessVerificationAspect")
class AccessVerificationAspectTest {

  private TestService createProxy(AccessVerifier accessVerifier, TestService target) {
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.setProxyTargetClass(true);
    factory.addAspect(new AccessVerificationAspect(accessVerifier));
    return factory.getProxy();
  }

  @Test
  @DisplayName("프로젝트 어노테이션은 기본 accessor로 requesterId와 projectId를 추출한다")
  void projectAccess_extractsDefaultAccessors() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess(anyString(), anyString(), any(ProjectRole.class)))
        .thenReturn(Mono.empty());
    TestService proxy = createProxy(verifier, new TestService());

    StepVerifier.create(proxy.loadProject(new ProjectCommand("project-id", "requester-id")))
        .expectNext("project")
        .verifyComplete();

    verify(verifier)
        .requireProjectAccess(eq("project-id"), eq("requester-id"), eq(ProjectRole.VIEWER));
  }

  @Test
  @DisplayName("프로젝트 어노테이션은 지정한 accessor 이름을 사용할 수 있다")
  void projectAccess_supportsCustomAccessors() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess(anyString(), anyString(), any(ProjectRole.class)))
        .thenReturn(Mono.empty());
    TestService proxy = createProxy(verifier, new TestService());

    StepVerifier.create(proxy.loadAlternateProject(
        new AlternateProjectCommand("project-id", "requester-id")))
        .expectNext("alternate-project")
        .verifyComplete();

    verify(verifier)
        .requireProjectAccess(eq("project-id"), eq("requester-id"), eq(ProjectRole.VIEWER));
  }

  @Test
  @DisplayName("워크스페이스 어노테이션은 Flux 반환도 검증 후 통과시킨다")
  void workspaceAccess_supportsFlux() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireWorkspaceAccess(anyString(), anyString(), any(WorkspaceRole.class)))
        .thenReturn(Mono.empty());
    TestService proxy = createProxy(verifier, new TestService());

    StepVerifier.create(proxy.workspaceStream(new WorkspaceCommand("workspace-id", "requester-id")))
        .expectNext("workspace-1", "workspace-2")
        .verifyComplete();

    verify(verifier)
        .requireWorkspaceAccess(eq("workspace-id"), eq("requester-id"), eq(WorkspaceRole.MEMBER));
  }

  @Test
  @DisplayName("검증 실패 시 대상 메서드는 실행되지 않는다")
  void verificationFailure_preventsMethodExecution() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess(eq("project-id"), eq("requester-id"), eq(ProjectRole.ADMIN)))
        .thenReturn(Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED)));
    TestService target = new TestService();
    TestService proxy = createProxy(verifier, target);

    StepVerifier.create(proxy.updateProject(new ProjectCommand("project-id", "requester-id")))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
        .verify();

    assertThat(target.invocations.get()).isZero();
  }

  @Test
  @DisplayName("지원하지 않는 시그니처는 IllegalStateException으로 실패한다")
  void unsupportedSignature_fails() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    TestService proxy = createProxy(verifier, new TestService());

    StepVerifier.create(proxy.invalid("project-id", "requester-id"))
        .expectError(IllegalStateException.class)
        .verify();
  }

  static class TestService {

    private final AtomicInteger invocations = new AtomicInteger();

    @RequireProjectAccess
    public Mono<String> loadProject(ProjectCommand command) {
      invocations.incrementAndGet();
      return Mono.just("project");
    }

    @RequireProjectAccess(role = ProjectRole.ADMIN)
    public Mono<String> updateProject(ProjectCommand command) {
      invocations.incrementAndGet();
      return Mono.just("updated");
    }

    @RequireProjectAccess(projectId = "targetProjectId", requesterId = "actorId")
    public Mono<String> loadAlternateProject(AlternateProjectCommand command) {
      invocations.incrementAndGet();
      return Mono.just("alternate-project");
    }

    @RequireWorkspaceAccess
    public Flux<String> workspaceStream(WorkspaceCommand command) {
      invocations.incrementAndGet();
      return Flux.just("workspace-1", "workspace-2");
    }

    @RequireProjectAccess
    public Mono<String> invalid(String projectId, String requesterId) {
      invocations.incrementAndGet();
      return Mono.just("invalid");
    }

  }

  record ProjectCommand(String projectId, String requesterId) {
  }

  record AlternateProjectCommand(String targetProjectId, String actorId) {
  }

  record WorkspaceCommand(String workspaceId, String requesterId) {
  }

}
