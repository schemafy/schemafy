package com.schemafy.core.project.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.service.ProjectMutationGuard;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 변경 동시성 통합 테스트")
class ProjectMutationConcurrencyIntegrationTest
    extends ProjectDomainIntegrationSupport {

  private static final int CONCURRENT_CHANGE_COUNT = 8;

  @Autowired
  private ProjectMutationGuard projectMutationGuard;

  @Autowired
  private LeaveProjectUseCase leaveProjectUseCase;

  @Autowired
  private DeleteProjectUseCase deleteProjectUseCase;

  @Autowired
  private AccessShareLinkUseCase accessShareLinkUseCase;

  @Autowired
  private DeleteShareLinkUseCase deleteShareLinkUseCase;

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
          .protectWorkspaceAndProjectMutation(project.getId(), () -> Mono.fromRunnable(
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
  @DisplayName("공유 링크 접근과 삭제가 동시에 실행되어도 삭제 뒤 접근 정보가 변경되지 않는다")
  void concurrentShareLinkAccessAndDelete_stopsMetadataUpdatesAfterDelete()
      throws InterruptedException {
    User admin = signUpUser("admin-share-link-race@test.com", "Admin");
    Workspace workspace = saveWorkspace("Share Link Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Share Link Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    List<ShareLink> links = new ArrayList<>();
    List<CheckedTask> tasks = new ArrayList<>();

    for (int i = 0; i < CONCURRENT_CHANGE_COUNT; i++) {
      ShareLink link = saveShareLink(project);
      links.add(link);
      tasks.add(() -> accessShareLinkIgnoringDeletion(link));
      tasks.add(() -> deleteShareLinkUseCase.deleteShareLink(
          new DeleteShareLinkCommand(project.getId(), link.getId(), admin.id()))
          .block());
    }

    ConcurrentLinkedQueue<Throwable> errors = runConcurrently(tasks);

    assertThat(errors).isEmpty();
    for (ShareLink link : links) {
      ShareLink deleted = shareLinkRepository.findById(link.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();
      long accessCountAfterDelete = deleted.getAccessCount();
      shareLinkRepository.incrementAccessCount(link.getId()).block();
      assertThat(shareLinkRepository.findById(link.getId()).block()
          .getAccessCount()).isEqualTo(accessCountAfterDelete);
    }
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
      assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block())
          .isNull();
    }
  }

  private void accessShareLinkIgnoringDeletion(ShareLink link) {
    try {
      accessShareLinkUseCase.accessShareLink(new AccessShareLinkQuery(
          link.getCode(), null, "127.0.0.1", "동시성 테스트")).block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ShareLinkErrorCode.NOT_FOUND
          && error.getErrorCode() != ShareLinkErrorCode.INVALID_LINK) {
        throw error;
      }
    }
  }

  private void updateProjectIgnoringDeletion(Project project, User admin) {
    try {
      updateProjectUseCase.updateProject(new UpdateProjectCommand(
          project.getId(), "Updated", "Updated", admin.id())).block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.NOT_FOUND
          && error.getErrorCode() != ProjectErrorCode.ACCESS_DENIED) {
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

    try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
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
      ready.await();
      start.countDown();
      done.await();
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
