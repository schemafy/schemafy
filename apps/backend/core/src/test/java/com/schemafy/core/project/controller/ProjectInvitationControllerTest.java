package com.schemafy.core.project.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.core.project.docs.ProjectInvitationApiSnippets;
import com.schemafy.core.project.repository.ProjectInvitationRepository;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectInvitation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ProjectInvitationController 통합 테스트")
class ProjectInvitationControllerTest {

  private static final String API_BASE = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  @Autowired
  private ProjectInvitationRepository invitationRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtProvider jwtProvider;

  private String adminUserId;
  private String workspaceMemberId;
  private String outsiderId;
  private String adminToken;
  private String workspaceMemberToken;
  private String outsiderToken;
  private Workspace testWorkspace;
  private Project testProject;

  @BeforeEach
  void setUp() {
    Mono<Void> cleanup = Mono.when(
        invitationRepository.deleteAll(),
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll());

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    cleanup.then(Mono.zip(
        User.signUp(new UserInfo("admin@test.com", "Admin", "password"), encoder)
            .flatMap(userRepository::save),
        User.signUp(new UserInfo("member@test.com", "Member", "password"), encoder)
            .flatMap(userRepository::save),
        User.signUp(new UserInfo("outsider@test.com", "Outsider", "password"), encoder)
            .flatMap(userRepository::save)))
        .doOnNext(users -> {
          adminUserId = users.getT1().getId();
          workspaceMemberId = users.getT2().getId();
          outsiderId = users.getT3().getId();

          adminToken = generateToken(adminUserId);
          workspaceMemberToken = generateToken(workspaceMemberId);
          outsiderToken = generateToken(outsiderId);
        })
        .block();

    testWorkspace = Workspace.create("Test Workspace", "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    WorkspaceMember adminWsMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUserId, WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(adminWsMember).block();

    WorkspaceMember normalWsMember = WorkspaceMember.create(
        testWorkspace.getId(), workspaceMemberId, WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(normalWsMember).block();

    testProject = Project.create(
        testWorkspace.getId(),
        "Test Project",
        "Test Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();

    ProjectMember adminProjMember = ProjectMember.create(
        testProject.getId(), adminUserId, ProjectRole.ADMIN);
    projectMemberRepository.save(adminProjMember).block();
  }

  private String generateToken(String userId) {
    return jwtProvider.generateAccessToken(userId, new HashMap<>(),
        System.currentTimeMillis());
  }

  @Nested
  @DisplayName("POST /api/v1.0/workspaces/{workspaceId}/projects/{projectId}/invitations - 초대 생성")
  class CreateInvitationTests {

    @Test
    @DisplayName("프로젝트 관리자가 워크스페이스 멤버를 초대하면 201 Created를 반환한다")
    void createInvitation_Success() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(workspaceMember.getEmail(),
          ProjectRole.EDITOR);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              testWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated()
          .expectBody()
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.projectId").isEqualTo(testProject.getId())
          .jsonPath("$.result.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.result.invitedEmail").isEqualTo(workspaceMember.getEmail())
          .jsonPath("$.result.invitedRole").isEqualTo(ProjectRole.EDITOR.getValue())
          .jsonPath("$.result.status").isEqualTo(InvitationStatus.PENDING.getValue());
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 403 Forbidden을 반환한다")
    void createInvitation_NotAdmin_Forbidden() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              testWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식이면 400 Bad Request를 반환한다")
    void createInvitation_InvalidEmail_BadRequest() {
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("invalid-email", ProjectRole.VIEWER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              testWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("워크스페이스와 프로젝트가 일치하지 않으면 400 Bad Request를 반환한다")
    void createInvitation_WorkspaceMismatch_BadRequest() {
      Workspace otherWorkspace = Workspace.create("Other Workspace", "Other");
      otherWorkspace = workspaceRepository.save(otherWorkspace).block();

      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest("newuser@test.com",
          ProjectRole.VIEWER);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              otherWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("중복 초대 시 409 Conflict를 반환한다")
    void createInvitation_Duplicate_Conflict() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      CreateProjectInvitationRequest request = new CreateProjectInvitationRequest(workspaceMember.getEmail(),
          ProjectRole.EDITOR);

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              testWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated();

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations",
              testWorkspace.getId(), testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("GET /api/v1.0/workspaces/{workspaceId}/projects/{projectId}/invitations - 초대 목록 조회")
  class ListInvitationsTests {

    @Test
    @DisplayName("프로젝트 관리자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listInvitations_Success() {
      ProjectInvitation invitation1 = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          "user1@test.com",
          ProjectRole.EDITOR,
          adminUserId);
      ProjectInvitation invitation2 = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          "user2@test.com",
          ProjectRole.VIEWER,
          adminUserId);
      invitationRepository.save(invitation1).block();
      invitationRepository.save(invitation2).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build(testWorkspace.getId(), testProject.getId()))
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(2)
          .jsonPath("$.result.page").isEqualTo(0)
          .jsonPath("$.result.size").isEqualTo(10);
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 403 Forbidden을 반환한다")
    void listInvitations_NotAdmin_Forbidden() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build(testWorkspace.getId(), testProject.getId()))
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        ProjectInvitation invitation = ProjectInvitation.create(
            testProject.getId(),
            testWorkspace.getId(),
            "user" + i + "@test.com",
            ProjectRole.VIEWER,
            adminUserId);
        invitationRepository.save(invitation).block();
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/projects/{projectId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build(testWorkspace.getId(), testProject.getId()))
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(5);
    }

  }

  @Nested
  @DisplayName("내 초대 목록 조회")
  class ListMyInvitationsTests {

    @Test
    @DisplayName("인증된 사용자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listMyInvitations_Success() {
      User outsiderUser = userRepository.findById(outsiderId).block();

      // 여러 프로젝트에서 초대 생성
      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2", ProjectSettings
          .defaultSettings());
      project2 = projectRepository.save(project2).block();

      ProjectInvitation invitation1 = ProjectInvitation.create(
          testProject.getId(), testWorkspace.getId(), outsiderUser.getEmail(), ProjectRole.EDITOR, adminUserId);
      ProjectInvitation invitation2 = ProjectInvitation.create(
          project2.getId(), testWorkspace.getId(), outsiderUser.getEmail(), ProjectRole.VIEWER, adminUserId);
      invitationRepository.save(invitation1).block();
      invitationRepository.save(invitation2).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/projects")
              .queryParam("page", 0)
              .queryParam("size", 20)
              .build())
          .header("Authorization", "Bearer " + outsiderToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("project-invitation-list-my",
              ProjectInvitationApiSnippets.listMyInvitationsRequestHeaders(),
              ProjectInvitationApiSnippets.listMyInvitationsQueryParameters(),
              ProjectInvitationApiSnippets.listMyInvitationsResponseHeaders(),
              ProjectInvitationApiSnippets.listMyInvitationsResponse()))
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(2)
          .jsonPath("$.result.content[0].invitedEmail").isEqualTo(outsiderUser.getEmail())
          .jsonPath("$.result.content[1].invitedEmail").isEqualTo(outsiderUser.getEmail());
    }

    @Test
    @DisplayName("초대가 없으면 빈 목록을 반환한다")
    void listMyInvitations_Empty() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/projects")
              .queryParam("page", 0)
              .queryParam("size", 20)
              .build())
          .header("Authorization", "Bearer " + outsiderToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.content.length()").isEqualTo(0)
          .jsonPath("$.result.totalElements").isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listMyInvitations_Pagination() {
      User outsiderUser = userRepository.findById(outsiderId).block();

      for (int i = 0; i < 5; i++) {
        Project project = Project.create(testWorkspace.getId(), "Project " + i, "Description " + i, ProjectSettings
            .defaultSettings());
        project = projectRepository.save(project).block();
        ProjectInvitation invitation = ProjectInvitation.create(
            project.getId(), testWorkspace.getId(), outsiderUser.getEmail(), ProjectRole.VIEWER, adminUserId);
        invitationRepository.save(invitation).block();
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/projects")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build())
          .header("Authorization", "Bearer " + outsiderToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(2)
          .jsonPath("$.result.totalElements").isEqualTo(5);
    }

    @Test
    @DisplayName("PENDING 상태의 초대만 조회된다")
    void listMyInvitations_OnlyPending() {
      User outsiderUser = userRepository.findById(outsiderId).block();

      ProjectInvitation pending = ProjectInvitation.create(
          testProject.getId(), testWorkspace.getId(), outsiderUser.getEmail(), ProjectRole.VIEWER, adminUserId);
      pending = invitationRepository.save(pending).block();

      Project project2 = Project.create(testWorkspace.getId(), "Project 2", "Description 2", ProjectSettings
          .defaultSettings());
      project2 = projectRepository.save(project2).block();
      ProjectInvitation accepted = ProjectInvitation.create(
          project2.getId(), testWorkspace.getId(), outsiderUser.getEmail(), ProjectRole.VIEWER, adminUserId);
      accepted = invitationRepository.save(accepted).block();
      accepted.accept();
      invitationRepository.save(accepted).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/projects")
              .queryParam("page", 0)
              .queryParam("size", 20)
              .build())
          .header("Authorization", "Bearer " + outsiderToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.result.content.length()").isEqualTo(1)
          .jsonPath("$.result.content[0].id").isEqualTo(pending.getId());
    }

    @Test
    @DisplayName("인증 토큰 없이 요청하면 401 Unauthorized를 반환한다")
    void listMyInvitations_NoAuth_Unauthorized() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/projects")
              .queryParam("page", 0)
              .queryParam("size", 20)
              .build())
          .exchange()
          .expectStatus().isUnauthorized();
    }

  }

  @Nested
  @DisplayName("프로젝트 초대 수락")
  class AcceptInvitationTests {

    @Test
    @DisplayName("워크스페이스 멤버가 프로젝트 초대를 수락하면 200 OK를 반환하고 프로젝트 멤버가 생성된다")
    void acceptInvitation_Success() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.success").isEqualTo(true)
          .jsonPath("$.result.userId").isEqualTo(workspaceMemberId)
          .jsonPath("$.result.projectId").isEqualTo(testProject.getId())
          .jsonPath("$.result.role").isEqualTo(ProjectRole.EDITOR.getValue());

      ProjectInvitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      boolean memberExists = projectMemberRepository
          .existsByProjectIdAndUserIdAndNotDeleted(testProject.getId(), workspaceMemberId)
          .block();
      assertThat(memberExists).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 초대를 수락하면 403 Forbidden을 반환한다")
    void acceptInvitation_WrongUser_Forbidden() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 수락하면 404 Not Found를 반환한다")
    void acceptInvitation_NotFound() {
      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              "nonexistent-id")
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyAccepted_Conflict() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 경우 초대 수락에 실패하고 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyProjectMember_Conflict() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();

      ProjectMember existingMember = ProjectMember.create(
          testProject.getId(), workspaceMemberId, ProjectRole.VIEWER);
      projectMemberRepository.save(existingMember).block();

      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("프로젝트 초대 거절")
  class RejectInvitationTests {

    @Test
    @DisplayName("초대받은 사용자가 초대를 거절하면 204 No Content를 반환한다")
    void rejectInvitation_Success() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNoContent();

      ProjectInvitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    @DisplayName("다른 사용자가 초대를 거절하면 403 Forbidden을 반환한다")
    void rejectInvitation_WrongUser_Forbidden() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 거절하면 404 Not Found를 반환한다")
    void rejectInvitation_NotFound() {
      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              "nonexistent-id")
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyRejected_Conflict() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyAccepted_Conflict() {
      User workspaceMember = userRepository.findById(workspaceMemberId).block();
      ProjectInvitation invitation = ProjectInvitation.create(
          testProject.getId(),
          testWorkspace.getId(),
          workspaceMember.getEmail(),
          ProjectRole.EDITOR,
          adminUserId);
      invitation = invitationRepository.save(invitation).block();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.put()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

}
