package com.schemafy.core.project.integration;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("워크스페이스 초대 동시성 통합 테스트")
class WorkspaceInvitationConcurrencyIntegrationTest
    extends ProjectDomainIntegrationSupport {

  @Autowired
  private AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;

  @Test
  @DisplayName("동시에 워크스페이스 초대를 수락해도 정확히 한 번만 성공한다")
  void concurrentAcceptSucceedsExactlyOnce() throws InterruptedException {
    User admin = signUpUser("admin-wic@test.com", "Admin");
    User invitee = signUpUser("invitee-wic@test.com", "Invitee");
    var workspace = saveWorkspace("Concurrency WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace, invitee.email(), WorkspaceRole.MEMBER,
        admin);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();
    CountDownLatch readyLatch = new CountDownLatch(5);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(5);
    ConcurrentLinkedQueue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    ExecutorService executor = Executors.newFixedThreadPool(5);
    try {
      for (int i = 0; i < 5; i++) {
        executor.submit(() -> {
          readyLatch.countDown();
          await(startLatch);

          try {
            acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
                new AcceptWorkspaceInvitationCommand(invitation.getId(), invitee.id()))
                .block();
            successCount.incrementAndGet();
          } catch (DomainException error) {
            if (error.getErrorCode() == ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED
                || error.getErrorCode() == ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED
                || error.getErrorCode() == ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER) {
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

      assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();
      startLatch.countDown();
      assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
    } finally {
      startLatch.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
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
