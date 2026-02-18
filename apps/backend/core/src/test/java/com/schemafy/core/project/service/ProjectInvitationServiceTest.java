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
import com.schemafy.core.project.controller.dto.request.CreateProjectInvitationRequest;
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
  private User workspaceMember;
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

    workspaceMember = User.signUp(
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
        testWorkspace.getId(), workspaceMember.getId(), WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(wsMember).block();

    testProject = Project.create(
        testWorkspace.getId(),
        "Test Project",
        "Test Description",
        ProjectSettings.defaultSettings());
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
          workspaceMember.getEmail(), ProjectRole.EDITOR);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(),
              testProject.getId(),
              request,
              adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getProjectId()).isEqualTo(testProject.getId());
            assertThat(response.getWorkspaceId()).isEqualTo(testWorkspace.getId());
            assertThat(response.getInvitedEmail()).isEqualTo(workspaceMember.getEmail());
            assertThat(response.getInvitedRole()).isEqualTo(ProjectRole.EDITOR.getValue());
            assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING.getValue());
            assertThat(response.getInvitedBy()).isEqualTo(adminUser.getId());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이메일이 대문자여도 소문자로 저장된다")
    void createInvitation_EmailLowerCase() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceMember.getEmail().toUpperCase(), ProjectRole.EDITOR);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(),
              testProject.getId(),
              request,
              adminUser.getId()))
          .assertNext(response -> {
            assertThat(response.getInvitedEmail()).isEqualTo(workspaceMember.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 초대에 실패한다")
    void createInvitation_NotAdmin_Fails() {
      ProjectMember editorMember = ProjectMember.create(
          testProject.getId(), workspaceMember.getId(), ProjectRole.EDITOR);
      projectMemberRepository.save(editorMember).block();

      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(),
              testProject.getId(),
              request,
              workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ADMIN_REQUIRED);
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
              testWorkspace.getId(),
              testProject.getId(),
              request,
              workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
          })
          .verify();
    }

    @Test
    @DisplayName("워크스페이스와 프로젝트가 일치하지 않으면 실패한다")
    void createInvitation_WorkspaceMismatch_Fails() {
      Workspace otherWorkspace = Workspace.create("Other Workspace", "Other");
      otherWorkspace = workspaceRepository.save(otherWorkspace).block();

      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              otherWorkspace.getId(),
              testProject.getId(),
              request,
              adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_WORKSPACE_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("동일한 이메일에 대한 대기 중인 초대가 있으면 실패한다")
    void createInvitation_DuplicatePending_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(
          workspaceMember.getEmail(), ProjectRole.EDITOR);

      invitationService.createInvitation(
          testWorkspace.getId(), testProject.getId(), request, adminUser.getId()).block();

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), testProject.getId(), request, adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_ALREADY_EXISTS);
          })
          .verify();
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트에 초대하면 실패한다")
    void createInvitation_ProjectNotFound_Fails() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      StepVerifier.create(
          invitationService.createInvitation(
              testWorkspace.getId(), "nonexistent-project-id", request, adminUser.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
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
              testWorkspace.getId(), testProject.getId(), adminUser.getId(), 0, 10))
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
              testWorkspace.getId(), testProject.getId(), adminUser.getId(), 0, 2))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(2);
            assertThat(page.totalElements()).isEqualTo(5);
          })
          .verifyComplete();

      StepVerifier.create(
          invitationService.getInvitations(
              testWorkspace.getId(), testProject.getId(), adminUser.getId(), 2, 2))
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
          testProject.getId(), workspaceMember.getId(), ProjectRole.EDITOR);
      projectMemberRepository.save(editorMember).block();

      StepVerifier.create(
          invitationService.getInvitations(
              testWorkspace.getId(),
              testProject.getId(),
              workspaceMember.getId(),
              0,
              10))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_ADMIN_REQUIRED);
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
      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2", ProjectSettings
          .defaultSettings());
      project2 = projectRepository.save(project2).block();
      Project project3 = Project.create(testWorkspace.getId(), "Project 3", "Description 3", ProjectSettings
          .defaultSettings());
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
        Project project = Project.create(testWorkspace.getId(), "Project " + i, "Description " + i, ProjectSettings
            .defaultSettings());
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

      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2", ProjectSettings
          .defaultSettings());
      project2 = projectRepository.save(project2).block();
      Invitation accepted = Invitation.createProjectInvitation(
          project2.getId(), testWorkspace.getId(), outsider.getEmail(), ProjectRole.VIEWER, adminUser.getId());
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      Project project3 = Project.create(testWorkspace.getId(), "Project 3", "Description 3", ProjectSettings
          .defaultSettings());
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
            assertThat(page.content().get(0).id()).isEqualTo(pendingId);
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
          testProject.getId(), testWorkspace.getId(), workspaceMember.getEmail(), ProjectRole.VIEWER, adminUser
              .getId());
      invitationRepository.save(otherInvitation).block();

      StepVerifier.create(
          invitationService.getMyInvitations(outsider.getId(), 0, 20))
          .assertNext(page -> {
            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).invitedEmail()).isEqualTo(outsider.getEmail());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 실패한다")
    void listMyInvitations_UserNotFound_Fails() {
      StepVerifier.create(
          invitationService.getMyInvitations("nonexistent-user-id", 0, 20))
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
    @DisplayName("워크스페이스 멤버가 프로젝트 초대를 수락하면 프로젝트 멤버가 생성된다")
    void acceptInvitation_Success() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.acceptInvitation(invitationId, workspaceMember.getId()))
          .assertNext(member -> {
            assertThat(member.userId()).isEqualTo(workspaceMember.getId());
            assertThat(member.role()).isEqualTo(ProjectRole.EDITOR.getValue());
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
          invitationService.acceptInvitation("nonexistent-id", workspaceMember.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "other@test.com",
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_EMAIL_MISMATCH);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 경우 초대 수락에 실패한다")
    void acceptInvitation_AlreadyProjectMember_Fails() {
      ProjectMember existingMember = ProjectMember.create(
          testProject.getId(), workspaceMember.getId(), ProjectRole.VIEWER);
      projectMemberRepository.save(existingMember).block();

      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode())
                .isEqualTo(ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
          })
          .verify();
    }

    @Test
    @DisplayName("만료된 초대를 수락하면 실패한다")
    void acceptInvitation_Expired_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      try {
        var field = invitation.getClass().getDeclaredField("expiresAt");
        field.setAccessible(true);
        field.set(invitation, Instant.now().minusSeconds(3600));
      } catch (Exception e) {}
      invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
          })
          .verify();
    }

    @Test
    @DisplayName("거절된 초대를 수락하면 실패한다")
    void acceptInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), workspaceMember.getId()).block();

      StepVerifier.create(
          invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION);
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
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
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
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      Invitation invitation2 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      invitation2 = invitationRepository.save(invitation2).block();

      String acceptedId = invitation1.getId();
      String otherPendingId = invitation2.getId();

      // invitation1 수락
      invitationService.acceptInvitation(acceptedId, workspaceMember.getId()).block();

      // 수락한 초대는 ACCEPTED
      Invitation accepted = invitationRepository.findById(acceptedId).block();
      assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      // 다른 pending 초대는 CANCELLED
      Invitation other = invitationRepository.findById(otherPendingId).block();
      assertThat(other.getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
      assertThat(other.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대 수락 시 다른 프로젝트의 pending 초대는 영향받지 않는다")
    void acceptInvitation_DoesNotAffectOtherProjectInvitations() {
      // 다른 프로젝트 생성
      Project otherProject = Project.create(
          testWorkspace.getId(), "Other Project", "Desc",
          ProjectSettings.defaultSettings());
      otherProject = projectRepository.save(otherProject).block();

      // 현재 프로젝트에 초대
      Invitation invitation1 = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation1 = invitationRepository.save(invitation1).block();

      // 다른 프로젝트에 같은 이메일로 초대
      Invitation otherProjectInvitation = Invitation.createProjectInvitation(
          otherProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.VIEWER,
          adminUser.getId());
      otherProjectInvitation = invitationRepository.save(otherProjectInvitation).block();

      // invitation1 수락
      invitationService.acceptInvitation(invitation1.getId(), workspaceMember.getId()).block();

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
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      String invitationId = invitation.getId();

      StepVerifier.create(
          invitationService.rejectInvitation(invitationId, workspaceMember.getId()))
          .verifyComplete();

      Invitation updated = invitationRepository.findById(invitationId).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
      assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대가 존재하지 않으면 실패한다")
    void rejectInvitation_NotFound_Fails() {
      StepVerifier.create(
          invitationService.rejectInvitation("nonexistent-id", workspaceMember.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          "other@test.com",
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceMember.getId()))
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
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.acceptInvitation(invitation.getId(), workspaceMember.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION);
          })
          .verify();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 실패한다")
    void rejectInvitation_AlreadyRejected_Fails() {
      Invitation invitation = Invitation.createProjectInvitation(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUser.getId());
      invitation = invitationRepository.save(invitation).block();

      invitationService.rejectInvitation(invitation.getId(), workspaceMember.getId()).block();

      StepVerifier.create(
          invitationService.rejectInvitation(invitation.getId(), workspaceMember.getId()))
          .expectErrorSatisfies(error -> {
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION);
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
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
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
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_TYPE_MISMATCH);
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
            assertThat(error).isInstanceOf(BusinessException.class);
            BusinessException be = (BusinessException) error;
            assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVITATION_TYPE_MISMATCH);
          })
          .verify();
    }

  }

}
