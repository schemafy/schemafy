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
import org.junit.jupiter.api.Test;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.api.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.api.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.api.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.api.project.docs.WorkspaceApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("WorkspaceController 통합 테스트")
class WorkspaceControllerTest extends ProjectHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0") + "/workspaces";

  @Autowired
  private WebTestClient webTestClient;

  private String testUserId;
  private String testUser2Id;
  private String accessToken;
  private String accessToken2;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    User testUser = createUser("test@example.com", "Test User");
    User testUser2 = createUser("test2@example.com", "Test User 2");

    testUserId = testUser.id();
    testUser2Id = testUser2.id();

    accessToken = generateAccessToken(testUserId);
    accessToken2 = generateAccessToken(testUser2Id);
  }

  @Test
  @DisplayName("워크스페이스 생성에 성공한다")
  void createWorkspaceSuccess() {
    CreateWorkspaceRequest request = new CreateWorkspaceRequest(
        "My Workspace", "Test Description");

    webTestClient.post().uri(API_BASE_PATH)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .consumeWith(document("workspace-create",
            WorkspaceApiSnippets.createWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.createWorkspaceRequest(),
            WorkspaceApiSnippets.createWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.createWorkspaceResponse()))
        .jsonPath("$.name").isEqualTo("My Workspace");

    findActiveWorkspaceMembersByUserId(testUserId)
        .collectList().block().forEach(member -> {
          assertThat(member.getRole())
              .isEqualTo(WorkspaceRole.ADMIN.name());
        });
  }

  @Test
  @DisplayName("워크스페이스 생성 시 이름이 없으면 실패한다")
  void createWorkspaceFailWithoutName() {
    CreateWorkspaceRequest request = new CreateWorkspaceRequest("", null);

    webTestClient.post().uri(API_BASE_PATH)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isBadRequest();
  }

  @Test
  @DisplayName("워크스페이스 목록 조회에 성공한다")
  void getWorkspacesSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    webTestClient.get().uri(API_BASE_PATH + "?page=0&size=10")
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-list",
            WorkspaceApiSnippets.getWorkspacesRequestHeaders(),
            WorkspaceApiSnippets.getWorkspacesQueryParameters(),
            WorkspaceApiSnippets.getWorkspacesResponseHeaders(),
            WorkspaceApiSnippets.getWorkspacesResponse()))
        .jsonPath("$.content[0].name")
        .isEqualTo("Test Workspace").jsonPath("$.totalElements")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 상세 조회에 성공한다")
  void getWorkspaceSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-get",
            WorkspaceApiSnippets.getWorkspacePathParameters(),
            WorkspaceApiSnippets.getWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.getWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.getWorkspaceResponse()))
        .jsonPath("$.id").isEqualTo(workspace.getId())
        .jsonPath("$.name").isEqualTo("Test Workspace");
  }

  @Test
  @DisplayName("멤버가 아닌 사용자는 워크스페이스 조회에 실패한다")
  void getWorkspaceFailWhenNotMember() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("워크스페이스 수정에 성공한다")
  void updateWorkspaceSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
        "Updated Workspace", "Updated Description");

    webTestClient.put()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-update",
            WorkspaceApiSnippets.updateWorkspacePathParameters(),
            WorkspaceApiSnippets.updateWorkspaceRequestHeaders(),
            WorkspaceApiSnippets.updateWorkspaceRequest(),
            WorkspaceApiSnippets.updateWorkspaceResponseHeaders(),
            WorkspaceApiSnippets.updateWorkspaceResponse()))
        .jsonPath("$.name").isEqualTo("Updated Workspace");
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 워크스페이스 수정에 실패한다")
  void updateWorkspaceFailWhenNotAdmin() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.MEMBER);

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
        "Updated Workspace", "Updated Description");

    webTestClient.put().uri(API_BASE_PATH + "/" + workspace.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("워크스페이스 삭제에 성공한다")
  void deleteWorkspaceSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}", workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("workspace-delete",
            WorkspaceApiSnippets.deleteWorkspacePathParameters(),
            WorkspaceApiSnippets.deleteWorkspaceRequestHeaders()));

    Workspace deletedWorkspace = workspaceRepository.findById(
        workspace.getId()).block();
    assertThat(deletedWorkspace.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 워크스페이스 삭제에 실패한다")
  void deleteWorkspaceFailWhenNotAdmin() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.MEMBER);

    webTestClient.delete().uri(API_BASE_PATH + "/" + workspace.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("멤버 목록 조회에 성공한다")
  void getMembersSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{id}/members?page=0&size=10",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-members",
            WorkspaceApiSnippets
                .getWorkspaceMembersPathParameters(),
            WorkspaceApiSnippets
                .getWorkspaceMembersRequestHeaders(),
            WorkspaceApiSnippets
                .getWorkspaceMembersQueryParameters(),
            WorkspaceApiSnippets
                .getWorkspaceMembersResponseHeaders(),
            WorkspaceApiSnippets.getWorkspaceMembersResponse()))
        .jsonPath("$.totalElements")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("MEMBER도 멤버 목록을 조회할 수 있다")
  void getMembersSuccessWithMemberRole() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.MEMBER);

    webTestClient.get().uri(API_BASE_PATH + "/" + workspace.getId()
        + "/members?page=0&size=20")
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isOk();
  }

  @Test
  @DisplayName("멤버 추가에 성공한다")
  void addMemberSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);

    User user = getUser(testUser2Id);
    AddWorkspaceMemberRequest request = addWorkspaceMemberRequest(
        user.email(),
        "MEMBER");

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .consumeWith(document("workspace-member-add",
            WorkspaceApiSnippets.addMemberPathParameters(),
            WorkspaceApiSnippets.addMemberRequestHeaders(),
            WorkspaceApiSnippets.addMemberRequest(),
            WorkspaceApiSnippets.addMemberResponseHeaders(),
            WorkspaceApiSnippets.addMemberResponse()))
        .jsonPath("$.userId").isEqualTo(testUser2Id)
        .jsonPath("$.role")
        .isEqualTo(WorkspaceRole.MEMBER.name());
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 멤버 추가에 실패한다")
  void addMemberFailWhenNotAdmin() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.MEMBER);

    User testUser2 = getUser(testUser2Id);
    AddWorkspaceMemberRequest request = addWorkspaceMemberRequest(
        testUser2.email(),
        "MEMBER");

    webTestClient.post()
        .uri(API_BASE_PATH + "/" + workspace.getId() + "/members")
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("ADMIN은 멤버 추방을 할 수 있다")
  void removeMemberSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    var member2 = addWorkspaceMember(workspace.getId(), testUser2Id,
        WorkspaceRole.MEMBER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{userId}",
            workspace.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("workspace-member-remove",
            WorkspaceApiSnippets.removeMemberPathParameters(),
            WorkspaceApiSnippets.removeMemberRequestHeaders()));

    assertThat(workspaceMemberRepository.findById(member2.getId()).block()
        .isDeleted()).isTrue();
  }

  @Test
  @DisplayName("ADMIN은 다른 ADMIN을 추방할 수 있다")
  void removeMember_AdminRemovesAnotherAdmin_Success() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    var admin2 = addWorkspaceMember(workspace.getId(), testUser2Id,
        WorkspaceRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{userId}",
            workspace.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent();

    WorkspaceMember deleted = workspaceMemberRepository.findById(admin2.getId()).block();
    assertThat(deleted).isNotNull();
    assertThat(deleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 멤버 추방에 실패한다")
  void removeMemberFailWhenNotAdmin() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.MEMBER);

    webTestClient.delete()
        .uri(API_BASE_PATH + "/" + workspace.getId() + "/members/"
            + testUser2Id)
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("워크스페이스 탈퇴에 성공한다")
  void leaveMemberSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    var member2 = addWorkspaceMember(workspace.getId(), testUser2Id,
        WorkspaceRole.MEMBER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/me",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("workspace-member-leave",
            WorkspaceApiSnippets.leaveMemberPathParameters(),
            WorkspaceApiSnippets.leaveMemberRequestHeaders()));

    assertThat(workspaceMemberRepository.findById(member2.getId()).block()
        .isDeleted()).isTrue();
  }

  @Test
  @DisplayName("워크스페이스 멤버가 1명이고 워크스페이스 비멤버 프로젝트 멤버가 있어도 탈퇴 시 워크스페이스가 삭제된다")
  void leaveMember_LastWorkspaceMember_WithProjectOnlyMember_DeletesWorkspace() {
    Workspace workspace = saveWorkspace("Single Admin Workspace",
        "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace.getId(), "Project", "Desc");
    addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/me",
            workspace.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isNoContent();

    Workspace deletedWorkspace = workspaceRepository
        .findByIdAndNotDeleted(workspace.getId()).block();
    assertThat(deletedWorkspace).isNull();

    Project deletedProject = projectRepository
        .findByIdAndNotDeleted(project.getId()).block();
    assertThat(deletedProject).isNull();

    ProjectMember activeProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), testUser2Id).block();
    assertThat(activeProjectMember).isNull();
  }

  @Test
  @DisplayName("멤버 역할 변경에 성공한다")
  void updateMemberRoleSuccess() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.MEMBER);

    UpdateMemberRoleRequest request = updateMemberRoleRequest("ADMIN");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{userId}/role",
            workspace.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .consumeWith(document("workspace-member-update-role",
            WorkspaceApiSnippets.updateMemberRolePathParameters(),
            WorkspaceApiSnippets.updateMemberRoleRequestHeaders(),
            WorkspaceApiSnippets.updateMemberRoleRequest(),
            WorkspaceApiSnippets.updateMemberRoleResponseHeaders(),
            WorkspaceApiSnippets.updateMemberRoleResponse()))
        .jsonPath("$.role")
        .isEqualTo(WorkspaceRole.ADMIN.name());
  }

  @Test
  @DisplayName("프로젝트 관리자여도 워크스페이스에서는 관리자 등급을 내릴 수 있다")
  void updateMemberRole_ProjectAdminTarget_Success() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspace.getId(), testUser2Id, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace.getId(), "Project", "Description");
    addProjectMember(project.getId(), testUser2Id, ProjectRole.ADMIN);

    UpdateMemberRoleRequest request = updateMemberRoleRequest("MEMBER");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{userId}/role",
            workspace.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .jsonPath("$.role")
        .isEqualTo(WorkspaceRole.MEMBER.name());
  }

  @Test
  @DisplayName("프로젝트 관리자여도 워크스페이스에서는 멤버를 추방할 수 있다")
  void removeMember_ProjectAdminTarget_Success() {
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUserId, WorkspaceRole.ADMIN);
    WorkspaceMember target = addWorkspaceMember(workspace.getId(), testUser2Id,
        WorkspaceRole.ADMIN);
    Project project = saveProject(workspace.getId(), "Project", "Description");
    addProjectMember(project.getId(), testUser2Id, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/members/{userId}",
            workspace.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken)
        .exchange().expectStatus().isNoContent();

    WorkspaceMember deleted = workspaceMemberRepository.findById(target.getId()).block();
    assertThat(deleted).isNotNull();
    assertThat(deleted.isDeleted()).isTrue();
  }

  private AddWorkspaceMemberRequest addWorkspaceMemberRequest(
      String email,
      String role) {
    return new AddWorkspaceMemberRequest(email,
        resolveRecordEnum(AddWorkspaceMemberRequest.class, 1, role));
  }

  private UpdateMemberRoleRequest updateMemberRoleRequest(String role) {
    return new UpdateMemberRoleRequest(
        resolveRecordEnum(UpdateMemberRoleRequest.class, 0, role));
  }

  @SuppressWarnings("unchecked")
  private <E extends Enum<E>> E resolveRecordEnum(
      Class<?> recordType,
      int componentIndex,
      String value) {
    return Enum.valueOf((Class<E>) recordType.getRecordComponents()[componentIndex]
        .getType(), value);
  }

}
