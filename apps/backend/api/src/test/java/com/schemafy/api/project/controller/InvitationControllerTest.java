package com.schemafy.api.project.controller;

import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.project.docs.MyInvitationApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.ulid.exception.UlidErrorCode;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("InvitationController 통합 테스트")
class InvitationControllerTest extends ProjectHttpTestSupport {

  private static final String API_BASE = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  private String adminUserId;
  private String inviteeUserId;
  private String adminToken;
  private String inviteeToken;
  private Workspace testWorkspace;
  private Project testProject;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    User admin = createUser("admin@test.com", "Admin");
    User invitee = createUser("invitee@test.com", "Invitee");

    adminUserId = admin.id();
    inviteeUserId = invitee.id();
    adminToken = generateAccessToken(adminUserId);
    inviteeToken = generateAccessToken(inviteeUserId);

    testWorkspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(testWorkspace.getId(), adminUserId, WorkspaceRole.ADMIN);
    testProject = saveProject(testWorkspace.getId(), "Test Project", "Description");
    addProjectMember(testProject.getId(), adminUserId, ProjectRole.ADMIN);
  }

  @Nested
  @DisplayName("통합 내 초대 목록 조회")
  class GetMyInvitationsTests {

    @Test
    @DisplayName("워크스페이스/프로젝트 초대를 통합 조회한다")
    void getMyInvitations_Success() {
      User invitee = getUser(inviteeUserId);
      Invitation workspaceInvitation = saveWorkspaceInvitation(
          testWorkspace.getId(), invitee.email(), WorkspaceRole.MEMBER,
          adminUserId);
      Invitation projectInvitation = saveProjectInvitation(
          testProject.getId(), testWorkspace.getId(), invitee.email(),
          ProjectRole.EDITOR, adminUserId);

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=10")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .consumeWith(document("my-invitation-list",
              MyInvitationApiSnippets.listMyInvitationsRequestHeaders(),
              MyInvitationApiSnippets.listMyInvitationsQueryParameters(),
              MyInvitationApiSnippets.listMyInvitationsResponseHeaders(),
              MyInvitationApiSnippets.listMyInvitationsResponse()))
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.content[0].id").isEqualTo(projectInvitation.getId())
          .jsonPath("$.content[0].type").isEqualTo("PROJECT")
          .jsonPath("$.content[0].targetId").isEqualTo(testProject.getId())
          .jsonPath("$.content[1].id").isEqualTo(workspaceInvitation.getId())
          .jsonPath("$.content[1].type").isEqualTo("WORKSPACE")
          .jsonPath("$.content[1].targetId").isEqualTo(testWorkspace.getId())
          .jsonPath("$.size").isEqualTo(10)
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("size를 생략하면 기본값 5를 사용한다")
    void getMyInvitations_UsesDefaultSize() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[6];

      for (int i = 0; i < invitations.length; i++) {
        Workspace workspace = saveWorkspace("Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(5)
          .jsonPath("$.size").isEqualTo(5)
          .jsonPath("$.hasNext").isEqualTo(true)
          .jsonPath("$.nextCursorId").isEqualTo(invitations[1].getId());
    }

    @Test
    @DisplayName("결과 수가 size와 같으면 hasNext는 false다")
    void getMyInvitations_ExactSizeHasNoNextPage() {
      User invitee = getUser(inviteeUserId);

      for (int i = 0; i < 5; i++) {
        Workspace workspace = saveWorkspace("Exact Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=5")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(5)
          .jsonPath("$.size").isEqualTo(5)
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("최대 허용 size 100을 사용해 조회할 수 있다")
    void getMyInvitations_AcceptsMaxSize() {
      User invitee = getUser(inviteeUserId);
      Invitation invitation = saveWorkspaceInvitation(
          testWorkspace.getId(), invitee.email(), WorkspaceRole.MEMBER,
          adminUserId);

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=100")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(1)
          .jsonPath("$.content[0].id").isEqualTo(invitation.getId())
          .jsonPath("$.size").isEqualTo(100)
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("cursorId로 다음 페이지를 조회한다")
    void getMyInvitations_NextPage() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[6];

      for (int i = 0; i < invitations.length; i++) {
        Workspace workspace = saveWorkspace("Paged Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      byte[] firstPageResponseBody = webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=3")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content[0].id").isEqualTo(invitations[5].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[4].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[3].getId())
          .jsonPath("$.hasNext").isEqualTo(true)
          .jsonPath("$.nextCursorId").isEqualTo(invitations[3].getId())
          .returnResult()
          .getResponseBodyContent();

      assertThat(firstPageResponseBody).isNotNull();
      String nextCursorId = JsonPath.read(new String(firstPageResponseBody),
          "$.nextCursorId");

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=3&cursorId="
              + nextCursorId)
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(3)
          .jsonPath("$.content[0].id").isEqualTo(invitations[2].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[1].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[0].getId())
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("소문자 cursorId도 같은 다음 페이지 경계로 처리한다")
    void getMyInvitations_LowercaseCursorId_UsesSameBoundary() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[6];

      for (int i = 0; i < invitations.length; i++) {
        Workspace workspace = saveWorkspace("Lowercase Cursor Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      String lowercaseCursorId = invitations[3].getId().toLowerCase(Locale.ROOT);

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=3&cursorId="
              + lowercaseCursorId)
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(3)
          .jsonPath("$.content[0].id").isEqualTo(invitations[2].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[1].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[0].getId())
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("공백이 섞인 유효한 cursorId도 trim 후 같은 다음 페이지 경계로 처리한다")
    void getMyInvitations_TrimmedCursorId_UsesSameBoundary() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[6];

      for (int i = 0; i < invitations.length; i++) {
        Workspace workspace = saveWorkspace("Trimmed Cursor Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      String paddedCursorId = "  " + invitations[3].getId() + "  ";

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations")
              .queryParam("size", 3)
              .queryParam("cursorId", paddedCursorId)
              .build())
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(3)
          .jsonPath("$.content[0].id").isEqualTo(invitations[2].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[1].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[0].getId())
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("빈 cursorId는 첫 페이지 요청으로 처리한다")
    void getMyInvitations_BlankCursorId_TreatedAsFirstPage() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[3];

      for (int i = 0; i < invitations.length; i++) {
        Workspace workspace = saveWorkspace("Blank Cursor Workspace " + i,
            "Description " + i);
        addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(workspace.getId(),
            invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      }

      webTestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(API_BASE + "/users/me/invitations")
              .queryParam("size", 3)
              .queryParam("cursorId", "   ")
              .build())
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(3)
          .jsonPath("$.content[0].id").isEqualTo(invitations[2].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[1].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[0].getId())
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("pending가 아니거나 만료된 초대는 제외한다")
    void getMyInvitations_OnlyPendingAndNotExpired() {
      User invitee = getUser(inviteeUserId);
      Invitation pendingInvitation = saveWorkspaceInvitation(
          testWorkspace.getId(), invitee.email(), WorkspaceRole.MEMBER,
          adminUserId);
      acceptInvitation(saveProjectInvitation(
          testProject.getId(), testWorkspace.getId(), invitee.email(),
          ProjectRole.VIEWER, adminUserId));

      expireInvitation(saveWorkspaceInvitation(
          saveWorkspace("Expired Workspace", "Description").getId(),
          invitee.email(), WorkspaceRole.MEMBER, adminUserId),
          Instant.now().minusSeconds(60));

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=10")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(1)
          .jsonPath("$.content[0].id").isEqualTo(pendingInvitation.getId());
    }

    @Test
    @DisplayName("워크스페이스와 프로젝트 초대가 섞여 있어도 다음 페이지를 이어서 조회한다")
    void getMyInvitations_MixedInvitations_NextPage() {
      User invitee = getUser(inviteeUserId);
      Invitation[] invitations = new Invitation[6];

      for (int i = 0; i < invitations.length; i++) {
        if (i % 2 == 0) {
          Workspace workspace = saveWorkspace("Mixed Workspace " + i,
              "Description " + i);
          addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
          invitations[i] = saveWorkspaceInvitation(workspace.getId(),
              invitee.email(), WorkspaceRole.MEMBER, adminUserId);
        } else {
          Workspace workspace = saveWorkspace("Mixed Project Workspace " + i,
              "Description " + i);
          addWorkspaceMember(workspace.getId(), adminUserId, WorkspaceRole.ADMIN);
          Project project = saveProject(workspace.getId(), "Mixed Project " + i,
              "Description " + i);
          addProjectMember(project.getId(), adminUserId, ProjectRole.ADMIN);
          invitations[i] = saveProjectInvitation(project.getId(),
              workspace.getId(), invitee.email(), ProjectRole.EDITOR, adminUserId);
        }
      }

      byte[] firstPageResponseBody = webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=4")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(4)
          .jsonPath("$.content[0].id").isEqualTo(invitations[5].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[4].getId())
          .jsonPath("$.content[2].id").isEqualTo(invitations[3].getId())
          .jsonPath("$.content[3].id").isEqualTo(invitations[2].getId())
          .jsonPath("$.hasNext").isEqualTo(true)
          .jsonPath("$.nextCursorId").isEqualTo(invitations[2].getId())
          .returnResult()
          .getResponseBodyContent();

      assertThat(firstPageResponseBody).isNotNull();
      String nextCursorId = JsonPath.read(new String(firstPageResponseBody),
          "$.nextCursorId");

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=4&cursorId="
              + nextCursorId)
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(2)
          .jsonPath("$.content[0].id").isEqualTo(invitations[1].getId())
          .jsonPath("$.content[1].id").isEqualTo(invitations[0].getId())
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "?size=0",
      "?size=101"
    })
    @DisplayName("잘못된 size면 400 Bad Request를 반환한다")
    void getMyInvitations_InvalidSize_BadRequest(String query) {
      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations" + query)
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isBadRequest()
          .expectBody()
          .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code())
          .jsonPath("$.detail").value(detail -> assertThat((String) detail)
              .isNotBlank());
    }

    @Test
    @DisplayName("blank가 아닌 잘못된 cursorId면 400 Bad Request를 반환한다")
    void getMyInvitations_InvalidCursorId_BadRequest() {
      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?cursorId=invalid")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isBadRequest()
          .expectBody()
          .jsonPath("$.reason").isEqualTo(UlidErrorCode.INVALID_VALUE.code())
          .jsonPath("$.detail").value(detail -> assertThat((String) detail)
              .isNotBlank());
    }

    @Test
    @DisplayName("너무 긴 cursorId면 400 Bad Request를 반환한다")
    void getMyInvitations_TooLongCursorId_BadRequest() {
      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?cursorId="
              + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABC")
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isBadRequest()
          .expectBody()
          .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code())
          .jsonPath("$.detail").value(detail -> assertThat((String) detail)
              .isNotBlank());
    }

    @Test
    @DisplayName("존재하지 않는 ULID cursorId는 단순 경계값으로 처리한다")
    void getMyInvitations_NonExistingCursorHandledAsBoundary() {
      User invitee = getUser(inviteeUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invitee.email(), WorkspaceRole.MEMBER, adminUserId);
      String cursorId = UlidGenerator.generate();

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=10&cursorId=" + cursorId)
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(1)
          .jsonPath("$.content[0].id").isEqualTo(invitation.getId());
    }

    @Test
    @DisplayName("마지막 페이지 이후에는 빈 목록을 반환한다")
    void getMyInvitations_ReturnsEmptyAfterLastPage() {
      User invitee = getUser(inviteeUserId);
      Invitation invitation = saveWorkspaceInvitation(testWorkspace.getId(),
          invitee.email(), WorkspaceRole.MEMBER, adminUserId);

      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations?size=5&cursorId="
              + invitation.getId())
          .header("Authorization", "Bearer " + inviteeToken)
          .exchange()
          .expectStatus().isOk()
          .expectBody()
          .jsonPath("$.content.length()").isEqualTo(0)
          .jsonPath("$.size").isEqualTo(5)
          .jsonPath("$.hasNext").isEqualTo(false)
          .jsonPath("$.nextCursorId").value(value -> assertThat(value).isNull());
    }

    @Test
    @DisplayName("인증 토큰 없이 요청하면 401 Unauthorized를 반환한다")
    void getMyInvitations_NoAuth_Unauthorized() {
      webTestClient.get()
          .uri(API_BASE + "/users/me/invitations")
          .exchange()
          .expectStatus().isUnauthorized();
    }

  }
}
