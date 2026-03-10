package com.schemafy.core.project.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.exception.ProjectErrorCode;
import com.schemafy.core.project.exception.WorkspaceErrorCode;
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
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.exception.UserErrorCode;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;
import com.schemafy.domain.common.exception.DomainException;

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
  private InvitationRepository invitationRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository memberRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  private User adminUser;
  private User memberUser;
  private User invitedUser;
  private Workspace testWorkspace;
  private WorkspaceMember adminMember;

  @BeforeEach
  void setUp() {
    Mono.when(
        invitationRepository.deleteAll(),
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
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
    @DisplayName("관리자가 가입된 비멤버 사용자를 초대하면 성공한다")
    void createInvitation_Success() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest(invitedUser.getEmail(),
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getWorkspaceId()).isEqualTo(testWorkspace.getId());
            assertThat(response.getInvitedEmail()).isEqualTo(invitedUser.getEmail());
            assertThat(response.getInvitedRole()).isEqualTo(WorkspaceRole.MEMBER.name());
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
            assertThat(response.getInvitedBy()).isEqualTo(adminUser.getId());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이메일이 대문자여도 소문자로 저장된다")
    void createInvitation_EmailLowerCase() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest(
          invitedUser.getEmail().toUpperCase(),
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getInvitedEmail()).isEqualTo(invitedUser.getEmail());
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
              testWorkspace.getId(), request.email(), request.role(), memberUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(WorkspaceErrorCode.ADMIN_REQUIRED);
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
              testWorkspace.getId(), request.email(), request.role(), memberUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(WorkspaceErrorCode.ACCESS_DENIED);
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
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("동일한 이메일에 대한 대기 중인 초대가 있으면 실패한다")
    void createInvitation_DuplicatePending_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest(invitedUser.getEmail(),
          WorkspaceRole.MEMBER);

      invitationService.createInvitation(
          testWorkspace.getId(), request.email(), request.role(), adminUser.getId()).block();

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_ALREADY_EXISTS);
          })
          .verify();
    }

    @Test
    @DisplayName("만료된 pending 초대는 중복 체크에서 제외된다")
    void createInvitation_ExpiredPending_DoesNotBlock() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest(invitedUser.getEmail(),
          WorkspaceRole.MEMBER);

      Invitation expiredInvitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          request.email(),
          request.role(),
          adminUser.getId());
      expiredInvitation = invitationRepository.save(expiredInvitation).block();
      ReflectionTestUtils.setField(expiredInvitation, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(expiredInvitation).block();
      final String expiredInvitationId = expiredInvitation.getId();

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
            assertThat(response.getId()).isNotEqualTo(expiredInvitationId);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 초대하면 실패한다")
    void createInvitation_UnregisteredEmail_NotFound() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("notyet@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), request.email(), request.role(), adminUser.getId()))
          .expectErrorMatches(DomainException.hasErrorCode(UserErrorCode.NOT_FOUND))
          .verify();
    }

    @Test
    @DisplayName("존재하지 않는 워크스페이스에 초대하면 실패한다")
    void createInvitation_WorkspaceNotFound_Fails() {
      CreateWorkspaceInvitationRequest request = new CreateWorkspaceInvitationRequest("newuser@test.com",
          WorkspaceRole.MEMBER);

      StepVerifier.create(
          invitationService.createInvitation(
              "nonexistent-workspace-id", request.email(), request.role(), adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(WorkspaceErrorCode.ACCESS_DENIED);
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
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), "user1@test.com", WorkspaceRole.MEMBER, adminUser.getId());
      Invitation invitation2 = Invitation.createWorkspaceInvitation(
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
        Invitation invitation = Invitation.createWorkspaceInvitation(
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
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(WorkspaceErrorCode.ADMIN_REQUIRED);
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
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      Invitation invitation2 = Invitation.createWorkspaceInvitation(
          workspace2.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      Invitation invitation3 = Invitation.createWorkspaceInvitation(
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
        Invitation invitation = Invitation.createWorkspaceInvitation(
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
      Invitation pending = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      pending = invitationRepository.save(pending).block();
      final String pendingId = pending.getId();

      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();
      Invitation accepted = Invitation.createWorkspaceInvitation(
          workspace2.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      Workspace workspace3 = Workspace.create("Workspace 3", "Description 3");
      workspace3 = workspaceRepository.save(workspace3).block();
      Invitation rejected = Invitation.createWorkspaceInvitation(
          workspace3.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      rejected = invitationRepository.save(rejected).block();
      rejected.reject();
      invitationRepository.save(rejected).block();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content().get(0).getId()).isEqualTo(pendingId);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("만료된 pending 초대는 목록에 포함되지 않는다")
    void listMyInvitations_ExcludesExpiredPending() {
      Invitation activePending = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      activePending = invitationRepository.save(activePending).block();
      final String activePendingId = activePending.getId();

      Workspace workspace2 = Workspace.create("Workspace 2", "Description 2");
      workspace2 = workspaceRepository.save(workspace2).block();
      Invitation expiredPending = Invitation.createWorkspaceInvitation(
          workspace2.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      expiredPending = invitationRepository.save(expiredPending).block();
      ReflectionTestUtils.setField(expiredPending, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(expiredPending).block();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content().get(0).getId()).isEqualTo(activePendingId);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 사용자에게 온 초대는 보이지 않는다")
    void listMyInvitations_OnlyMyInvitations() {
      Invitation myInvitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitationRepository.save(myInvitation).block();

      Invitation otherInvitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), memberUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitationRepository.save(otherInvitation).block();

      StepVerifier.create(
          invitationService.listMyInvitations(invitedUser.getId(), 0, 10))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).getInvitedEmail()).isEqualTo(invitedUser.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 실패한다")
    void listMyInvitations_UserNotFound_Fails() {
      StepVerifier.create(
          invitationService.listMyInvitations("nonexistent-user-id", 0, 10))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.NOT_FOUND);
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
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, invitedUser.getId()))
          .assertNext(member -> {
            assertThat(member.member().getUserId()).isEqualTo(invitedUser.getId());
            assertThat(member.member().getRole()).isEqualTo(WorkspaceRole.MEMBER.name());
          })
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void acceptInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.acceptInvitation("nonexistent-id", invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("삭제된 워크스페이스의 초대는 수락할 수 없다")
    void acceptInvitation_DeletedWorkspace_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      testWorkspace.delete();
      workspaceRepository.save(testWorkspace).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(WorkspaceErrorCode.NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void acceptInvitation_EmailMismatch_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          "other@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("만료된 초대를 수락하면 실패한다")
    void acceptInvitation_Expired_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      ReflectionTestUtils.setField(invitation, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_EXPIRED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 실패한다")
    void acceptInvitation_AlreadyAccepted_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("거절된 초대를 수락하면 실패한다")
    void acceptInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 워크스페이스 멤버인 경우 초대 수락에 실패한다")
    void acceptInvitation_AlreadyMember_Fails() {
      WorkspaceMember existingMember = WorkspaceMember.create(
          testWorkspace.getId(), invitedUser.getId(), WorkspaceRole.MEMBER);
      memberRepository.save(existingMember).block();

      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode())
                .isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 수락에 실패한다")
    void acceptInvitation_UserNotFound_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          "newuser@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), "nonexistent-user-id"))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("초대 수락 시 동일 대상의 다른 pending 초대가 cancelled 된다")
    void acceptInvitation_CancelsOtherPendingInvitations() {
      // 같은 워크스페이스, 같은 이메일로 pending 초대 2개 직접 생성
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      Invitation invitation2 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.ADMIN,
          adminUser.getId());
      invitation2 = invitationRepository.save(invitation2).block();

      String acceptedId = invitation1.getId();
      String otherPendingId = invitation2.getId();

      // invitation1 수락
      invitationService.acceptInvitation(acceptedId, invitedUser.getId()).block();

      // 수락한 초대는 ACCEPTED
      Invitation accepted = invitationRepository.findById(acceptedId).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      // 다른 pending 초대는 CANCELLED
      Invitation other = invitationRepository.findById(otherPendingId).block();
      assertThat(other.getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
      assertThat(other.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대 수락 시 만료된 pending 초대는 cancelled 되지 않는다")
    void acceptInvitation_DoesNotCancelExpiredPendingInvitations() {
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      Invitation expiredPending = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.ADMIN,
          adminUser.getId());
      expiredPending = invitationRepository.save(expiredPending).block();
      ReflectionTestUtils.setField(expiredPending, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(expiredPending).block();

      invitationService.acceptInvitation(invitation1.getId(), invitedUser.getId()).block();

      Invitation accepted = invitationRepository.findById(invitation1.getId()).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      Invitation expiredOther = invitationRepository.findById(expiredPending.getId()).block();
      assertThat(expiredOther.getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
      assertThat(expiredOther.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("초대 수락 시 다른 워크스페이스의 pending 초대는 영향받지 않는다")
    void acceptInvitation_DoesNotAffectOtherWorkspaceInvitations() {
      Workspace otherWorkspace = Workspace.create("Other Workspace", "Desc");
      otherWorkspace = workspaceRepository.save(otherWorkspace).block();

      // 현재 워크스페이스에 초대
      Invitation invitation1 = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      // 다른 워크스페이스에 같은 이메일로 초대
      Invitation otherWsInvitation = Invitation.createWorkspaceInvitation(
          otherWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      otherWsInvitation = invitationRepository.save(otherWsInvitation).block();

      // invitation1 수락
      invitationService.acceptInvitation(invitation1.getId(), invitedUser.getId()).block();

      // 다른 워크스페이스의 초대는 여전히 PENDING
      Invitation otherInv = invitationRepository.findById(otherWsInvitation.getId()).block();
      assertThat(otherInv.getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
    }

  }

  @Nested
  @DisplayName("초대 거절 (rejectInvitation)")
  class RejectInvitationTests {

    @Test
    @DisplayName("유효한 초대를 거절하면 성공한다")
    void rejectInvitation_Success() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, invitedUser.getId()))
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void rejectInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.rejectInvitation("nonexistent-id", invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("삭제된 워크스페이스의 초대도 거절할 수 있다")
    void rejectInvitation_DeletedWorkspace_Succeeds() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      testWorkspace.delete();
      workspaceRepository.save(testWorkspace).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void rejectInvitation_EmailMismatch_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          "other@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 실패한다")
    void rejectInvitation_AlreadyAccepted_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 실패한다")
    void rejectInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          invitedUser.getEmail(),
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 거절에 실패한다")
    void rejectInvitation_UserNotFound_Fails() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          "newuser@test.com",
          WorkspaceRole.MEMBER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), "nonexistent-user-id"))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.NOT_FOUND);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 타입 검증")
  class InvitationTypeValidationTests {

    @Test
    @DisplayName("PROJECT 타입 초대를 Workspace 서비스로 수락하면 실패한다")
    void acceptProjectInvitation_WithWorkspaceService_Fails() {
      // PROJECT 타입 초대 생성
      Invitation projectInvitation = Invitation.createProjectInvitation(
          "project-id",
          testWorkspace.getId(),
          invitedUser.getEmail(),
          com.schemafy.core.project.repository.vo.ProjectRole.VIEWER,
          adminUser.getId());
      projectInvitation = invitationRepository.save(projectInvitation).block();
      final String invitationId = projectInvitation.getId();

      // WorkspaceInvitationService로 수락 시도
      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("PROJECT 타입 초대를 Workspace 서비스로 거절하면 실패한다")
    void rejectProjectInvitation_WithWorkspaceService_Fails() {
      // PROJECT 타입 초대 생성
      Invitation projectInvitation = Invitation.createProjectInvitation(
          "project-id",
          testWorkspace.getId(),
          invitedUser.getEmail(),
          com.schemafy.core.project.repository.vo.ProjectRole.VIEWER,
          adminUser.getId());
      projectInvitation = invitationRepository.save(projectInvitation).block();
      final String invitationId = projectInvitation.getId();

      // WorkspaceInvitationService로 거절 시도
      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, invitedUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 수락 시 프로젝트 역할 전파")
  class AcceptInvitationRolePropagationTests {

    @Test
    @DisplayName("MEMBER로 초대 수락 시 모든 기존 프로젝트에 VIEWER로 추가된다")
    void acceptInvitation_Member_PropagatesAsViewer() {
      Project project1 = Project.create(testWorkspace.getId(), "Project 1", "Desc");
      project1 = projectRepository.save(project1).block();
      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Desc");
      project2 = projectRepository.save(project2).block();

      // MEMBER로 초대 생성
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      // 초대 수락
      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      // 프로젝트 1에 VIEWER로 추가되었는지 확인
      ProjectMember pm1 = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project1.getId(), invitedUser.getId()).block();
      assertThat(pm1).isNotNull();
      assertThat(pm1.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);

      // 프로젝트 2에 VIEWER로 추가되었는지 확인
      ProjectMember pm2 = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project2.getId(), invitedUser.getId()).block();
      assertThat(pm2).isNotNull();
      assertThat(pm2.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("ADMIN으로 초대 수락 시 모든 기존 프로젝트에 ADMIN으로 추가된다")
    void acceptInvitation_Admin_PropagatesAsAdmin() {
      Project project = Project.create(testWorkspace.getId(), "Project 1", "Desc");
      project = projectRepository.save(project).block();

      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitedUser.getId()).block();
      assertThat(pm).isNotNull();
      assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 경우 기존 역할이 유지된다")
    void acceptInvitation_AlreadyProjectMember_KeepsExistingRole() {
      Project project = Project.create(testWorkspace.getId(), "Project 1", "Desc");
      project = projectRepository.save(project).block();

      // 미리 EDITOR로 프로젝트 멤버 추가
      ProjectMember existing = ProjectMember.create(project.getId(), invitedUser.getId(), ProjectRole.EDITOR);
      projectMemberRepository.save(existing).block();

      // ADMIN으로 워크스페이스 초대 수락
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      // 기존 EDITOR 역할이 유지되어야 함
      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitedUser.getId()).block();
      assertThat(pm).isNotNull();
      assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    @DisplayName("삭제된 프로젝트에는 전파되지 않는다")
    void acceptInvitation_DeletedProject_Skipped() {
      // 프로젝트 생성 후 삭제
      Project project = Project.create(testWorkspace.getId(), "Deleted Project", "Desc");
      project = projectRepository.save(project).block();
      project.delete();
      projectRepository.save(project).block();

      // 활성 프로젝트 하나 생성
      Project activeProject = Project.create(testWorkspace.getId(), "Active Project", "Desc");
      activeProject = projectRepository.save(activeProject).block();

      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      // 삭제된 프로젝트에는 추가되지 않아야 함
      ProjectMember deletedPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitedUser.getId()).block();
      assertThat(deletedPm).isNull();

      // 활성 프로젝트에는 추가되어야 함
      ProjectMember activePm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(activeProject.getId(), invitedUser.getId()).block();
      assertThat(activePm).isNotNull();
      assertThat(activePm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("프로젝트가 없는 워크스페이스에서는 전파 없이 성공한다")
    void acceptInvitation_NoProjects_SucceedsWithoutPropagation() {
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()))
          .assertNext(member -> {
            assertThat(member.member().getUserId()).isEqualTo(invitedUser.getId());
            assertThat(member.member().getRole()).isEqualTo(WorkspaceRole.MEMBER.name());
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("재초대 시나리오")
  class ReInvitationTests {

    @Test
    @DisplayName("탈퇴한 멤버를 초대하면 기존 레코드가 복원되고 역할이 갱신된다")
    void reInvite_AfterLeave_RestoresMember() {
      // invitedUser를 MEMBER로 추가 후 soft-delete
      WorkspaceMember existingMember = WorkspaceMember.create(
          testWorkspace.getId(), invitedUser.getId(), WorkspaceRole.MEMBER);
      existingMember = memberRepository.save(existingMember).block();
      existingMember.delete();
      memberRepository.save(existingMember).block();
      final String originalMemberId = existingMember.getId();

      // 동일 이메일로 ADMIN 초대 생성 및 수락
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      // 기존 row(same ID)가 복원되고 역할이 ADMIN으로 갱신되었는지 확인
      WorkspaceMember restored = memberRepository
          .findByWorkspaceIdAndUserIdAndNotDeleted(testWorkspace.getId(), invitedUser.getId()).block();
      assertThat(restored).isNotNull();
      assertThat(restored.getId()).isEqualTo(originalMemberId);
      assertThat(restored.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
      assertThat(restored.isDeleted()).isFalse();

      Invitation accepted = invitationRepository.findById(invitation.getId()).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("워크스페이스와 프로젝트 모두 탈퇴 후 재초대 시 프로젝트 멤버도 함께 복원된다")
    void reInvite_AfterLeaveWorkspaceAndProject_RestoresBothMemberships() {
      Project project = Project.create(testWorkspace.getId(), "Test Project", "Desc");
      project = projectRepository.save(project).block();

      // invitedUser를 워크스페이스(MEMBER) + 프로젝트(EDITOR) 멤버로 추가 후 둘 다 soft-delete
      WorkspaceMember wsMember = WorkspaceMember.create(
          testWorkspace.getId(), invitedUser.getId(), WorkspaceRole.MEMBER);
      wsMember = memberRepository.save(wsMember).block();
      wsMember.delete();
      memberRepository.save(wsMember).block();

      ProjectMember pm = ProjectMember.create(project.getId(), invitedUser.getId(), ProjectRole.EDITOR);
      pm = projectMemberRepository.save(pm).block();
      pm.delete();
      projectMemberRepository.save(pm).block();

      final String originalWsMemberId = wsMember.getId();
      final String originalProjectMemberId = pm.getId();

      // ADMIN으로 워크스페이스 초대 생성 및 수락
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), invitedUser.getId()).block();

      // 워크스페이스 멤버 복원 확인 (같은 row, ADMIN으로 갱신)
      WorkspaceMember restoredWs = memberRepository
          .findByWorkspaceIdAndUserIdAndNotDeleted(testWorkspace.getId(), invitedUser.getId()).block();
      assertThat(restoredWs).isNotNull();
      assertThat(restoredWs.getId()).isEqualTo(originalWsMemberId);
      assertThat(restoredWs.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);

      // 프로젝트 멤버 복원 확인 (같은 row, ADMIN으로 전파)
      ProjectMember restoredPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitedUser.getId()).block();
      assertThat(restoredPm).isNotNull();
      assertThat(restoredPm.getId()).isEqualTo(originalProjectMemberId);
      assertThat(restoredPm.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    @DisplayName("거부된 초대 이후 동일 이메일로 새 초대를 생성하면 성공한다")
    void reInvite_AfterRejection_NewInvitationSucceeds() {
      // 초대 생성 후 거부
      Invitation invitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.MEMBER, adminUser.getId());
      invitation = invitationRepository.save(invitation).block();
      invitationService.rejectInvitation(invitation.getId(), invitedUser.getId()).block();

      // 거부 후 동일 이메일로 새 초대 생성 → PENDING 아니므로 성공
      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), invitedUser.getEmail(), WorkspaceRole.ADMIN, adminUser.getId()))
          .assertNext(newInvitation -> {
            assertThat(newInvitation.getInvitedEmail()).isEqualTo(invitedUser.getEmail());
            assertThat(newInvitation.getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
            assertThat(newInvitation.getInvitedRole()).isEqualTo(WorkspaceRole.ADMIN.name());
          })
          .verifyComplete();
    }

  }

}
