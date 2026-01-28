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
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Invitation;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("Invitation Race Condition 테스트")
class ProjectInvitationRaceConditionTest {

  @Autowired
  private ProjectInvitationService invitationService;

  @Autowired
  private InvitationRepository invitationRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private UserRepository userRepository;

  private User invitedUser;
  private Workspace testWorkspace;
  private Project testProject;
  private Invitation testInvitation;

  @BeforeEach
  void setUp() {
    Mono.when(
        invitationRepository.deleteAll(),
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
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

    WorkspaceMember adminWsMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(adminWsMember).block();

    WorkspaceMember invitedWsMember = WorkspaceMember.create(
        testWorkspace.getId(), invitedUser.getId(), WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(invitedWsMember).block();

    testProject = Project.create(
        testWorkspace.getId(),
        "Test Project",
        "Test Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();

    ProjectMember adminProjMember = ProjectMember.create(
        testProject.getId(), adminUser.getId(), ProjectRole.ADMIN);
    projectMemberRepository.save(adminProjMember).block();

    testInvitation = Invitation.createProjectInvitation(
        testProject.getId(),
        testWorkspace.getId(),
        invitedUser.getEmail(),
        ProjectRole.EDITOR,
        adminUser.getId());
    testInvitation = invitationRepository.save(testInvitation).block();
  }

  @Test
  @DisplayName("동시에 프로젝트 초대를 수락할 때 한 번만 성공하고 나머지는 실패한다")
  void concurrentProjectAccept_OnlyOneSucceeds() throws InterruptedException {
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
              e.getErrorCode() == ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION ||
              e.getErrorCode() == ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT) {
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

    long memberCount = projectMemberRepository
        .countByProjectIdAndNotDeleted(testProject.getId())
        .block();
    assertThat(memberCount).isEqualTo(2);
  }

  @Test
  @DisplayName("동시에 프로젝트 초대를 거절할 때 한 번만 성공하고 나머지는 실패한다")
  void concurrentProjectReject_OnlyOneSucceeds() throws InterruptedException {
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
              e.getErrorCode() == ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION) {
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
  void concurrentProjectAcceptAndReject_OnlyOneSucceeds() throws InterruptedException {
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
              e.getErrorCode() == ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION ||
              e.getErrorCode() == ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT) {
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
  @DisplayName("여러 사용자가 동시에 프로젝트에 참여할 때 멤버 수 제한을 초과하지 않는다")
  void concurrentMultipleAccepts_RespectsMemberLimit() throws InterruptedException {
    for (int i = 0; i < 28; i++) {
      User user = User.signUp(
          new UserInfo("user" + i + "@test.com", "User " + i, "password"),
          new BCryptPasswordEncoder()).flatMap(userRepository::save).block();

      WorkspaceMember wsMember = WorkspaceMember.create(
          testWorkspace.getId(), user.getId(), WorkspaceRole.MEMBER);
      workspaceMemberRepository.save(wsMember).block();

      ProjectMember projMember = ProjectMember.create(
          testProject.getId(), user.getId(), ProjectRole.VIEWER);
      projectMemberRepository.save(projMember).block();
    }

    User lastUser = User.signUp(
        new UserInfo("last@test.com", "Last User", "password"),
        new BCryptPasswordEncoder()).flatMap(userRepository::save).block();

    WorkspaceMember lastWsMember = WorkspaceMember.create(
        testWorkspace.getId(), lastUser.getId(), WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(lastWsMember).block();

    Invitation invitation1 = Invitation.createProjectInvitation(
        testProject.getId(),
        testWorkspace.getId(),
        invitedUser.getEmail(),
        ProjectRole.EDITOR,
        testProject.getId());
    invitation1 = invitationRepository.save(invitation1).block();

    Invitation invitation2 = Invitation.createProjectInvitation(
        testProject.getId(),
        testWorkspace.getId(),
        lastUser.getEmail(),
        ProjectRole.VIEWER,
        testProject.getId());
    invitation2 = invitationRepository.save(invitation2).block();

    CountDownLatch latch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger limitExceededCount = new AtomicInteger(0);

    String inv1Id = invitation1.getId();
    String inv2Id = invitation2.getId();
    String userId1 = invitedUser.getId();
    String userId2 = lastUser.getId();

    Mono.fromRunnable(() -> {
      try {
        startLatch.await();
        invitationService.acceptInvitation(inv1Id, userId1).block();
        successCount.incrementAndGet();
      } catch (BusinessException e) {
        if (e.getErrorCode() == ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED) {
          limitExceededCount.incrementAndGet();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        latch.countDown();
      }
    })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

    Mono.fromRunnable(() -> {
      try {
        startLatch.await();
        invitationService.acceptInvitation(inv2Id, userId2).block();
        successCount.incrementAndGet();
      } catch (BusinessException e) {
        if (e.getErrorCode() == ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED) {
          limitExceededCount.incrementAndGet();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        latch.countDown();
      }
    })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

    startLatch.countDown();
    latch.await();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(limitExceededCount.get()).isEqualTo(1);

    long memberCount = projectMemberRepository
        .countByProjectIdAndNotDeleted(testProject.getId())
        .block();
    assertThat(memberCount).isEqualTo(30);
  }

}
