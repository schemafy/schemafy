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

import com.schemafy.core.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.core.project.exception.ProjectErrorCode;
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
@DisplayName("ProjectInvitationService 테스트")
class ProjectInvitationServiceTest {

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

  private User adminUser;
  private User workspaceUser;
  private User outsider;
  private Workspace testWorkspace;
  private Project testProject;
  private ProjectMember adminProjectMember;

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

    adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    workspaceUser = User.signUp(
        new UserInfo("member@test.com", "Member User", "password"),
        encoder).flatMap(userRepository::save).block();

    outsider = User.signUp(
        new UserInfo("outsider@test.com", "Outsider User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create("Test Workspace", "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    WorkspaceMember wsMemberAdmin = WorkspaceMember.create(
        testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(wsMemberAdmin).block();

    WorkspaceMember wsMember = WorkspaceMember.create(
        testWorkspace.getId(), workspaceUser.getId(), WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(wsMember).block();

    testProject = Project.create(testWorkspace.getId(), "Test Project", "Test Description");
    testProject = projectRepository.save(testProject).block();

    adminProjectMember = ProjectMember.create(
        testProject.getId(), adminUser.getId(), ProjectRole.ADMIN);
    adminProjectMember = projectMemberRepository.save(adminProjectMember).block();
  }

  @Nested
  @DisplayName("초대 생성 (createInvitation)")
  class CreateInvitationTests {

    @Test
    @DisplayName("프로젝트 관리자가 워크스페이스 멤버를 초대하면 성공한다")
    void createInvitation_Success() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceUser.getEmail(), ProjectRole.EDITOR);

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              request.email(),
              request.role(),
              adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getProjectId()).isEqualTo(testProject.getId());
            assertThat(response.getWorkspaceId()).isEqualTo(testWorkspace.getId());
            assertThat(response.getInvitedEmail()).isEqualTo(workspaceUser.getEmail());
            assertThat(response.getInvitedRole()).isEqualTo(ProjectRole.EDITOR.name());
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
            assertThat(response.getInvitedBy()).isEqualTo(adminUser.getId());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이메일이 대문자여도 소문자로 저장된다")
    void createInvitation_EmailLowerCase() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceUser.getEmail().toUpperCase(), ProjectRole.EDITOR);

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              request.email(),
              request.role(),
              adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getInvitedEmail()).isEqualTo(workspaceUser.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 초대에 실패한다")
    void createInvitation_NotAdmin_Fails() {
      ProjectMember editorMember = ProjectMember.create(
          testProject.getId(), workspaceUser.getId(), ProjectRole.EDITOR);
      projectMemberRepository.save(editorMember).block();

      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              request.email(),
              request.role(),
              workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.ADMIN_REQUIRED);
          })
          .verify();
    }

    @Test
    @DisplayName("프로젝트 멤버가 아니면 초대에 실패한다")
    void createInvitation_NotProjectMember_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              request.email(),
              request.role(),
              workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.ACCESS_DENIED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 사용자를 초대하면 실패한다")
    void createInvitation_AlreadyMember_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          adminUser.getEmail(), ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              request.email(),
              request.role(),
              adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
          })
          .verify();
    }

    @Test
    @DisplayName("동일한 이메일에 대한 대기 중인 초대가 있으면 실패한다")
    void createInvitation_DuplicatePending_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceUser.getEmail(), ProjectRole.EDITOR);

      invitationService.createInvitation(
          testProject.getId(), request.email(), request.role(), adminUser.getId()).block();

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(), request.email(), request.role(), adminUser.getId()))
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
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceUser.getEmail(), ProjectRole.EDITOR);

      Invitation expiredInvitation = Invitation.createProjectInvitation(
          testProject.getId(),
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
              testProject.getId(), request.email(), request.role(), adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.name());
            assertThat(response.getId()).isNotEqualTo(expiredInvitationId);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 초대하면 실패한다")
    void createInvitation_UnregisteredEmail_NotFound() {
      String unregisteredEmail = "notyet@test.com";

      StepVerifier.create(
          invitationService.createInvitation(
              testProject.getId(),
              unregisteredEmail,
              ProjectRole.VIEWER,
              adminUser.getId()))
          .expectErrorMatches(DomainException.hasErrorCode(UserErrorCode.NOT_FOUND))
          .verify();
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트에 초대하면 실패한다")
    void createInvitation_ProjectNotFound_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              "nonexistent-project-id", request.email(), request.role(), adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.ACCESS_DENIED);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("초대 목록 조회 (listInvitations)")
  class ListInvitationsTests {

    @Test
    @DisplayName("프로젝트 관리자는 초대 목록을 조회할 수 있다")
    void listInvitations_Success() {
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "user1@test.com",
          ProjectRole.EDITOR,
          adminUser.getId());
      Invitation invitation2 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "user2@test.com",
          ProjectRole.VIEWER,
          adminUser.getId());

      Flux.just(invitation1, invitation2)
          .flatMap(invitationRepository::save)
          .blockLast();

      StepVerifier.create(
          invitationService.getInvitations(
              testProject.getId(), adminUser.getId(), 0, 10))
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
        Invitation invitation = Invitation.createProjectInvitation(
            testProject.getId(),
            testWorkspace.getId(),
            "user" + i + "@test.com",
            ProjectRole.VIEWER,
            adminUser.getId());
        invitationRepository.save(invitation).block();
      }

      StepVerifier.create(
          invitationService.getInvitations(
              testProject.getId(), adminUser.getId(), 0, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();

      StepVerifier.create(
          invitationService.getInvitations(
              testProject.getId(), adminUser.getId(), 2, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 목록 조회에 실패한다")
    void listInvitations_NotAdmin_Fails() {
      ProjectMember editorMember = ProjectMember.create(
          testProject.getId(), workspaceUser.getId(), ProjectRole.EDITOR);
      projectMemberRepository.save(editorMember).block();

      StepVerifier.create(
          invitationService.getInvitations(
              testProject.getId(),
              workspaceUser.getId(),
              0,
              10))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.ADMIN_REQUIRED);
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
      // 다른 프로젝트들 생성
      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2");
      project2 = projectRepository.save(project2).block();
      Project project3 = Project.create(testWorkspace.getId(), "Project 3", "Description 3");
      project3 = projectRepository.save(project3).block();

      // outsider에게 여러 초대 생성
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      Invitation invitation2 = Invitation.createProjectInvitation(
          project2.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.ADMIN, adminUser.getId());
      Invitation invitation3 = Invitation.createProjectInvitation(
          project3.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());

      Flux.just(invitation1, invitation2, invitation3)
          .flatMap(invitationRepository::save)
          .blockLast();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(3);
            assertThat(page.totalElements()).isEqualTo(3);
            assertThat(page.page()).isZero();
            assertThat(page.size()).isEqualTo(20);
            assertThat(page.content())
                .extracting("invitedEmail")
                .containsOnly(outsider.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("대기 중인 초대가 없으면 빈 목록을 반환한다")
    void listMyInvitations_Empty() {
      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
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
        Project project = Project.create(testWorkspace.getId(), "Project " + i, "Description " + i);
        project = projectRepository.save(project).block();
        Invitation invitation = Invitation.createProjectInvitation(
            project.getId(),
            testWorkspace.getId(),
            outsider.getEmail(),
            ProjectRole.VIEWER,
            adminUser.getId());
        invitationRepository.save(invitation).block();
      }

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 2, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("수락/거절된 초대는 목록에 포함되지 않는다")
    void listMyInvitations_OnlyPending() {
      Invitation pending = Invitation.createProjectInvitation(
          testProject.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      pending = invitationRepository.save(pending).block();
      final String pendingId = pending.getId();

      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2");
      project2 = projectRepository.save(project2).block();
      Invitation accepted = Invitation.createProjectInvitation(
          project2.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      Project project3 = Project.create(testWorkspace.getId(), "Project 3", "Description 3");
      project3 = projectRepository.save(project3).block();
      Invitation rejected = Invitation.createProjectInvitation(
          project3.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      rejected = invitationRepository.save(rejected).block();
      rejected.reject();
      invitationRepository.save(rejected).block();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
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
      Invitation activePending = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          outsider.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      activePending = invitationRepository.save(activePending).block();
      final String activePendingId = activePending.getId();

      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2");
      project2 = projectRepository.save(project2).block();
      Invitation expiredPending = Invitation.createProjectInvitation(
          project2.getId(),
          testWorkspace.getId(),
          outsider.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      expiredPending = invitationRepository.save(expiredPending).block();
      ReflectionTestUtils.setField(expiredPending, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(expiredPending).block();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
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
      Invitation myInvitation = Invitation.createProjectInvitation(
          testProject.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      invitationRepository.save(myInvitation).block();

      Invitation otherInvitation = Invitation.createProjectInvitation(
          testProject.getId(), testWorkspace.getId(), workspaceUser.getEmail(), ProjectRole.VIEWER, adminUser
              .getId());
      invitationRepository.save(otherInvitation).block();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).getInvitedEmail()).isEqualTo(outsider.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 실패한다")
    void listMyInvitations_UserNotFound_Fails() {
      StepVerifier.create(
          invitationService.getMyInvitations("nonexistent-user-id", 0, 20))
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
    @DisplayName("워크스페이스 멤버가 프로젝트 초대를 수락하면 프로젝트 멤버가 생성된다")
    void acceptInvitation_Success() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, workspaceUser.getId()))
          .assertNext(member -> {
            assertThat(member.member().getUserId()).isEqualTo(workspaceUser.getId());
            assertThat(member.member().getRole()).isEqualTo(ProjectRole.EDITOR.name());
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
          invitationService.acceptInvitation("nonexistent-id", workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("삭제된 프로젝트의 초대는 수락할 수 없다")
    void acceptInvitation_DeletedProject_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      testProject.delete();
      projectRepository.save(testProject).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void acceptInvitation_EmailMismatch_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "other@test.com",
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 경우 초대 수락에 실패한다")
    void acceptInvitation_AlreadyProjectMember_Fails() {
      ProjectMember existingMember = ProjectMember.create(
          testProject.getId(), workspaceUser.getId(), ProjectRole.VIEWER);
      projectMemberRepository.save(existingMember).block();

      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode())
                .isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
          })
          .verify();
    }

    @Test
    @DisplayName("만료된 초대를 수락하면 실패한다")
    void acceptInvitation_Expired_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      ReflectionTestUtils.setField(invitation, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
          })
          .verify();
    }

    @Test
    @DisplayName("거절된 초대를 수락하면 실패한다")
    void acceptInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 수락에 실패한다")
    void acceptInvitation_UserNotFound_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "newuser@test.com",
          ProjectRole.EDITOR,
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
      // 같은 프로젝트, 같은 이메일로 pending 초대 2개 직접 생성 (서비스 중복 체크 우회)
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      Invitation invitation2 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      invitation2 = invitationRepository.save(invitation2).block();

      String acceptedId = invitation1.getId();
      String otherPendingId = invitation2.getId();

      // invitation1 수락
      invitationService.acceptInvitation(acceptedId, workspaceUser.getId()).block();

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
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      Invitation expiredPending = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      expiredPending = invitationRepository.save(expiredPending).block();
      ReflectionTestUtils.setField(expiredPending, "expiresAt", Instant.now().minusSeconds(3600));
      invitationRepository.save(expiredPending).block();

      invitationService.acceptInvitation(invitation1.getId(), workspaceUser.getId()).block();

      Invitation accepted = invitationRepository.findById(invitation1.getId()).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      Invitation expiredOther = invitationRepository.findById(expiredPending.getId()).block();
      assertThat(expiredOther.getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
      assertThat(expiredOther.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("초대 수락 시 다른 프로젝트의 pending 초대는 영향받지 않는다")
    void acceptInvitation_DoesNotAffectOtherProjectInvitations() {
      // 다른 프로젝트 생성
      Project otherProject = Project.create(testWorkspace.getId(), "Other Project", "Desc");
      otherProject = projectRepository.save(otherProject).block();

      // 현재 프로젝트에 초대
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      // 다른 프로젝트에 같은 이메일로 초대
      Invitation otherProjectInvitation = Invitation.createProjectInvitation(
          otherProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      otherProjectInvitation = invitationRepository.save(otherProjectInvitation).block();

      // invitation1 수락
      invitationService.acceptInvitation(invitation1.getId(), workspaceUser.getId()).block();

      // 다른 프로젝트의 초대는 여전히 PENDING
      Invitation otherInv = invitationRepository.findById(otherProjectInvitation.getId()).block();
      assertThat(otherInv.getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
    }

  }

  @Nested
  @DisplayName("초대 거절 (rejectInvitation)")
  class RejectInvitationTests {

    @Test
    @DisplayName("유효한 초대를 거절하면 성공한다")
    void rejectInvitation_Success() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, workspaceUser.getId()))
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void rejectInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.rejectInvitation("nonexistent-id", workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_NOT_FOUND);
          })
          .verify();
    }

    @Test
    @DisplayName("삭제된 프로젝트의 초대도 거절할 수 있다")
    void rejectInvitation_DeletedProject_Succeeds() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      testProject.delete();
      projectRepository.save(testProject).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()))
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대 이메일과 사용자 이메일이 다르면 실패한다")
    void rejectInvitation_EmailMismatch_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "other@test.com",
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 실패한다")
    void rejectInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED);
          })
          .verify();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 초대 거절에 실패한다")
    void rejectInvitation_UserNotFound_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "newuser@test.com",
          ProjectRole.EDITOR,
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
    @DisplayName("WORKSPACE 타입 초대를 Project 서비스로 수락하면 실패한다")
    void acceptWorkspaceInvitation_WithProjectService_Fails() {
      Invitation workspaceInvitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          outsider.getEmail(),
          com.schemafy.core.project.repository.vo.WorkspaceRole.MEMBER,
          adminUser.getId());
      workspaceInvitation = invitationRepository.save(workspaceInvitation).block();
      final String invitationId = workspaceInvitation.getId();

      // ProjectInvitationService로 수락 시도
      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, outsider.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("WORKSPACE 타입 초대를 Project 서비스로 거절하면 실패한다")
    void rejectWorkspaceInvitation_WithProjectService_Fails() {
      Invitation workspaceInvitation = Invitation.createWorkspaceInvitation(
          testWorkspace.getId(),
          outsider.getEmail(),
          com.schemafy.core.project.repository.vo.WorkspaceRole.MEMBER,
          adminUser.getId());
      workspaceInvitation = invitationRepository.save(workspaceInvitation).block();
      final String invitationId = workspaceInvitation.getId();

      // ProjectInvitationService로 거절 시도
      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, outsider.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(DomainException.class);
            DomainException be = (DomainException) error;
            assertThat(be.getErrorCode()).isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
          })
          .verify();
    }

  }

  @Nested
  @DisplayName("재초대 시나리오")
  class ReInvitationTests {

    @Test
    @DisplayName("제거된 프로젝트 멤버를 초대하면 기존 레코드가 복원되고 역할이 갱신된다")
    void reInvite_AfterRemoval_RestoresMember() {
      // workspaceMember를 프로젝트에 EDITOR 역할로 추가 후 soft-delete
      ProjectMember existingMember = ProjectMember.create(
          testProject.getId(), workspaceUser.getId(), ProjectRole.EDITOR);
      existingMember = projectMemberRepository.save(existingMember).block();
      existingMember.delete();
      projectMemberRepository.save(existingMember).block();
      final String originalMemberId = existingMember.getId();

      // VIEWER 역할로 초대 생성 후 수락
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceUser.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();
      invitationService.acceptInvitation(invitation.getId(), workspaceUser.getId()).block();

      // 기존 row가 복원됐는지 확인 (새 row가 아닌 같은 ID)
      ProjectMember restored = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(testProject.getId(), workspaceUser.getId())
          .block();
      assertThat(restored).isNotNull();
      assertThat(restored.getId()).isEqualTo(originalMemberId);
      assertThat(restored.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
      assertThat(restored.isDeleted()).isFalse();

      Invitation accepted = invitationRepository.findById(invitation.getId()).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
    }

  }

}
