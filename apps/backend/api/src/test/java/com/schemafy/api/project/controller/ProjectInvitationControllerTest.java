package com.schemafy.api.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.api.project.docs.ProjectInvitationApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ProjectInvitationController 통합 테스트")
class ProjectInvitationControllerTest extends ProjectHttpTestSupport {

  private static final String API_BASE = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

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
    cleanupProjectFixtures().block();

    User admin = createUser("admin@test.com", "Admin");
    User member = createUser("member@test.com", "Member");
    User outsider = createUser("outsider@test.com", "Outsider");

    adminUserId = admin.id();
    workspaceMemberId = member.id();
    outsiderId = outsider.id();

    adminToken = generateAccessToken(adminUserId);
    workspaceMemberToken = generateAccessToken(workspaceMemberId);
    outsiderToken = generateAccessToken(outsiderId);

    testWorkspace = saveWorkspace("Test Workspace", "Test Description");
    addWorkspaceMember(testWorkspace.getId(), adminUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(testWorkspace.getId(), workspaceMemberId,
        WorkspaceRole.MEMBER);
    testProject = saveProject(testWorkspace.getId(), "Test Project",
        "Test Description");
    addProjectMember(testProject.getId(), adminUserId, ProjectRole.ADMIN);
  }

  private CreateProjectInvitationRequest createProjectInvitationRequest(
      String email,
      String role) {
    return new CreateProjectInvitationRequest(email,
        resolveRecordEnum(CreateProjectInvitationRequest.class, 1, role));
  }

  @SuppressWarnings("unchecked")
  private <E extends Enum<E>> E resolveRecordEnum(
      Class<?> recordType,
      int componentIndex,
      String value) {
    return Enum.valueOf((Class<E>) recordType.getRecordComponents()[componentIndex]
        .getType(), value);
  }

  @Nested
  @DisplayName("POST /api/v1.0/projects/{projectId}/invitations - 초대 생성")
  class CreateInvitationTests {

