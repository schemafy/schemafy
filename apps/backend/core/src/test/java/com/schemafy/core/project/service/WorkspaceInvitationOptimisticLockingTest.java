package com.schemafy.core.project.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.repository.InvitationRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Invitation;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("Invitation Optimistic Locking 테스트")
class WorkspaceInvitationOptimisticLockingTest {

  @Autowired
  private WorkspaceInvitationService invitationService;

  @Autowired
  private InvitationRepository invitationRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository memberRepository;

  @Autowired
  private UserRepository userRepository;

  private User invitedUser;
  private Workspace testWorkspace;
  private Invitation testInvitation;

  @BeforeEach
  void setUp() {
    Mono.when(
        invitationRepository.deleteAll(),
        memberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll()).block();

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    User adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    invitedUser = User.signUp(
        new UserInfo("invited@test.com", "Invited User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create("Test Workspace", "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    WorkspaceMember adminMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
    memberRepository.save(adminMember).block();

    testInvitation = Invitation.createWorkspaceInvitation(
        testWorkspace.getId(),
        invitedUser.getEmail(),
        WorkspaceRole.MEMBER,
        adminUser.getId());
    testInvitation = invitationRepository.save(testInvitation).block();
  }

  @Test
  @DisplayName("동시에 초대를 수락할 때 한 번만 성공하고 나머지는 실패한다")
  void concurrentAccept_OnlyOneSucceeds() throws InterruptedException {
    int threadCount = 5;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    String invitationId = testInvitation.getId();
    String userId = invitedUser.getId();

    for (int i = 0; i < threadCount; i++) {
      Mono.fromRunnable(() -> {
        try {
          startLatch.await();
          invitationService.acceptInvitation(invitationId, userId)
              .block();
          successCount.incrementAndGet();
        } catch (BusinessException e) {
          if (e.getErrorCode() == ErrorCode.INVITATION_CONCURRENT_MODIFICATION ||
              e.getErrorCode() == ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION ||
              e.getErrorCode() == ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER) {
            failureCount.incrementAndGet();
          } else {
            throw e;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      })
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();
    }

    startLatch.countDown();
    latch.await();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);

    Invitation finalInvitation = invitationRepository.findById(invitationId).block();
    assertThat(finalInvitation.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

    long memberCount = memberRepository
        .countByWorkspaceIdAndNotDeleted(testWorkspace.getId())
        .block();
    assertThat(memberCount).isEqualTo(2);
  }

  @Test
  @DisplayName("동시에 초대를 거절할 때 한 번만 성공하고 나머지는 실패한다")
  void concurrentReject_OnlyOneSucceeds() throws InterruptedException {
    int threadCount = 5;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    String invitationId = testInvitation.getId();
    String userId = invitedUser.getId();

    for (int i = 0; i < threadCount; i++) {
      Mono.fromRunnable(() -> {
        try {
          startLatch.await();
          invitationService.rejectInvitation(invitationId, userId)
              .block();
          successCount.incrementAndGet();
        } catch (BusinessException e) {
          if (e.getErrorCode() == ErrorCode.INVITATION_CONCURRENT_MODIFICATION ||
              e.getErrorCode() == ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION) {
            failureCount.incrementAndGet();
          } else {
            throw e;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      })
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();
    }

    startLatch.countDown();
    latch.await();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);

    Invitation finalInvitation = invitationRepository.findById(invitationId).block();
    assertThat(finalInvitation.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
  }

  @Test
  @DisplayName("동시에 수락과 거절을 시도하면 한 쪽만 성공한다")
  void concurrentAcceptAndReject_OnlyOneSucceeds() throws InterruptedException {
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger acceptSuccessCount = new AtomicInteger(0);
    AtomicInteger rejectSuccessCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    String invitationId = testInvitation.getId();
    String userId = invitedUser.getId();

    for (int i = 0; i < threadCount; i++) {
      final boolean shouldAccept = i % 2 == 0;

      Mono.fromRunnable(() -> {
        try {
          startLatch.await();
          if (shouldAccept) {
            invitationService.acceptInvitation(invitationId, userId).block();
            acceptSuccessCount.incrementAndGet();
          } else {
            invitationService.rejectInvitation(invitationId, userId).block();
            rejectSuccessCount.incrementAndGet();
          }
        } catch (BusinessException e) {
          if (e.getErrorCode() == ErrorCode.INVITATION_CONCURRENT_MODIFICATION ||
              e.getErrorCode() == ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION ||
              e.getErrorCode() == ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER) {
            failureCount.incrementAndGet();
          } else {
            throw e;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      })
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();
    }

    startLatch.countDown();
    latch.await();

    int totalSuccess = acceptSuccessCount.get() + rejectSuccessCount.get();
    assertThat(totalSuccess).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);

    Invitation finalInvitation = invitationRepository.findById(invitationId).block();
    assertThat(finalInvitation.getStatusAsEnum()).isIn(
        InvitationStatus.ACCEPTED, InvitationStatus.REJECTED);
  }

  @Test
  @DisplayName("재시도 메커니즘이 동작하여 OptimisticLockingFailureException을 처리한다")
  void retryMechanism_HandlesOptimisticLocking() throws InterruptedException {
    int threadCount = 3;
    CountDownLatch latch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger businessExceptionCount = new AtomicInteger(0);

    String invitationId = testInvitation.getId();
    String userId = invitedUser.getId();

    for (int i = 0; i < threadCount; i++) {
      Mono.fromRunnable(() -> {
        try {
          startLatch.await();
          invitationService.acceptInvitation(invitationId, userId)
              .block();
          successCount.incrementAndGet();
        } catch (BusinessException e) {
          businessExceptionCount.incrementAndGet();
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      })
          .subscribeOn(Schedulers.boundedElastic())
          .subscribe();
    }

    startLatch.countDown();
    latch.await();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(businessExceptionCount.get()).isEqualTo(threadCount - 1);
  }

}
