package com.schemafy.api.user.integration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.application.port.in.WithdrawUserCommand;
import com.schemafy.core.user.application.port.in.WithdrawUserUseCase;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("MariaDB 계정 탈퇴 동시성 통합 테스트")
class WithdrawUserConcurrencyIntegrationTest extends ProjectHttpTestSupport {

  private static final String DATABASE = "schemafy";
  private static final String DATABASE_USER = "schemafy";
  private static final String DATABASE_PASSWORD = "schemafy-test";

  @Container
  private static final GenericContainer<?> MARIADB = new GenericContainer<>(
      DockerImageName.parse("mariadb:11.4.13"))
      .withEnv("MARIADB_DATABASE", DATABASE)
      .withEnv("MARIADB_USER", DATABASE_USER)
      .withEnv("MARIADB_PASSWORD", DATABASE_PASSWORD)
      .withEnv("MARIADB_ROOT_PASSWORD", "schemafy-root-test")
      .withExposedPorts(3306);

  @Autowired
  private WithdrawUserUseCase withdrawUserUseCase;

  @Autowired
  private AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;

  @Autowired
  private CreateProjectInvitationUseCase createProjectInvitationUseCase;

  @Autowired
  private AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;

  @Autowired
  private UpdateProjectMemberRoleUseCase updateProjectMemberRoleUseCase;

  @MockitoSpyBean
  private WorkspacePort workspacePort;

  @MockitoSpyBean
  private ProjectPort projectPort;

  @MockitoSpyBean
  private InvitationPort invitationPort;

  @MockitoSpyBean
  private RevokeMcpTokenPort revokeMcpTokenPort;

