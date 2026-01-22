package com.schemafy.core.project.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.repository.WorkspaceInvitationRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceInvitation;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("WorkspaceInvitationService 테스트")
class WorkspaceInvitationServiceTest {

  @Autowired
  private WorkspaceInvitationService invitationService;

  @Autowired
  private WorkspaceInvitationRepository invitationRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository memberRepository;

  @Autowired
  private UserRepository userRepository;

  private User adminUser;
  private User memberUser;
  private User invitedUser;
  private Workspace testWorkspace;
  private WorkspaceMember adminMember;

  @BeforeEach
  void setUp() {
    Mono.when(
        invitationRepository.deleteAll(),
        memberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll()).block();

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    memberUser = User.signUp(
        new UserInfo("member@test.com", "Member User", "password"),
        encoder).flatMap(userRepository::save).block();

    invitedUser = User.signUp(
        new UserInfo("invited@test.com", "Invited User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create("Test Workspace", "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    adminMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
    adminMember = memberRepository.save(adminMember).block();
  }

  @Nested
  @DisplayName("초대 생성 (createInvitation)")
  class CreateInvitationTests {

    @Test
    @DisplayName("관리자가 새로운 사용자를 초대하면 성공한다")
    void createInvitation_Success() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getWorkspaceId()).isEqualTo(testWorkspace.getId());
            assertThat(response.getInvitedEmail()).isEqualTo("newuser@test.com");
            assertThat(response.getInvitedRole()).isEqualTo(WorkspaceRole.MEMBER.getValue());
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.getValue());
            assertThat(response.getInvitedBy()).isEqualTo(adminUser.getId());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이메일이 대문자여도 소문자로 저장된다")
    void createInvitation_EmailLowerCase() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("NewUser@Test.COM",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getInvitedEmail()).isEqualTo("newuser@test.com");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 실패한다")
    void createInvitation_NotAdmin_Fails() {
      WorkspaceMember normalMember = WorkspaceMember.create(
          testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER);
      memberRepository.save(normalMember).block();

      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, memberUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_ADMIN_REQUIRED);
          })
          .verify();
    }

    @Test
    @DisplayName("워크스페이스 멤버가 아니면 실패한다")
    void createInvitation_NotMember_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, memberUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 멤버인 사용자를 초대하면 실패한다")
    void createInvitation_AlreadyMember_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest(adminUser.getEmail(),
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("동일한 이메일에 대한 대기 중인 초대가 있으면 실패한다")
    void createInvitation_DuplicatePending_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      invitationService.createInvitation(
          testWorkspace.getId(), request, adminUser.getId()).block();

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request, adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_ALREADY_EXISTS);
          })
          .verify();
    }

    @Test
    @DisplayName("존재하지 않는 워크스페이스에 초대하면 실패한다")
    void createInvitation_WorkspaceNotFound_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              "nonexistent-workspace-id", request, adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 목록 조회 (listInvitations)")
  class ListInvitationsTests {

    @Test
    @DisplayName("관리자는 초대 목록을 조회할 수 있다")
    void listInvitations_Success() {
      WorkspaceInvitation invitation1 = WorkspaceInvitation.create(
          testWorkspace.getId(), "user1@test.com", WorkspaceRole.MEMBER, adminUser.getId());
      WorkspaceInvitation invitation2 = WorkspaceInvitation.create(
          testWorkspace.getId(), "user2@test.com", WorkspaceRole.MEMBER, adminUser.getId());

      Flux.just(invitation1, invitation2)
          .flatMap(invitationRepository::save)
          .blockLast();

      StepVerifier.create(
          invitationService.listInvitations(
              testWorkspace.getId(), adminUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(2);
            assertThat(page.page()).isZero();
            assertThat(page.size()).isEqualTo(10);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("페이지네이션이 올바르게 동작한다")
    void listInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        WorkspaceInvitation invitation = WorkspaceInvitation.create(
            testWorkspace.getId(),
            "user" + i + "@test.com",
            WorkspaceRole.MEMBER,
            adminUser.getId());
        invitationRepository.save(invitation).block();
      }

      StepVerifier.create(
          invitationService.listInvitations(
              testWorkspace.getId(), adminUser.getId(), 0, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();

      StepVerifier.create(
          invitationService.listInvitations(
              testWorkspace.getId(), adminUser.getId(), 2, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 목록 조회에 실패한다")
    void listInvitations_NotAdmin_Fails() {
      WorkspaceMember normalMember = WorkspaceMember.create(
          testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER);
      memberRepository.save(normalMember).block();

      StepVerifier.create(
          invitationService.listInvitations(
              testWorkspace.getId(), memberUser.getId(), 0, 10))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_ADMIN_REQUIRED);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("내 초대 목록 조회 (listMyInvitations)")
  class ListMyInvitationsTests {

    @Test
    @DisplayName("사용자가 받은 대기 중인 초대 목록을 조회할 수 있다")
    void listMyInvitations_Success() {
      // 다른 워크스페이스들 생성
      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();
      Workspace workspace3 = Workspace.create("Workspace 3", "Description 3");
      workspace3 = workspaceRepository.save(workspace3).block();

      // invitedUser에게 여러 초대 생성
      WorkspaceInvitation invitation1 = WorkspaceInvitation.create(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      WorkspaceInvitation invitation2 = WorkspaceInvitation.create(
          workspace2.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      WorkspaceInvitation invitation3 = WorkspaceInvitation.create(
          workspace3.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());

      Flux.just(invitation1, invitation2, invitation3)
          .flatMap(invitationRepository::save)
          .blockLast();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(3);
            assertThat(page.totalElements()).isEqualTo(3);
            assertThat(page.page()).isZero();
            assertThat(page.size()).isEqualTo(10);
            assertThat(page.content())
                .extracting("invitedEmail")
                .containsOnly(invitedUser.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("대기 중인 초대가 없으면 빈 목록을 반환한다")
    void listMyInvitations_Empty() {
      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("페이지네이션이 올바르게 동작한다")
    void listMyInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        Workspace workspace = Workspace.create("Workspace " + i, "Description " + i);
        workspace = workspaceRepository.save(workspace).block();
        WorkspaceInvitation invitation = WorkspaceInvitation.create(
            workspace.getId(),
            invitedUser.getEmail(),
            WorkspaceRole.MEMBER,
            adminUser.getId());
        invitationRepository.save(invitation).block();
      }

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 2, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("수락/거절된 초대는 목록에 포함되지 않는다")
    void listMyInvitations_OnlyPending() {
      WorkspaceInvitation pending = WorkspaceInvitation.create(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      pending = invitationRepository.save(pending).block();
      final String pendingId = pending.getId();

      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();
      WorkspaceInvitation accepted = WorkspaceInvitation.create(
          workspace2.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      Workspace workspace3 = Workspace.create("Workspace 3", "Description 3");
      workspace3 = workspaceRepository.save(workspace3).block();
      WorkspaceInvitation rejected = WorkspaceInvitation.create(
          workspace3.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      rejected = invitationRepository.save(rejected).block();
      rejected.reject();
      invitationRepository.save(rejected).block();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content().get(0).id()).isEqualTo(pendingId);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 사용자에게 온 초대는 보이지 않는다")
    void listMyInvitations_OnlyMyInvitations() {
      WorkspaceInvitation myInvitation = WorkspaceInvitation.create(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitationRepository.save(myInvitation).block();

      WorkspaceInvitation otherInvitation = WorkspaceInvitation.create(
          testWorkspace.getId(), memberUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitationRepository.save(otherInvitation).block();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).invitedEmail()).isEqualTo(invitedUser.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 실패한다")
    void listMyInvitations_UserNotFound_Fails() {
      StepVerifier.create(
          invitationService.listMyInvitations("nonexistent-user-id", 0, 10))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 수락 (acceptInvitation)")
  class AcceptInvitationTests {

    @Test
    @DisplayName("유효한 초대를 수락하면 워크스페이스 멤버가 생성된다")
    void acceptInvitation_Success() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, invitedUser.getId()))
          .assertNext(member -> {
            assertThat(member.userId()).isEqualTo(invitedUser.getId());
            assertThat(member.role()).isEqualTo(WorkspaceRole.MEMBER.getValue());
          })
          .verifyComplete();

      WorkspaceInvitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void acceptInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.acceptInvitation("nonexistent-id", invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void acceptInvitation_EmailMismatch_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          "other@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("만료된 초대를 수락하면 실패한다")
    void acceptInvitation_Expired_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitation.getClass().getDeclaredFields();
      try {
        var field = invitation.getClass().getDeclaredField("expiresAt");
        field.setAccessible(true);
        field.set(invitation, Instant.now().minusSeconds(3600));
      } catch (Exception e) {}
      invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_EXPIRED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 실패한다")
    void acceptInvitation_AlreadyAccepted_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("거절된 초대를 수락하면 실패한다")
    void acceptInvitation_AlreadyRejected_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 워크스페이스 멤버인 경우 초대 수락에 실패한다")
    void acceptInvitation_AlreadyMember_Fails() {
      WorkspaceMember existingMember = WorkspaceMember.create(
          testWorkspace.getId(), invitedUser.getId(), WorkspaceRole.MEMBER);
      memberRepository.save(existingMember).block();

      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("멤버 수 제한(30명)에 도달하면 초대 수락에 실패한다")
    void acceptInvitation_MemberLimitExceeded_Fails() {
      for (int i = 0; i < 29; i++) {
        User user = User.signUp(
            new UserInfo("user" + i + "@test.com", "User " + i, "password"),
            new BCryptPasswordEncoder()).flatMap(userRepository::save).block();
        WorkspaceMember member = WorkspaceMember.create(
            testWorkspace.getId(), user.getId(), WorkspaceRole.MEMBER);
        memberRepository.save(member).block();
      }

      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode())
                .isEqualTo(ErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEED);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 수락에 실패한다")
    void acceptInvitation_UserNotFound_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          "newuser@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), "nonexistent-user-id"))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 거절 (rejectInvitation)")
  class RejectInvitationTests {

    @Test
    @DisplayName("유효한 초대를 거절하면 성공한다")
    void rejectInvitation_Success() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, invitedUser.getId()))
          .verifyComplete();

      WorkspaceInvitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void rejectInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.rejectInvitation("nonexistent-id", invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void rejectInvitation_EmailMismatch_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          "other@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 실패한다")
    void rejectInvitation_AlreadyAccepted_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 실패한다")
    void rejectInvitation_AlreadyRejected_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 거절에 실패한다")
    void rejectInvitation_UserNotFound_Fails() {
      WorkspaceInvitation invitation = WorkspaceInvitation.create(
          testWorkspace.getId(),
          "newuser@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), "nonexistent-user-id"))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
          })
          .verify();
    }

  }

}
