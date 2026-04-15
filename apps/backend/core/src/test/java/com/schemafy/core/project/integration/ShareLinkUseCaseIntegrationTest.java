package com.schemafy.core.project.integration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.core.project.application.port.in.CreateShareLinkCommand;
import com.schemafy.core.project.application.port.in.CreateShareLinkUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinksQuery;
import com.schemafy.core.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.core.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.core.project.application.port.in.RevokeShareLinkUseCase;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("ShareLink usecase integration")
class ShareLinkUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private AccessShareLinkUseCase accessShareLinkUseCase;

  @Autowired
  private CreateShareLinkUseCase createShareLinkUseCase;

  @Autowired
  private GetShareLinksUseCase getShareLinksUseCase;

  @Autowired
  private GetShareLinkUseCase getShareLinkUseCase;

  @Autowired
  private RevokeShareLinkUseCase revokeShareLinkUseCase;

  @Autowired
  private DeleteShareLinkUseCase deleteShareLinkUseCase;

  @Autowired
  private DeleteProjectUseCase deleteProjectUseCase;

  @Test
  @DisplayName("create/get/list share links for project admins")
  void createGetAndListShareLinks() {
    User admin = signUpUser("admin-share@test.com", "Admin");
    var workspace = saveWorkspace("Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    ShareLink created = createShareLinkUseCase.createShareLink(
        new CreateShareLinkCommand(project.getId(), admin.id()))
        .block();

    ShareLink fetched = getShareLinkUseCase.getShareLink(
        new GetShareLinkQuery(project.getId(), created.getId(), admin.id()))
        .block();
    PageResult<ShareLink> listed = getShareLinksUseCase.getShareLinks(
        new GetShareLinksQuery(project.getId(), admin.id(), 0, 10))
        .block();

    assertThat(fetched.getId()).isEqualTo(created.getId());
    assertThat(listed.content()).extracting(ShareLink::getId).contains(created.getId());
  }

  @Test
  @DisplayName("accessShareLink returns project and increments access count")
  void accessShareLink_incrementsAccessCount() {
    User admin = signUpUser("admin-share-access@test.com", "Admin");
    var workspace = saveWorkspace("Access WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Access Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    Project accessedProject = accessShareLinkUseCase.accessShareLink(new AccessShareLinkQuery(
        link.getCode(),
        admin.id(),
        "127.0.0.1",
        "JUnit"))
        .block();

    ShareLink updatedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(accessedProject.getId()).isEqualTo(project.getId());
    assertThat(updatedLink.getAccessCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("삭제된 공유 링크는 접근 카운트를 증가시키지 않는다")
  void incrementAccessCount_ignoresSoftDeletedShareLink() {
    User admin = signUpUser("admin-share-deleted-access@test.com", "Admin");
    var workspace = saveWorkspace("Deleted Access WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Deleted Access Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);
    deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(
        project.getId(),
        link.getId(),
        admin.id())).block();

    shareLinkRepository.incrementAccessCount(link.getId()).block();

    ShareLink updatedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(updatedLink.getAccessCount()).isZero();
    assertThat(updatedLink.getLastAccessedAt()).isNull();
  }

  @Test
  @DisplayName("accessShareLink rejects revoked or expired links")
  void accessShareLink_rejectsInvalidLinks() {
    User admin = signUpUser("admin-share-invalid@test.com", "Admin");
    var workspace = saveWorkspace("Invalid WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Invalid Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink revokedLink = saveShareLink(project);
    revokeShareLink(revokedLink.getId());
    ShareLink expiredLink = saveShareLink(project, Instant.now().minus(1, ChronoUnit.DAYS));

    StepVerifier.create(accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(revokedLink.getCode(), null, null, null)))
        .expectErrorMatches(DomainException.hasErrorCode(ShareLinkErrorCode.INVALID_LINK))
        .verify();

    StepVerifier.create(accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(expiredLink.getCode(), null, null, null)))
        .expectErrorMatches(DomainException.hasErrorCode(ShareLinkErrorCode.INVALID_LINK))
        .verify();
  }

  @Test
  @DisplayName("revokeShareLink and deleteShareLink update lifecycle state")
  void revokeAndDeleteShareLink() {
    User admin = signUpUser("admin-share-delete@test.com", "Admin");
    var workspace = saveWorkspace("Delete Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Delete Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    ShareLink revoked = revokeShareLinkUseCase.revokeShareLink(
        new RevokeShareLinkCommand(project.getId(), link.getId(), admin.id()))
        .block();
    deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(
        project.getId(),
        link.getId(),
        admin.id())).block();

    ShareLink deleted = shareLinkRepository.findById(link.getId()).block();

    assertThat(revoked.getIsRevoked()).isTrue();
    assertThat(deleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("프로젝트 삭제와 공유 링크 무효화가 동시에 실행되어도 공유 링크는 삭제 상태로 수렴한다")
  void concurrentProjectDeleteAndShareLinkRevoke_deletesShareLink()
      throws InterruptedException {
    User admin = signUpUser("admin-share-project-revoke-race@test.com", "Admin");
    var workspace = saveWorkspace("Share Project Revoke Race WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project Revoke Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> deleteProjectUseCase.deleteProject(
            new DeleteProjectCommand(project.getId(), admin.id())).block(),
        () -> {
          try {
            revokeShareLinkUseCase.revokeShareLink(new RevokeShareLinkCommand(
                project.getId(), link.getId(), admin.id())).block();
          } catch (DomainException error) {
            ignoreExpectedShareLinkLifecycleRaceError(error);
          }
        });

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project deletedProject = projectRepository.findById(project.getId()).block();
    ShareLink deletedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(deletedProject).isNotNull();
    assertThat(deletedProject.isDeleted()).isTrue();
    assertThat(deletedLink).isNotNull();
    assertThat(deletedLink.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("공유 링크 접근과 프로젝트 삭제가 동시에 실행되어도 공유 링크는 삭제 상태로 수렴한다")
  void concurrentProjectDeleteAndShareLinkAccess_deletesShareLink()
      throws InterruptedException {
    User admin = signUpUser("admin-share-project-access-race@test.com",
        "Admin");
    var workspace = saveWorkspace("Share Project Access Race WS",
        "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project Access Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> deleteProjectUseCase.deleteProject(
            new DeleteProjectCommand(project.getId(), admin.id())).block(),
        () -> {
          try {
            accessShareLinkUseCase.accessShareLink(new AccessShareLinkQuery(
                link.getCode(), null, "127.0.0.1", "JUnit")).block();
          } catch (DomainException error) {
            ignoreExpectedShareLinkLifecycleRaceError(error);
          }
        });

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project deletedProject = projectRepository.findById(project.getId()).block();
    ShareLink deletedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(deletedProject).isNotNull();
    assertThat(deletedProject.isDeleted()).isTrue();
    assertThat(deletedLink).isNotNull();
    assertThat(deletedLink.isDeleted()).isTrue();
    assertThat(deletedLink.getAccessCount()).isBetween(0L, 1L);
  }

  @Test
  @DisplayName("프로젝트 삭제와 공유 링크 삭제가 동시에 실행되어도 공유 링크는 삭제 상태로 수렴한다")
  void concurrentProjectDeleteAndShareLinkDelete_deletesShareLink()
      throws InterruptedException {
    User admin = signUpUser("admin-share-project-delete-race@test.com", "Admin");
    var workspace = saveWorkspace("Share Project Delete Race WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project Delete Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> deleteProjectUseCase.deleteProject(
            new DeleteProjectCommand(project.getId(), admin.id())).block(),
        () -> {
          try {
            deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(
                project.getId(), link.getId(), admin.id())).block();
          } catch (DomainException error) {
            ignoreExpectedShareLinkLifecycleRaceError(error);
          }
        });

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project deletedProject = projectRepository.findById(project.getId()).block();
    ShareLink deletedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(deletedProject).isNotNull();
    assertThat(deletedProject.isDeleted()).isTrue();
    assertThat(deletedLink).isNotNull();
    assertThat(deletedLink.isDeleted()).isTrue();
  }

  private ConcurrentLinkedQueue<Throwable> runConcurrently(CheckedTask... tasks)
      throws InterruptedException {
    CountDownLatch readyLatch = new CountDownLatch(tasks.length);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(tasks.length);
    ConcurrentLinkedQueue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(tasks.length)) {
      for (CheckedTask task : tasks) {
        executor.submit(() -> {
          readyLatch.countDown();
          await(startLatch);
          try {
            task.run();
          } catch (Throwable error) {
            unexpectedErrors.add(error);
          } finally {
            doneLatch.countDown();
          }
        });
      }

      readyLatch.await();
      startLatch.countDown();
      doneLatch.await();
    }

    return unexpectedErrors;
  }

  private void ignoreExpectedShareLinkLifecycleRaceError(
      DomainException error) {
    if (error.getErrorCode() == ShareLinkErrorCode.NOT_FOUND
        || error.getErrorCode() == ProjectErrorCode.NOT_FOUND
        || error.getErrorCode() == ProjectErrorCode.ACCESS_DENIED
        || error.getErrorCode() == ProjectErrorCode.ADMIN_REQUIRED) {
      return;
    }
    throw error;
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(error);
    }
  }

  @FunctionalInterface
  private interface CheckedTask {

    void run() throws Exception;

  }

}
