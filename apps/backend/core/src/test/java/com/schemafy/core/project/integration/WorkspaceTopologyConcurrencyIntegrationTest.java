package com.schemafy.core.project.integration;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.application.service.WorkspaceMutationGuard;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("워크스페이스 토폴로지 동시성 통합 테스트")
class WorkspaceTopologyConcurrencyIntegrationTest
    extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateProjectUseCase createProjectUseCase;

  @Autowired
  private DeleteWorkspaceUseCase deleteWorkspaceUseCase;

  @Autowired
  private AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;

  @Autowired
  private UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;

  @Autowired
  private WorkspaceMutationGuard workspaceMutationGuard;

  @Test
  @DisplayName("동시에 상호 강등해도 활성 워크스페이스 관리자는 한 명 유지된다")
  void concurrentMutualAdminDemotionsLeaveExactlyOneActiveAdmin()
      throws InterruptedException {
    User firstAdmin = signUpUser("topology-first-admin@test.com", "First Admin");
    User secondAdmin = signUpUser("topology-second-admin@test.com", "Second Admin");
    Workspace workspace = saveWorkspace("Topology Demotion", "Description");
    saveWorkspaceMember(workspace, firstAdmin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, secondAdmin, WorkspaceRole.ADMIN);
    AtomicInteger successes = new AtomicInteger();

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(List.of(
        () -> {
          updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
              new UpdateWorkspaceMemberRoleCommand(
                  workspace.getId(), secondAdmin.id(), WorkspaceRole.MEMBER, firstAdmin.id()))
              .contextWrite(ProjectAccessRequesterContext.withRequesterId(firstAdmin.id()))
              .block();
          successes.incrementAndGet();
        },
        () -> {
          updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
              new UpdateWorkspaceMemberRoleCommand(
                  workspace.getId(), firstAdmin.id(), WorkspaceRole.MEMBER, secondAdmin.id()))
              .contextWrite(ProjectAccessRequesterContext.withRequesterId(secondAdmin.id()))
              .block();
          successes.incrementAndGet();
        }));

    assertThat(successes).hasValue(1);
    assertThat(errors).hasSize(1);
    assertThat(errors.peek()).isInstanceOf(DomainException.class);
    assertThat((DomainException) errors.peek())
        .extracting(DomainException::getErrorCode)
        .isIn(WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE,
            WorkspaceErrorCode.ADMIN_REQUIRED);
    assertThat(workspaceMemberRepository.countByWorkspaceIdAndRoleAndNotDeleted(
        workspace.getId(), WorkspaceRole.ADMIN.name()).block()).isEqualTo(1L);
  }

  @Test
  @DisplayName("프로젝트 생성과 워크스페이스 멤버 추가를 동시에 요청해도 새 멤버가 새 프로젝트에 활성 상태로 존재한다")
  void concurrentProjectCreationAndMemberAdditionPropagatesNewMemberToNewProject()
      throws InterruptedException {
    User admin = signUpUser("topology-project-admin@test.com", "Admin");
    User newMember = signUpUser("topology-new-member@test.com", "New Member");
    Workspace workspace = saveWorkspace("Topology Project", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    AtomicReference<ProjectDetail> createdProject = new AtomicReference<>();

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(List.of(
        () -> createdProject.set(createProjectUseCase.createProject(
            new CreateProjectCommand(
                workspace.getId(), "Concurrent Project", "Description", admin.id()))
            .contextWrite(ProjectAccessRequesterContext.withRequesterId(admin.id()))
            .block()),
        () -> addWorkspaceMemberUseCase.addWorkspaceMember(
            new AddWorkspaceMemberCommand(
                workspace.getId(), newMember.email(), WorkspaceRole.MEMBER, admin.id()))
            .contextWrite(ProjectAccessRequesterContext.withRequesterId(admin.id()))
            .block()));

    assertThat(errors).isEmpty();
    assertThat(createdProject).hasValueSatisfying(project -> {
      ProjectMember membership = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.project().getId(), newMember.id())
          .block();
      assertThat(membership).isNotNull();
      assertThat(membership.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    });
  }

  @Test
  @DisplayName("워크스페이스 삭제와 프로젝트 생성을 동시에 요청해도 삭제된 워크스페이스 아래 활성 프로젝트가 남지 않는다")
  void concurrentWorkspaceDeletionAndProjectCreationLeavesNoActiveProjectBelowDeletedWorkspace()
      throws InterruptedException {
    User admin = signUpUser("topology-delete-create-admin@test.com", "Admin");
    Workspace workspace = saveWorkspace("Topology Delete Create", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(List.of(
        () -> deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(
            workspace.getId(), admin.id()))
            .contextWrite(ProjectAccessRequesterContext.withRequesterId(admin.id()))
            .block(),
        () -> createProjectUseCase.createProject(new CreateProjectCommand(
            workspace.getId(), "Concurrent Project", "Description", admin.id()))
            .contextWrite(ProjectAccessRequesterContext.withRequesterId(admin.id()))
            .block()));

    assertThat(errors).allMatch(DomainException.class::isInstance);
    assertThat(workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId()).block())
        .isNull();
    assertThat(projectRepository.countByWorkspaceIdAndNotDeleted(workspace.getId()).block())
        .isZero();
  }

  @Test
  @DisplayName("공유 토폴로지 작업이 끝난 뒤 배타 토폴로지 변경이 진행된다")
  void sharedTopologyWorkCompletesBeforeExclusiveTopologyChangeProceeds()
      throws Exception {
    Workspace workspace = saveWorkspace("Topology Guard", "Description");
    CountDownLatch sharedReady = new CountDownLatch(1);
    CountDownLatch sharedStart = new CountDownLatch(1);
    CountDownLatch sharedActionStarted = new CountDownLatch(1);
    CountDownLatch releaseShared = new CountDownLatch(1);
    CountDownLatch exclusiveReady = new CountDownLatch(1);
    CountDownLatch exclusiveStart = new CountDownLatch(1);
    CountDownLatch exclusiveActionStarted = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> shared = executor.submit(() -> {
        sharedReady.countDown();
        await(sharedStart);
        try {
          workspaceMutationGuard.protectShared(workspace.getId(),
              () -> Mono.fromRunnable(() -> {
                sharedActionStarted.countDown();
                await(releaseShared);
              }))
              .block();
        } finally {
          done.countDown();
        }
      });
      assertThat(sharedReady.await(3, TimeUnit.SECONDS)).isTrue();
      sharedStart.countDown();
      assertThat(sharedActionStarted.await(3, TimeUnit.SECONDS)).isTrue();

      Future<?> exclusive = executor.submit(() -> {
        exclusiveReady.countDown();
        await(exclusiveStart);
        try {
          workspaceMutationGuard.protectExclusive(workspace.getId(),
              () -> Mono.fromRunnable(exclusiveActionStarted::countDown))
              .block();
        } finally {
          done.countDown();
        }
      });
      assertThat(exclusiveReady.await(3, TimeUnit.SECONDS)).isTrue();
      exclusiveStart.countDown();
      assertThat(exclusiveActionStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();

      releaseShared.countDown();
      assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
      shared.get(3, TimeUnit.SECONDS);
      exclusive.get(3, TimeUnit.SECONDS);
      assertThat(exclusiveActionStarted.getCount()).isZero();
    } finally {
      sharedStart.countDown();
      exclusiveStart.countDown();
      releaseShared.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
    }
  }

  private ConcurrentLinkedQueue<Throwable> runConcurrently(List<CheckedTask> tasks)
      throws InterruptedException {
    CountDownLatch ready = new CountDownLatch(tasks.size());
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(tasks.size());
    ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
    ExecutorService executor = Executors.newFixedThreadPool(tasks.size());

    try {
      for (CheckedTask task : tasks) {
        executor.submit(() -> {
          ready.countDown();
          await(start);
          try {
            task.run();
          } catch (Throwable error) {
            errors.add(error);
          } finally {
            done.countDown();
          }
        });
      }
      assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
    }
    return errors;
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(error);
    }
  }

  @FunctionalInterface
  private interface CheckedTask {

    void run();

  }

}
