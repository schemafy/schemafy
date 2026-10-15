package com.schemafy.core.project.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.application.service.ProjectMutationGuard;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DisplayName("프로젝트 변경 동시성 통합 테스트")
class ProjectMutationConcurrencyIntegrationTest
    extends ProjectDomainIntegrationSupport {

  private static final int CONCURRENT_CHANGE_COUNT = 8;

  @Autowired
  private ProjectMutationGuard projectMutationGuard;

  @Autowired
  private DeleteProjectUseCase deleteProjectUseCase;

  @Autowired
  private CreateProjectInvitationUseCase createProjectInvitationUseCase;

  @Autowired
  private UpdateProjectShareLinkUseCase updateProjectShareLinkUseCase;

  @MockitoSpyBean
  private ShareLinkPort shareLinkPort;

  @Autowired
  private UpdateProjectUseCase updateProjectUseCase;

  @Test
  @DisplayName("프로젝트 배타 변경은 진행 중인 하위 변경이 끝날 때까지 대기한다")
  void exclusiveMutation_waitsForOngoingChildCreation() throws Exception {
    Workspace workspace = saveWorkspace("Mutation Lock", "Description");
    Project project = saveProject(workspace, "Mutation Lock Project");
    CountDownLatch mutationLocked = new CountDownLatch(1);
    CountDownLatch releaseMutation = new CountDownLatch(1);
    CountDownLatch exclusiveStarted = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> mutation = executor.submit(() -> projectMutationGuard
          .protectChildCreation(project.getId(), () -> Mono.fromRunnable(() -> {
            mutationLocked.countDown();
            await(releaseMutation);
          }))
          .block());
      assertThat(mutationLocked.await(3, TimeUnit.SECONDS)).isTrue();

      Future<?> exclusive = executor.submit(() -> projectMutationGuard
          .protectProjectMutation(project.getId(), () -> Mono.fromRunnable(
              exclusiveStarted::countDown))
          .block());

      assertThat(exclusiveStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();
      releaseMutation.countDown();
      mutation.get(3, TimeUnit.SECONDS);
      exclusive.get(3, TimeUnit.SECONDS);
      assertThat(exclusiveStarted.getCount()).isZero();
    } finally {
      releaseMutation.countDown();
    }
  }

  @Test
  @DisplayName("최초 공유 링크 생성이 끝날 때까지 프로젝트 삭제가 대기하고 생성된 링크도 정리한다")
  @SuppressWarnings("unchecked")
  void firstShareLinkActivationAndProjectDeletion_leaveNoOrphanLink() throws Exception {
    User admin = signUpUser("admin-share-link-create-delete@test.com", "Admin");
    Workspace workspace = saveWorkspace("Share Link Creation Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Share Link Creation Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    CountDownLatch creationStarted = new CountDownLatch(1);
    Sinks.Empty<Void> releaseCreation = Sinks.empty();
    CountDownLatch deletionStarted = new CountDownLatch(1);

    doAnswer(invocation -> {
      Mono<ShareLink> save = (Mono<ShareLink>) invocation.callRealMethod();
      return Mono.defer(() -> {
        creationStarted.countDown();
        return releaseCreation.asMono().then(save);
      });
    }).when(shareLinkPort).save(any(ShareLink.class));

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<ShareLink> activation = executor.submit(() -> updateProjectShareLinkUseCase
          .updateProjectShareLink(new UpdateProjectShareLinkCommand(
              project.getId(), true, admin.id())).block());
      assertThat(creationStarted.await(3, TimeUnit.SECONDS)).isTrue();

      Future<?> deletion = executor.submit(() -> {
        deletionStarted.countDown();
        deleteProjectUseCase.deleteProject(
            new DeleteProjectCommand(project.getId(), admin.id())).block();
      });
      assertThat(deletionStarted.await(3, TimeUnit.SECONDS)).isTrue();
      boolean deletionWaited;
      try {
        deletion.get(200, TimeUnit.MILLISECONDS);
        deletionWaited = false;
      } catch (TimeoutException expected) {
        deletionWaited = true;
      }

      releaseCreation.tryEmitEmpty();
      ShareLink createdLink = activation.get(5, TimeUnit.SECONDS);
      deletion.get(5, TimeUnit.SECONDS);

      assertThat(projectRepository.findByIdAndDeletedAtIsNull(project.getId()).block()).isNull();
      assertThat(shareLinkRepository.findById(createdLink.getId()).block()).isNull();
      assertThat(shareLinkRepository.findByProjectIdAndNotDeleted(project.getId()).block()).isNull();
      assertThat(deletionWaited).isTrue();
    } finally {
      releaseCreation.tryEmitEmpty();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("프로젝트 삭제가 먼저 완료되면 최초 공유 링크를 생성하지 않는다")
  void firstShareLinkActivationAfterProjectDeletion_isRejected() {
    User admin = signUpUser("admin-share-link-after-delete@test.com", "Admin");
    Workspace workspace = saveWorkspace("Share Link After Delete", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Share Link After Delete Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    deleteProjectUseCase.deleteProject(new DeleteProjectCommand(project.getId(), admin.id())).block();

    StepVerifier.create(updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(project.getId(), true, admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
        .verify();

    assertThat(shareLinkRepository.findByProjectIdAndNotDeleted(project.getId()).block()).isNull();
  }

  @Test
  @DisplayName("프로젝트 수정과 삭제가 동시에 실행되어도 삭제된 프로젝트가 복원되지 않는다")
  void concurrentProjectUpdateAndDelete_doesNotRestoreProject()
      throws InterruptedException {
    User admin = signUpUser("admin-project-update-delete@test.com", "Admin");
    Workspace workspace = saveWorkspace("Update Delete Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    List<Project> projects = new ArrayList<>();
    List<CheckedTask> tasks = new ArrayList<>();

    for (int i = 0; i < CONCURRENT_CHANGE_COUNT; i++) {
      Project project = saveProject(workspace, "Update Delete Race " + i);
      saveProjectMember(project, admin, ProjectRole.ADMIN);
      projects.add(project);
      tasks.add(() -> updateProjectIgnoringDeletion(project, admin));
      tasks.add(() -> deleteProjectUseCase.deleteProject(
          new DeleteProjectCommand(project.getId(), admin.id())).block());
    }

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(tasks);

    assertThat(errors).isEmpty();
    for (Project project : projects) {
      assertThat(projectRepository.findByIdAndDeletedAtIsNull(project.getId()).block())
          .isNull();
    }
  }

  @Test
  @DisplayName("프로젝트 삭제와 초대 생성이 동시에 실행되어도 삭제된 프로젝트에 활성 대기 초대가 남지 않는다")
  void concurrentProjectDeletionAndInvitationCreationLeavesNoActiveInvitation()
      throws InterruptedException {
    User admin = signUpUser("admin-project-invitation-delete@test.com", "Admin");
    User invitee = signUpUser("invitee-project-invitation-delete@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Project Invitation Delete", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    Project project = saveProject(workspace, "Project Invitation Delete");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    List<CheckedTask> tasks = List.of(
        () -> createProjectInvitationIgnoringDeletion(project, invitee, admin),
        () -> deleteProjectIgnoringInvitationCreation(project, admin));

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(tasks);

    assertThat(errors).isEmpty();
    assertThat(projectRepository.findByIdAndDeletedAtIsNull(project.getId()).block()).isNull();
    assertThat(invitationRepository.countByTarget(InvitationType.PROJECT.name(), project.getId())
        .block()).isZero();
  }

  private void updateProjectIgnoringDeletion(Project project, User admin) {
    try {
      updateProjectUseCase.updateProject(new UpdateProjectCommand(
          project.getId(), "Updated", "Updated", admin.id())).block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.NOT_FOUND
          && error.getErrorCode() != ProjectErrorCode.ACCESS_DENIED
          && error.getErrorCode() != ProjectErrorCode.MEMBER_NOT_FOUND) {
        throw error;
      }
    }
  }

  private void createProjectInvitationIgnoringDeletion(
      Project project,
      User invitee,
      User admin) {
    try {
      createProjectInvitationUseCase.createProjectInvitation(
          new CreateProjectInvitationCommand(project.getId(), invitee.email(),
              ProjectRole.EDITOR, admin.id())).block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.NOT_FOUND
          && error.getErrorCode() != ProjectErrorCode.ACCESS_DENIED
          && error.getErrorCode() != ProjectErrorCode.MEMBER_NOT_FOUND) {
        throw error;
      }
    }
  }

  private void deleteProjectIgnoringInvitationCreation(Project project, User admin) {
    try {
      deleteProjectUseCase.deleteProject(
          new DeleteProjectCommand(project.getId(), admin.id())).block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.NOT_FOUND
          && error.getErrorCode() != ProjectErrorCode.ACCESS_DENIED
          && error.getErrorCode() != ProjectErrorCode.MEMBER_NOT_FOUND) {
        throw error;
      }
    }
  }

  private ConcurrentLinkedQueue<Throwable> runConcurrently(
      List<CheckedTask> tasks) throws InterruptedException {
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
