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
import com.schemafy.api.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.api.project.docs.WorkspaceInvitationApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("WorkspaceInvitationController 통합 테스트")
class WorkspaceInvitationControllerTest extends ProjectHttpTestSupport {

  private static final String API_BASE = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  private String adminUserId;
  private String memberUserId;
  private String invitedUserId;
  private String adminToken;
  private String memberToken;
  private String invitedToken;
  private Workspace testWorkspace;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    User admin = createUser("admin@test.com", "Admin");
    User member = createUser("member@test.com", "Member");
    User invited = createUser("invited@test.com", "Invited");

    adminUserId = admin.id();
    memberUserId = member.id();
    invitedUserId = invited.id();

    adminToken = generateAccessToken(adminUserId);
    memberToken = generateAccessToken(memberUserId);
    invitedToken = generateAccessToken(invitedUserId);

    testWorkspace = saveWorkspace("Test Workspace", "Test Description");
    addWorkspaceMember(testWorkspace.getId(), adminUserId,
        WorkspaceRole.ADMIN);
    addWorkspaceMember(testWorkspace.getId(), memberUserId,
        WorkspaceRole.MEMBER);
  }

  private CreateWorkspaceInvitationRequest createWorkspaceInvitationRequest(
      String email,
      String role) {
    return new CreateWorkspaceInvitationRequest(email,
        resolveRecordEnum(CreateWorkspaceInvitationRequest.class, 1, role));
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
  @DisplayName("초대 생성")
  class CreateInvitationTests {

    @Test
    @DisplayName("관리자가 새로운 사용자를 초대하면 201 Created를 반환한다")
    void createInvitation_Success() {
      CreateWorkspaceInvitationRequest request = createWorkspaceInvitationRequest("invited@test.com",
          "MEMBER");

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated()
          .expectBody()
          .consumeWith(document("workspace-invitation-create",
              WorkspaceInvitationApiSnippets.createInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.createInvitationPathParameters(),
              WorkspaceInvitationApiSnippets.createInvitationRequest(),
              WorkspaceInvitationApiSnippets.createInvitationResponseHeaders(),
              WorkspaceInvitationApiSnippets.createInvitationResponse()))
          .jsonPath("$.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.invitedEmail").isEqualTo("invited@test.com")
          .jsonPath("$.invitedRole").isEqualTo(WorkspaceRole.MEMBER.name())
          .jsonPath("$.status").isEqualTo(InvitationStatus.PENDING.name());
    }

    @Test
    @DisplayName("일반 멤버가 초대하면 403 Forbidden을 반환한다")
    void createInvitation_NotAdmin_Forbidden() {
      CreateWorkspaceInvitationRequest request = createWorkspaceInvitationRequest("invited@test.com",
          "MEMBER");

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + memberToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식이면 400 Bad Request를 반환한다")
    void createInvitation_InvalidEmail_BadRequest() {
      CreateWorkspaceInvitationRequest request = createWorkspaceInvitationRequest("invalid-email",
          "MEMBER");

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("인증 토큰 없이 요청하면 401 Unauthorized를 반환한다")
    void createInvitation_NoAuth_Unauthorized() {
      CreateWorkspaceInvitationRequest request = createWorkspaceInvitationRequest("invited@test.com",
          "MEMBER");

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("중복 초대 시 409 Conflict를 반환한다")
    void createInvitation_Duplicate_Conflict() {
      CreateWorkspaceInvitationRequest request = createWorkspaceInvitationRequest("invited@test.com",
          "MEMBER");

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated();

      webTestClient.post()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations", testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("초대 목록 조회")
  class ListInvitationsTests {

    @Test
    @DisplayName("관리자가 초대 목록을 조회하면 200 OK를 반환한다")
    void listInvitations_Success() {
      saveWorkspaceInvitation(testWorkspace.getId(), "user1@test.com",
          WorkspaceRole.MEMBER, adminUserId);
      saveWorkspaceInvitation(testWorkspace.getId(), "user2@test.com",
          WorkspaceRole.MEMBER, adminUserId);

      webTestClient.get()
          .uri(API_BASE + "/workspaces/{workspaceId}/invitations?page=0&size=10",
              testWorkspace.getId())
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-list",
              WorkspaceInvitationApiSnippets.listInvitationsRequestHeaders(),
              WorkspaceInvitationApiSnippets.listInvitationsPathParameters(),
              WorkspaceInvitationApiSnippets.listInvitationsQueryParameters(),
              WorkspaceInvitationApiSnippets.listInvitationsResponseHeaders(),
              WorkspaceInvitationApiSnippets.listInvitationsResponse()))
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.content[0].type").isEqualTo("WORKSPACE")
          .jsonPath("$.content[0].targetId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.content[0].targetName").isEqualTo(testWorkspace.getName())
          .jsonPath("$.content[0].invitedBy").isEqualTo("Admin")
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.page").isEqualTo(0)
          .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    @DisplayName("일반 멤버가 목록 조회하면 403 Forbidden을 반환한다")
    void listInvitations_NotAdmin_Forbidden() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build(testWorkspace.getId()))
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listInvitations_Pagination() {
      for (int i = 0; i < 5; i++) {
        saveWorkspaceInvitation(testWorkspace.getId(),
            "user" + i + "@test.com", WorkspaceRole.MEMBER, adminUserId);
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/workspaces/{workspaceId}/invitations")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build(testWorkspace.getId()))
          .header("Authorization", "Bearer " + adminToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(5);
    }

    @DisplayName("초대 목록 조회는 잘못된 pagination 쿼리에 대해 400 Bad Request를 반환한다")
    @ParameterizedTest(name = "초대 목록 조회 실패 쿼리: {0}")
    @ValueSource(strings = {
      "?page=-1&size=10",
      "?page=0&size=0",
      "?page=0&size=101"
    })
    void listInvitations_InvalidPagination_BadRequest(
        String query) {
      assertInvalidPagination(
          webTestClient,
          API_BASE + "/workspaces/" + testWorkspace.getId()
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
      User invited = getUser(invitedUserId);

      // 여러 워크스페이스에서 초대 생성
      Workspace workspace2 = saveWorkspace("Workspace 2", "Description 2");

      saveWorkspaceInvitation(testWorkspace.getId(), invited.email(),
          WorkspaceRole.MEMBER, adminUserId);
      saveWorkspaceInvitation(workspace2.getId(), invited.email(),
          WorkspaceRole.ADMIN, adminUserId);

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-list-my",
              WorkspaceInvitationApiSnippets.listMyInvitationsRequestHeaders(),
              WorkspaceInvitationApiSnippets.listMyInvitationsQueryParameters(),
              WorkspaceInvitationApiSnippets.listMyInvitationsResponseHeaders(),
              WorkspaceInvitationApiSnippets.listMyInvitationsResponse()))
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.content[0].type").isEqualTo("WORKSPACE")
          .jsonPath("$.content[0].targetName").isEqualTo(workspace2.getName())
          .jsonPath("$.content[0].invitedBy").isEqualTo("Admin")
          .jsonPath("$.content[0].invitedEmail").isEqualTo(invited.email())
          .jsonPath("$.content[1].type").isEqualTo("WORKSPACE")
          .jsonPath("$.content[1].targetName").isEqualTo(testWorkspace.getName())
          .jsonPath("$.content[1].invitedBy").isEqualTo("Admin")
          .jsonPath("$.content[1].invitedEmail").isEqualTo(invited.email());
    }

    @Test
    @DisplayName("초대가 없으면 빈 목록을 반환한다")
    void listMyInvitations_Empty() {
      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(0)
          .jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션 파라미터가 올바르게 동작한다")
    void listMyInvitations_Pagination() {
      User invited = getUser(invitedUserId);

      for (int i = 0; i < 5; i++) {
        Workspace workspace = saveWorkspace("Workspace " + i,
            "Description " + i);
        saveWorkspaceInvitation(workspace.getId(), invited.email(),
            WorkspaceRole.MEMBER, adminUserId);
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 2)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.totalElements").isEqualTo(5);
    }

    @Test
    @DisplayName("PENDING 상태의 초대만 조회된다")
    void listMyInvitations_OnlyPending() {
      User invited = getUser(invitedUserId);

      // PENDING 초대
      Invitation pending = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      // ACCEPTED 초대
      Workspace workspace2 = saveWorkspace("Workspace 2", "Description 2");
      Invitation accepted = saveWorkspaceInvitation(workspace2.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);
      accepted.accept();
      invitationRepository.save(accepted).block();

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .header("Authorization", "Bearer " + invitedToken)
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
              .path(API_BASE + "/users/me/invitations/workspaces")
              .queryParam("page", 0)
              .queryParam("size", 10)
              .build())
          .exchange()
          .expectStatus().isUnauthorized();
    }

    @DisplayName("내 초대 목록 조회는 잘못된 pagination 쿼리에 대해 400 Bad Request를 반환한다")
    @ParameterizedTest(name = "내 초대 목록 조회 실패 쿼리: {0}")
    @ValueSource(strings = {
      "?page=-1&size=10",
      "?page=0&size=0",
      "?page=0&size=101"
    })
    void listMyInvitations_InvalidPagination_BadRequest(
        String query) {
      assertInvalidPagination(
          webTestClient,
          API_BASE + "/users/me/invitations/workspaces" + query,
          invitedToken);
    }

  }

  @Nested
  @DisplayName("초대 수락")
  class AcceptInvitationTests {

    @Test
    @DisplayName("초대받은 사용자가 초대를 수락하면 200 OK를 반환하고 멤버가 생성된다")
    void acceptInvitation_Success() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("workspace-invitation-accept",
              WorkspaceInvitationApiSnippets.acceptInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.acceptInvitationPathParameters(),
              WorkspaceInvitationApiSnippets.acceptInvitationResponseHeaders(),
              WorkspaceInvitationApiSnippets.acceptInvitationResponse()))
          .jsonPath("$.userId").isEqualTo(invitedUserId)
          .jsonPath("$.workspaceId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.role").isEqualTo(WorkspaceRole.MEMBER.name());

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);

      boolean memberExists = workspaceMemberRepository
          .existsByWorkspaceIdAndUserIdAndNotDeleted(testWorkspace.getId(), invitedUserId)
          .block();
      assertThat(memberExists).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 초대를 수락하면 400 Bad Request를 반환한다")
    void acceptInvitation_WrongUser_Forbidden() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 수락하면 404 Not Found를 반환한다")
    void acceptInvitation_NotFound() {
      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              "nonexistent-id")
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 수락된 초대를 다시 수락하면 409 Conflict를 반환한다")
    void acceptInvitation_AlreadyAccepted_Conflict() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

  @Nested
  @DisplayName("초대 거절")
  class RejectInvitationTests {

    @Test
    @DisplayName("초대받은 사용자가 초대를 거절하면 204 No Content를 반환한다")
    void rejectInvitation_Success() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNoContent()
          .expectBody()
          .consumeWith(document("workspace-invitation-reject",
              WorkspaceInvitationApiSnippets.rejectInvitationRequestHeaders(),
              WorkspaceInvitationApiSnippets.rejectInvitationPathParameters()));

      Invitation updated = invitationRepository.findById(invitation.getId()).block();
      assertThat(updated.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    @DisplayName("다른 사용자가 초대를 거절하면 400 Bad Request를 반환한다")
    void rejectInvitation_WrongUser_Forbidden() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + memberToken)
          .exchange()
          .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("존재하지 않는 초대를 거절하면 404 Not Found를 반환한다")
    void rejectInvitation_NotFound() {
      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              "nonexistent-id")
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 거절된 초대를 다시 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyRejected_Conflict() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("이미 수락된 초대를 거절하면 409 Conflict를 반환한다")
    void rejectInvitation_AlreadyAccepted_Conflict() {
      User invited = getUser(invitedUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invited.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/accept",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isOk();

      webTestClient.patch()
          .uri(API_BASE + "/workspaces/invitations/{invitationId}/reject",
              invitation.getId())
          .header("Authorization", "Bearer " + invitedToken)
          .exchange()
          .expectStatus().isEqualTo(409);
    }

  }

}