  @DynamicPropertySource
  static void configureMariaDb(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> "r2dbc:mariadb://"
        + MARIADB.getHost() + ":" + MARIADB.getMappedPort(3306) + "/"
        + DATABASE);
    registry.add("spring.r2dbc.username", () -> DATABASE_USER);
    registry.add("spring.r2dbc.password", () -> DATABASE_PASSWORD);
    registry.add("spring.sql.init.schema-locations",
        () -> "classpath:ddl/mariadb/**/*.sql");
  }

  @BeforeEach
  void cleanFixtures() {
    cleanupProjectFixtures().block();
  }

  @Test
  @DisplayName("동시에 탈퇴하는 두 ADMIN 중 하나만 나가고 하나는 마지막 ADMIN 오류를 받는다")
  void concurrentSoleAdminWithdrawalsLeaveOneAdmin() throws Exception {
    User firstAdmin = createUser("withdraw-admin-one@test.com", "Admin one");
    User secondAdmin = createUser("withdraw-admin-two@test.com", "Admin two");
    User member = createUser("withdraw-member@test.com", "Member");
    Workspace workspace = saveWorkspace("Concurrent withdrawal", "description");
    addWorkspaceMember(workspace.getId(), firstAdmin.id(), WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), secondAdmin.id(), WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), member.id(), WorkspaceRole.MEMBER);

    CountDownLatch bothRequestsReachedWorkspaceLock = new CountDownLatch(2);
    CountDownLatch releaseWorkspaceLocks = new CountDownLatch(1);
    doAnswer(invocation -> {
      bothRequestsReachedWorkspaceLock.countDown();
      if (!releaseWorkspaceLocks.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting to release lock barrier");
      }
      return invocation.callRealMethod();
    }).when(workspacePort).findByIdAndNotDeletedForUpdate(eq(workspace.getId()));

    CompletableFuture<Throwable> firstWithdrawal = CompletableFuture.supplyAsync(
        () -> withdraw(firstAdmin.id()));
    CompletableFuture<Throwable> secondWithdrawal = CompletableFuture.supplyAsync(
        () -> withdraw(secondAdmin.id()));

    try {
      assertThat(bothRequestsReachedWorkspaceLock.await(10, TimeUnit.SECONDS))
          .as("both requests collected their candidate scopes before locking")
          .isTrue();
    } finally {
      releaseWorkspaceLocks.countDown();
    }

    List<Throwable> outcomes = CompletableFuture.allOf(firstWithdrawal,
        secondWithdrawal)
        .thenApply(ignored -> Stream.of(firstWithdrawal.join(),
            secondWithdrawal.join()).toList())
        .get(20, TimeUnit.SECONDS);
    List<Throwable> failures = outcomes.stream()
        .filter(error -> error != null)
        .toList();

    assertThat(outcomes.stream().filter(error -> error == null)).hasSize(1);
    assertThat(failures).hasSize(1);
    assertThat(DomainException.hasErrorCode(
        WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE).test(failures.getFirst()))
        .isTrue();
    assertThat(workspaceMemberRepository.countByWorkspaceIdAndRoleAndDeletedAtIsNull(
        workspace.getId(), WorkspaceRole.ADMIN.name()).block()).isEqualTo(1L);
    assertThat(Stream.of(firstAdmin, secondAdmin)
        .map(user -> findUserByIdPort.findUserById(user.id()).block().status())
        .filter(status -> status == UserStatus.INACTIVE))
        .hasSize(1);
  }

  @Test
  @DisplayName("후속 DB 변경이 실패하면 앞서 취소한 초대도 rollback한다")
  void rollsBackInvitationCancellationWhenMcpRevocationFails() {
    User user = createUser("withdraw-rollback@test.com", "Rollback user");
    Workspace workspace = saveWorkspace("Rollback workspace", "description");
    addWorkspaceMember(workspace.getId(), user.id(), WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace.getId(),
        "pending@test.com", WorkspaceRole.MEMBER, user.id());
    doAnswer(invocationCall -> Mono.error(
        new IllegalStateException("injected MCP revoke failure")))
        .when(revokeMcpTokenPort)
        .revokeAllMcpTokensByUserId(eq(user.id()), ArgumentMatchers.any());

    StepVerifier.create(withdrawUserUseCase.withdrawUser(
        new WithdrawUserCommand(user.id())))
        .expectErrorMessage("injected MCP revoke failure")
        .verify();

    assertThat(invitationRepository.findById(invitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
    assertThat(findUserByIdPort.findUserById(user.id()).block().status())
        .isEqualTo(UserStatus.ACTIVE);
    assertThat(workspaceRepository
        .findByIdAndDeletedAtIsNull(workspace.getId()).block())
        .isNotNull();
  }

  @Test
  @DisplayName("transaction timeout 뒤에는 이미 실행된 초대 변경도 rollback한다")
  void rollsBackInvitationCancellationWhenTransactionTimesOut() {
    User user = createUser("withdraw-timeout@test.com", "Timeout user");
    Workspace workspace = saveWorkspace("Timeout workspace", "description");
    addWorkspaceMember(workspace.getId(), user.id(), WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace.getId(),
        "pending-timeout@test.com", WorkspaceRole.MEMBER, user.id());
    doAnswer(invocationCall -> {
      @SuppressWarnings("unchecked")
      Mono<Long> cancellation = (Mono<Long>) invocationCall.callRealMethod();
      return cancellation.then(Mono.never());
    }).when(invitationPort).cancelPendingByInvitedBy(user.id());

    StepVerifier.create(withdrawUserUseCase.withdrawUser(
        new WithdrawUserCommand(user.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            UserErrorCode.WITHDRAWAL_TIMEOUT))
        .verify(Duration.ofSeconds(15));

    assertThat(invitationRepository.findById(invitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
    assertThat(findUserByIdPort.findUserById(user.id()).block().status())
        .isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("탈퇴 scope 탐색 뒤 workspace membership이 추가되면 재시도 후 새 관계까지 정리한다")
  void retriesWhenWorkspaceMembershipIsAddedAfterScopeDiscovery() throws Exception {
    User user = createUser("withdraw-workspace-race@test.com", "Workspace race");
    User existingAdmin = createUser("withdraw-workspace-admin@test.com", "Existing admin");
    User newWorkspaceAdmin = createUser("withdraw-workspace-new-admin@test.com", "New admin");

    Workspace workspace = saveWorkspace("Workspace race", "description");
    addWorkspaceMember(workspace.getId(), existingAdmin.id(), WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), user.id(), WorkspaceRole.MEMBER);
    var project = saveProject(workspace.getId(), "Workspace race project", "description");
    addProjectMember(project.getId(), existingAdmin.id(), ProjectRole.ADMIN);
    addProjectMember(project.getId(), user.id(), ProjectRole.VIEWER);

    Workspace newWorkspace = saveWorkspace("Workspace added during withdrawal", "description");
    addWorkspaceMember(newWorkspace.getId(), newWorkspaceAdmin.id(), WorkspaceRole.ADMIN);
    var newProject = saveProject(newWorkspace.getId(), "Added project", "description");
    addProjectMember(newProject.getId(), newWorkspaceAdmin.id(), ProjectRole.ADMIN);

    CountDownLatch firstScopeReady = new CountDownLatch(1);
    CompletableFuture<Void> releaseFirstScope = new CompletableFuture<>();
    doAnswer(invocation -> delayedFirstWorkspaceScope(
        invocation,
        user.id(),
        firstScopeReady,
        releaseFirstScope))
        .when(workspacePort).findByUserId(eq(user.id()));

    CompletableFuture<Throwable> withdrawal = CompletableFuture.supplyAsync(
        () -> withdraw(user.id()));
    awaitLatch(firstScopeReady);

    CompletableFuture<Void> addMembership = CompletableFuture.runAsync(() -> addWorkspaceMemberUseCase
        .addWorkspaceMember(new AddWorkspaceMemberCommand(
            newWorkspace.getId(),
            user.email(),
            WorkspaceRole.MEMBER,
            newWorkspaceAdmin.id()))
        .block(Duration.ofSeconds(15)));
    try {
      addMembership.get(20, TimeUnit.SECONDS);
      releaseFirstScope.complete(null);

      assertThat(withdrawal.get(20, TimeUnit.SECONDS)).isNull();
      assertInactiveWithoutActiveRelations(user, newWorkspace, newProject);
    } finally {
      releaseFirstScope.complete(null);
    }
  }

  @Test
  @DisplayName("탈퇴 scope 탐색 뒤 project-only membership이 수락되면 재시도 후 관계를 정리한다")
  void retriesWhenProjectOnlyMembershipIsAcceptedAfterScopeDiscovery() throws Exception {
    User user = createUser("withdraw-project-race@test.com", "Project race");
    User existingAdmin = createUser("withdraw-project-admin@test.com", "Existing admin");
    User projectAdmin = createUser("withdraw-project-new-admin@test.com", "Project admin");

    Workspace workspace = saveWorkspace("Project race", "description");
    addWorkspaceMember(workspace.getId(), existingAdmin.id(), WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), user.id(), WorkspaceRole.MEMBER);
    var project = saveProject(workspace.getId(), "Project race project", "description");
    addProjectMember(project.getId(), existingAdmin.id(), ProjectRole.ADMIN);
    addProjectMember(project.getId(), user.id(), ProjectRole.VIEWER);

    Workspace projectOnlyWorkspace = saveWorkspace("Project-only workspace", "description");
    addWorkspaceMember(projectOnlyWorkspace.getId(), projectAdmin.id(), WorkspaceRole.ADMIN);
    var projectOnly = saveProject(projectOnlyWorkspace.getId(), "Project-only project",
        "description");
    addProjectMember(projectOnly.getId(), projectAdmin.id(), ProjectRole.ADMIN);
    Invitation invitation = createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(
            projectOnly.getId(),
            user.email(),
            ProjectRole.VIEWER,
            projectAdmin.id()))
        .block(Duration.ofSeconds(15));

    CountDownLatch firstScopeReady = new CountDownLatch(1);
    CompletableFuture<Void> releaseFirstScope = new CompletableFuture<>();
    doAnswer(invocation -> delayedFirstProjectScope(
        invocation,
        user.id(),
        firstScopeReady,
        releaseFirstScope))
        .when(projectPort)
        .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(eq(user.id()));

    CompletableFuture<Throwable> withdrawal = CompletableFuture.supplyAsync(
        () -> withdraw(user.id()));
    awaitLatch(firstScopeReady);

    CompletableFuture<Void> acceptMembership = CompletableFuture.runAsync(() -> acceptProjectInvitationUseCase
        .acceptProjectInvitation(
            new AcceptProjectInvitationCommand(invitation.getId(), user.id()))
        .block(Duration.ofSeconds(15)));
    try {
      acceptMembership.get(20, TimeUnit.SECONDS);
      releaseFirstScope.complete(null);

      assertThat(withdrawal.get(20, TimeUnit.SECONDS)).isNull();
      assertThat(projectRepository.findByIdAndDeletedAtIsNull(projectOnly.getId()).block())
          .isNotNull();
      assertThat(projectMemberRepository.findByProjectIdAndUserIdAndDeletedAtIsNull(
          projectOnly.getId(), user.id()).block()).isNull();
      assertThat(findUserByIdPort.findUserById(user.id()).block().status())
          .isEqualTo(UserStatus.INACTIVE);
    } finally {
      releaseFirstScope.complete(null);
    }
  }

  private Throwable withdraw(String userId) {
    try {
      withdrawUserUseCase.withdrawUser(new WithdrawUserCommand(userId))
          .block(Duration.ofSeconds(15));
      return null;
    } catch (Throwable error) {
      return error;
    }
  }

  @SuppressWarnings("unchecked")
  private Flux<Workspace> delayedFirstWorkspaceScope(
      InvocationOnMock invocation,
      String userId,
      CountDownLatch scopeReady,
      CompletableFuture<Void> releaseScope) {
    if (!userId.equals(invocation.getArgument(0, String.class))) {
      return callRealMethod(invocation);
    }
    return Flux.defer(() -> this.<Workspace>callRealMethod(invocation)
        .concatWith(Flux.defer(() -> {
          scopeReady.countDown();
          return Mono.fromFuture(releaseScope).thenMany(Flux.<Workspace>empty());
        })));
  }

  @SuppressWarnings("unchecked")
  private Flux<Project> delayedFirstProjectScope(
      InvocationOnMock invocation,
      String userId,
      CountDownLatch scopeReady,
      CompletableFuture<Void> releaseScope) {
    if (!userId.equals(invocation.getArgument(0, String.class))) {
      return callRealMethod(invocation);
    }
    return Flux.defer(() -> this.<Project>callRealMethod(invocation)
        .concatWith(Flux.defer(() -> {
          scopeReady.countDown();
          return Mono.fromFuture(releaseScope).thenMany(Flux.<Project>empty());
        })));
  }

  @SuppressWarnings("unchecked")
  private <T> Flux<T> callRealMethod(
      InvocationOnMock invocation) {
    try {
      return (Flux<T>) invocation.callRealMethod();
    } catch (Throwable error) {
      throw new AssertionError("Real scope query failed", error);
    }
  }

  private void assertInactiveWithoutActiveRelations(
      User user,
      Workspace workspace,
      Project project) {
    assertThat(findUserByIdPort.findUserById(user.id()).block().status())
        .isEqualTo(UserStatus.INACTIVE);
    assertThat(workspaceMemberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(
        workspace.getId(), user.id()).block()).isNull();
    assertThat(projectMemberRepository.findByProjectIdAndUserIdAndDeletedAtIsNull(
        project.getId(), user.id()).block()).isNull();
  }

  private long countRows(String table, String column, String value) {
    return databaseClient.sql(
        "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :value")
        .bind("value", value)
        .map((row, metadata) -> ((Number) row.get(0)).longValue())
        .one()
        .block();
  }

  private void awaitLatch(CountDownLatch latch) {
    try {
      assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for concurrency barrier", error);
    }
  }

}
