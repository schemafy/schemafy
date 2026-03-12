package com.schemafy.core.project.integration;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("Project invitation concurrency integration")
class ProjectInvitationConcurrencyIntegrationTest
    extends ProjectDomainIntegrationSupport {

  @Autowired
  private AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;

  @Test
  @DisplayName("concurrent project invitation accept succeeds exactly once")
  void concurrentAccept_onlyOneSucceeds() throws InterruptedException {
    User admin = signUpUser("admin-pic@test.com", "Admin");
    User invitee = signUpUser("invitee-pic@test.com", "Invitee");
    var workspace = saveWorkspace("Project Concurrency WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Project Concurrency");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation invitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.EDITOR, admin);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();
    CountDownLatch readyLatch = new CountDownLatch(5);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(5);
    ConcurrentLinkedQueue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
      for (int i = 0; i < 5; i++) {
        executor.submit(() -> {
          readyLatch.countDown();
          await(startLatch);

          try {
            acceptProjectInvitationUseCase.acceptProjectInvitation(
                new AcceptProjectInvitationCommand(invitation.getId(), invitee.id()))
                .block();
            successCount.incrementAndGet();
          } catch (DomainException error) {
            if (error.getErrorCode() == ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED
                || error.getErrorCode() == ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED
                || error.getErrorCode() == ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT) {
              failureCount.incrementAndGet();
            } else {
              unexpectedErrors.add(error);
            }
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

    Invitation updatedInvitation = invitationRepository.findById(invitation.getId()).block();

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(4);
    assertThat(updatedInvitation.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

}
