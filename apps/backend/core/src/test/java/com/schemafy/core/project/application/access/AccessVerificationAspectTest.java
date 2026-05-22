package com.schemafy.core.project.application.access;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
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
    factory.addAspect(new AccessVerificationAspect(
        accessVerifier,
        new ProjectAccessTargetInference(new ProjectAccessResourceRegistry(List.of())),
        null));
    return factory.getProxy();
  }

  private TestService createErdProxy(AccessVerifier accessVerifier, TestService target) {
    ProjectAccessResourceRegistry registry = new ProjectAccessResourceRegistry(
        List.of(new TestTableResourceResolver()));
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.setProxyTargetClass(true);
    factory.addAspect(new AccessVerificationAspect(
        accessVerifier,
        new ProjectAccessTargetInference(registry),
        new ErdProjectContextResolver(registry)));
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

  @Test
  @DisplayName("ERD resource access는 requester가 없으면 실패한다")
  void erdResourceAccess_failsWithoutRequester() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    TestService target = new TestService();
    TestService proxy = createErdProxy(verifier, target);

    StepVerifier.create(proxy.loadTable(new GetTableQuery("table-id")))
        .expectErrorMatches(error -> error instanceof IllegalStateException
            && error.getMessage().equals("Project access requester is missing"))
        .verify();

    assertThat(target.invocations.get()).isZero();
  }

  @Test
  @DisplayName("ERD resource access는 requester context로 프로젝트 접근을 검증한다")
  void erdResourceAccess_usesRequesterContext() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess("project-id", "requester-id", ProjectRole.VIEWER))
        .thenReturn(Mono.empty());
    TestService proxy = createErdProxy(verifier, new TestService());

    StepVerifier.create(proxy.loadTable(new GetTableQuery("table-id"))
        .contextWrite(ProjectAccessRequesterContext.withRequesterId("requester-id")))
        .expectNext("table")
        .verifyComplete();

    verify(verifier)
        .requireProjectAccess(eq("project-id"), eq("requester-id"), eq(ProjectRole.VIEWER));
  }

  @Test
  @DisplayName("ERD resource access는 command requesterId로 프로젝트 접근을 검증한다")
  void erdResourceAccess_usesCommandRequesterId() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess("project-id", "requester-id", ProjectRole.VIEWER))
        .thenReturn(Mono.empty());
    TestService proxy = createErdProxy(verifier, new TestService());

    StepVerifier.create(proxy.loadMemo(
        new MemoAccessCommand("memo-id", "requester-id")))
        .expectNext("memo")
        .verifyComplete();

    verify(verifier)
        .requireProjectAccess(eq("project-id"), eq("requester-id"), eq(ProjectRole.VIEWER));
  }

  @Test
  @DisplayName("ERD resource access는 지정한 requester accessor 이름을 사용할 수 있다")
  void erdResourceAccess_supportsCustomRequesterAccessor() {
    AccessVerifier verifier = mock(AccessVerifier.class);
    when(verifier.requireProjectAccess("project-id", "actor-id", ProjectRole.VIEWER))
        .thenReturn(Mono.empty());
    TestService proxy = createErdProxy(verifier, new TestService());

    StepVerifier.create(proxy.loadAlternateMemo(
        new AlternateMemoAccessCommand("memo-id", "actor-id")))
        .expectNext("alternate-memo")
        .verifyComplete();

    verify(verifier)
        .requireProjectAccess(eq("project-id"), eq("actor-id"), eq(ProjectRole.VIEWER));
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

    @RequireProjectAccess
    public Mono<String> loadTable(GetTableQuery query) {
      invocations.incrementAndGet();
      return Mono.just("table");
    }

    @RequireProjectAccess(target = "memo:memoId")
    public Mono<String> loadMemo(MemoAccessCommand command) {
      invocations.incrementAndGet();
      return Mono.just("memo");
    }

    @RequireProjectAccess(target = "memo:memoId", requesterId = "actorId")
    public Mono<String> loadAlternateMemo(AlternateMemoAccessCommand command) {
      invocations.incrementAndGet();
      return Mono.just("alternate-memo");
    }

  }

  static class TestTableResourceResolver implements ProjectAccessResourceResolver {

    @Override
    public Set<ProjectAccessResourceType> resourceTypes() {
      return Set.of(ProjectAccessResourceType.TABLE, ProjectAccessResourceType.MEMO);
    }

    @Override
    public List<ProjectAccessAccessorRule> accessorRules() {
      return List.of(
          new ProjectAccessAccessorRule("tableId", ProjectAccessResourceType.TABLE),
          new ProjectAccessAccessorRule("memoId", ProjectAccessResourceType.MEMO));
    }

    @Override
    public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
      return Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.PROJECT,
          "project-id"));
    }

  }

  record ProjectCommand(String projectId, String requesterId) {
  }

  record AlternateProjectCommand(String targetProjectId, String actorId) {
  }

  record WorkspaceCommand(String workspaceId, String requesterId) {
  }

  record MemoAccessCommand(String memoId, String requesterId) {
  }

  record AlternateMemoAccessCommand(String memoId, String actorId) {
  }

}