    @Test
    @DisplayName("프로젝트 관리자가 워크스페이스 멤버를 초대하면 201 Created를 반환한다")
    void createInvitation_Success() {
      User workspaceMember = getUser(workspaceMemberId);
      CreateProjectInvitationRequest request = createProjectInvitationRequest(workspaceMember.email(),
          "EDITOR");

      webTestClient.post()
          .uri(API_BASE + "/projects/{projectId}/invitations",
              testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated()
          .expectBody()
          .consumeWith(document("project-invitation-create",
              ProjectInvitationApiSnippets.createInvitationRequestHeaders(),
              ProjectInvitationApiSnippets.createInvitationPathParameters(),
              ProjectInvitationApiSnippets.createInvitationRequest(),
              ProjectInvitationApiSnippets.createInvitationResponseHeaders(),
              ProjectInvitationApiSnippets.createInvitationResponse()))
          .jsonPath("$.projectId").isEqualTo(testProject.getId())
          .jsonPath("$.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.invitedEmail").isEqualTo(workspaceMember.email())
          .jsonPath("$.invitedRole").isEqualTo(ProjectRole.EDITOR.name())
          .jsonPath("$.status").isEqualTo(InvitationStatus.PENDING.name());
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 403 Forbidden을 반환한다")
    void createInvitation_NotAdmin_Forbidden() {
      CreateProjectInvitationRequest request = createProjectInvitationRequest("newuser@test.com",
          "VIEWER");

      webTestClient.post()
          .uri(API_BASE + "/projects/{projectId}/invitations",
              testProject.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식이면 400 Bad Request를 반환한다")
    void createInvitation_InvalidEmail_BadRequest() {
      CreateProjectInvitationRequest request = createProjectInvitationRequest("invalid-email",
          "VIEWER");

      webTestClient.post()
          .uri(API_BASE + "/projects/{projectId}/invitations",
              testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("중복 초대 시 409 Conflict를 반환한다")
    void createInvitation_Duplicate_Conflict() {
      User workspaceMember = getUser(workspaceMemberId);
      CreateProjectInvitationRequest request = createProjectInvitationRequest(workspaceMember.email(),
          "EDITOR");

      webTestClient.post()
          .uri(API_BASE + "/projects/{projectId}/invitations",
              testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated();

      webTestClient.post()
          .uri(API_BASE + "/projects/{projectId}/invitations",
              testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("GET /api/v1.0/projects/{projectId}/invitations - 초대 목록 조회")
  class ListInvitationsTests {

    @Test
    @DisplayName("프로젝트 관리자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listInvitations_Success() {
      saveProjectInvitation(testProject.getId(), testWorkspace.getId(),
          "user1@test.com", ProjectRole.EDITOR, adminUserId);
      saveProjectInvitation(testProject.getId(), testWorkspace.getId(),
          "user2@test.com", ProjectRole.VIEWER, adminUserId);

      webTestClient.get()
          .uri(API_BASE + "/projects/{projectId}/invitations?page=0&size=10",
              testProject.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("project-invitation-list",
              ProjectInvitationApiSnippets.listInvitationsRequestHeaders(),
              ProjectInvitationApiSnippets.listInvitationsPathParameters(),
              ProjectInvitationApiSnippets.listInvitationsQueryParameters(),
              ProjectInvitationApiSnippets.listInvitationsResponseHeaders(),
              ProjectInvitationApiSnippets.listInvitationsResponse()))
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.page").isEqualTo(0)
          .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    @DisplayName("프로젝트 관리자가 아니면 403 Forbidden을 반환한다")
    void listInvitations_NotAdmin_Forbidden() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/projects/{projectId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build(testProject.getId()))
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        saveProjectInvitation(testProject.getId(), testWorkspace.getId(),
            "user" + i + "@test.com", ProjectRole.VIEWER, adminUserId);
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/projects/{projectId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build(testProject.getId()))
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(5);
    }

    @DisplayName("프로젝트 초대 목록 조회는 잘못된 pagination 쿼리에 대해 400 Bad Request를 반환한다")
    @ParameterizedTest(name = "프로젝트 초대 목록 조회 실패 쿼리: {0}")
    @ValueSource(strings = {
      "?page=-1&size=10",
      "?page=0&size=0",
      "?page=0&size=101"
    })
    void listInvitations_InvalidPagination_BadRequest(
        String query) {
      assertInvalidPagination(
          webTestClient,
          API_BASE + "/projects/" + testProject.getId()
              + "/invitations" + query,
          adminToken);
    }

  }

  @Nested
  @DisplayName("내 초대 목록 조회")
  class ListMyInvitationsTests {

    @Test
    @DisplayName("인증된 사용자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listMyInvitations_Success() {
      User outsiderUser = getUser(outsiderId);
      Project project2 = saveProject(testWorkspace.getId(), "Project 2",
          "Description 2");

      saveProjectInvitation(testProject.getId(), testWorkspace.getId(),
          outsiderUser.email(), ProjectRole.EDITOR, adminUserId);
      saveProjectInvitation(project2.getId(), testWorkspace.getId(),
          outsiderUser.email(), ProjectRole.VIEWER, adminUserId);

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
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.content[0].invitedEmail").isEqualTo(outsiderUser.email())
          .jsonPath("$.content[1].invitedEmail").isEqualTo(outsiderUser.email());
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
          .jsonPath("$.content.length()").isEqualTo(0)
          .jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listMyInvitations_Pagination() {
      User outsiderUser = getUser(outsiderId);

      for (int i = 0; i < 5; i++) {
        Project project = saveProject(testWorkspace.getId(), "Project " + i,
            "Description " + i);
        saveProjectInvitation(project.getId(), testWorkspace.getId(),
            outsiderUser.email(), ProjectRole.VIEWER, adminUserId);
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
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(5);
    }

    @Test
    @DisplayName("PENDING 상태의 초대만 조회된다")
    void listMyInvitations_OnlyPending() {
      User outsiderUser = getUser(outsiderId);

      Invitation pending = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), outsiderUser.email(), ProjectRole.VIEWER,
          adminUserId);

      Project project2 = saveProject(testWorkspace.getId(), "Project 2",
          "Description 2");
      Invitation accepted = saveProjectInvitation(project2.getId(),
          testWorkspace.getId(), outsiderUser.email(), ProjectRole.VIEWER,
          adminUserId);
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
          .jsonPath("$.content.length()").isEqualTo(1)
          .jsonPath("$.content[0].id").isEqualTo(pending.getId());
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

    @DisplayName("내 프로젝트 초대 목록 조회는 잘못된 pagination 쿼리에 대해 400 Bad Request를 반환한다")
    @ParameterizedTest(name = "내 프로젝트 초대 목록 조회 실패 쿼리: {0}")
    @ValueSource(strings = {
      "?page=-1&size=10",
      "?page=0&size=0",
      "?page=0&size=101"
    })
    void listMyInvitations_InvalidPagination_BadRequest(
        String query) {
      assertInvalidPagination(
          webTestClient,
          API_BASE + "/users/me/invitations/projects" + query,
          outsiderToken);
    }

  }

  @Nested
  @DisplayName("프로젝트 초대 수락")
  class AcceptInvitationTests {

    @Test
    @DisplayName("워크스페이스 멤버가 프로젝트 초대를 수락하면 200 OK를 반환하고 프로젝트 멤버가 생성된다")
    void acceptInvitation_Success() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("project-invitation-accept",
              ProjectInvitationApiSnippets.acceptInvitationRequestHeaders(),
              ProjectInvitationApiSnippets.acceptInvitationPathParameters(),
              ProjectInvitationApiSnippets.acceptInvitationResponseHeaders(),
              ProjectInvitationApiSnippets.acceptInvitationResponse()))
          .jsonPath("$.userId").isEqualTo(workspaceMemberId)
          .jsonPath("$.projectId").isEqualTo(testProject.getId())
          .jsonPath("$.role").isEqualTo(ProjectRole.EDITOR.name());

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      boolean memberExists = projectMemberRepository
          .existsByProjectIdAndUserIdAndNotDeleted(testProject.getId(), workspaceMemberId)
          .block();
      assertThat(memberExists).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 초대를 수락하면 400 Bad Request를 반환한다")
    void acceptInvitation_WrongUser_Forbidden() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 수락하면 404 Not Found를 반환한다")
    void acceptInvitation_NotFound() {
      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              "nonexistent-id")
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyAccepted_Conflict() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 프로젝트 멤버인 경우 초대 수락에 실패하고 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyProjectMember_Conflict() {
      User workspaceMember = getUser(workspaceMemberId);

      addProjectMember(testProject.getId(), workspaceMemberId,
          ProjectRole.VIEWER);

      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
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
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNoContent()
          .expectBody()
          .consumeWith(document("project-invitation-reject",
              ProjectInvitationApiSnippets.rejectInvitationRequestHeaders(),
              ProjectInvitationApiSnippets.rejectInvitationPathParameters()));

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    @DisplayName("다른 사용자가 초대를 거절하면 400 Bad Request를 반환한다")
    void rejectInvitation_WrongUser_Forbidden() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 거절하면 404 Not Found를 반환한다")
    void rejectInvitation_NotFound() {
      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              "nonexistent-id")
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyRejected_Conflict() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyAccepted_Conflict() {
      User workspaceMember = getUser(workspaceMemberId);
      Invitation invitation = saveProjectInvitation(testProject.getId(),
          testWorkspace.getId(), workspaceMember.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.patch()
          .uri(API_BASE + "/projects/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + workspaceMemberToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

}
