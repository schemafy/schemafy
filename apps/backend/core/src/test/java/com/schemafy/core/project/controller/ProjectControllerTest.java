package com.schemafy.core.project.controller;

import java.util.UUID;

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

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.docs.ProjectApiSnippets;
import com.schemafy.core.project.docs.ShareLinkApiSnippets;
import com.schemafy.core.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.project.domain.ProjectRole;
import com.schemafy.domain.project.domain.ShareLink;
import com.schemafy.domain.project.domain.Workspace;
import com.schemafy.domain.project.domain.WorkspaceRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ProjectController 통합 테스트")
class ProjectControllerTest extends ProjectHttpTestSupport {

  private static final String PUBLIC_API_PREFIX = ApiPath.PUBLIC_API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  private String testUserId;
  private String testUser2Id;
  private String testWorkspaceId;
  private String accessToken;
  private String accessToken2;
  private String projectBasePath;
  private String workspaceProjectBasePath;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    User testUser = createUser("test@example.com", "Test User");
    User testUser2 = createUser("test2@example.com", "Test User 2");

    testUserId = testUser.id();
    testUser2Id = testUser2.id();

    accessToken = generateAccessToken(testUserId);
    accessToken2 = generateAccessToken(testUser2Id);

    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    testWorkspaceId = workspace.getId();

    addWorkspaceMember(testWorkspaceId, testUserId, WorkspaceRole.ADMIN);
    addWorkspaceMember(testWorkspaceId, testUser2Id, WorkspaceRole.MEMBER);

    projectBasePath = ApiPath.API.replace("{version}", "v1.0")
        + "/projects";
    workspaceProjectBasePath = ApiPath.API.replace("{version}", "v1.0")
        + "/workspaces/" + testWorkspaceId + "/projects";
  }

  private String generateShareLinkCode() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  @Test
  @DisplayName("프로젝트 생성에 성공한다")
  void createProjectSuccess() {
    CreateProjectRequest request = new CreateProjectRequest("My Project",
        "Test Description");

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects", testWorkspaceId)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .consumeWith(document("project-create",
            ProjectApiSnippets.createProjectPathParameters(),
            ProjectApiSnippets.createProjectRequestHeaders(),
            ProjectApiSnippets.createProjectRequest(),
            ProjectApiSnippets.createProjectResponseHeaders(),
            ProjectApiSnippets.createProjectResponse()))
        .jsonPath("$.name").isEqualTo("My Project")
        .jsonPath("$.workspaceId").isEqualTo(testWorkspaceId);

    projectMemberRepository.findRolesByWorkspaceIdAndUserIdWithPaging(testWorkspaceId, testUserId, 100, 0)
        .collectList().block().forEach(role -> assertThat(role).isEqualTo(ProjectRole.ADMIN.name()));
  }

  @Test
  @DisplayName("프로젝트 생성 시 이름이 없으면 실패한다")
  void createProjectFailWithoutName() {
    CreateProjectRequest request = new CreateProjectRequest("", null);

    webTestClient.post().uri(workspaceProjectBasePath)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isBadRequest();
  }

  @Test
  @DisplayName("워크스페이스 멤버가 아닌 사용자는 프로젝트를 생성할 수 없다")
  void createProjectFailWhenNotWorkspaceMember() {
    // Remove user2 from workspace
    workspaceMemberRepository.findByWorkspaceIdAndUserIdAndNotDeleted(testWorkspaceId, testUser2Id)
        .flatMap(workspaceMemberRepository::delete).block();

    CreateProjectRequest request = new CreateProjectRequest("My Project",
        "Description");

    webTestClient.post().uri(workspaceProjectBasePath)
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 목록 조회에 성공한다")
  void getProjectsSuccess() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects?page=0&size=10",
            testWorkspaceId)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("project-list",
            ProjectApiSnippets.getProjectsPathParameters(),
            ProjectApiSnippets.getProjectsRequestHeaders(),
            ProjectApiSnippets.getProjectsQueryParameters(),
            ProjectApiSnippets.getProjectsResponseHeaders(),
            ProjectApiSnippets.getProjectsResponse()))
        .jsonPath("$.content[0].name").isEqualTo("Test Project")
        .jsonPath("$.totalElements").isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 멤버가 아니면 워크스페이스 내의 프로젝트 목록 조회에 실패한다")
  void getProjectsFailWhenNotWorkspaceMember() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    workspaceMemberRepository.findByWorkspaceIdAndUserIdAndNotDeleted(testWorkspaceId, testUser2Id)
        .flatMap(workspaceMemberRepository::delete)
        .block();

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects?page=0&size=10",
            testWorkspaceId)
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 상세 조회에 성공한다")
  void getProjectSuccess() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("project-get",
            ProjectApiSnippets.getProjectPathParameters(),
            ProjectApiSnippets.getProjectRequestHeaders(),
            ProjectApiSnippets.getProjectResponseHeaders(),
            ProjectApiSnippets.getProjectResponse()))
        .jsonPath("$.id")
        .isEqualTo(project.getId()).jsonPath("$.name")
        .isEqualTo("Test Project");
  }

  @Test
  @DisplayName("멤버가 아닌 사용자는 프로젝트 조회에 실패한다")
  void getProjectFailWhenNotMember() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 수정에 성공한다")
  void updateProjectSuccess() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    UpdateProjectRequest request = new UpdateProjectRequest(
        "Updated Project", "Updated Description");

    webTestClient.put()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .consumeWith(document("project-update",
            ProjectApiSnippets.updateProjectPathParameters(),
            ProjectApiSnippets.updateProjectRequestHeaders(),
            ProjectApiSnippets.updateProjectRequest(),
            ProjectApiSnippets.updateProjectResponseHeaders(),
            ProjectApiSnippets.updateProjectResponse()))
        .jsonPath("$.name").isEqualTo("Updated Project");
  }

  @Test
  @DisplayName("Admin이 아닌 사용자는 프로젝트 수정에 실패한다")
  void updateProjectFailWhenNotAdmin() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member1 = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember member2 = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    UpdateProjectRequest request = new UpdateProjectRequest(
        "Updated Project", "Updated Description");

    webTestClient.put()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 삭제에 성공한다")
  void deleteProjectSuccess() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("project-delete",
            ProjectApiSnippets.deleteProjectPathParameters(),
            ProjectApiSnippets.deleteProjectRequestHeaders()));

    // Verify soft delete
    Project deletedProject = projectRepository.findById(project.getId())
        .block();
    assertThat(deletedProject.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("ADMIN이 아닌 사용자는 프로젝트 삭제에 실패한다")
  void deleteProjectFailWhenNotAdmin() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member1 = addProjectMember(project.getId(), testUserId, ProjectRole.EDITOR);

    ProjectMember member2 = addProjectMember(project.getId(), testUser2Id, ProjectRole.EDITOR);

    webTestClient.delete()
        .uri(projectBasePath + "/{projectId}", project.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("멤버 목록 조회에 성공한다")
  void getMembersSuccess() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(projectBasePath + "/{projectId}/members?page=0&size=20", project.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("project-members",
            ProjectApiSnippets.getProjectMembersPathParameters(),
            ProjectApiSnippets.getProjectMembersRequestHeaders(),
            ProjectApiSnippets.getProjectMembersQueryParameters(),
            ProjectApiSnippets.getProjectMembersResponseHeaders(),
            ProjectApiSnippets.getProjectMembersResponse()))
        .jsonPath("$.totalElements")
        .isEqualTo(1);
  }

  @Test
  @DisplayName("ADMIN 멤버는 다른 멤버의 역할을 변경할 수 있다")
  void updateMemberRole_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember targetMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    UpdateProjectMemberRoleRequest request = updateProjectMemberRoleRequest(
        "EDITOR");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}/role",
            project.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isOk().expectBody()
        .consumeWith(document("project-member-update-role",
            ProjectApiSnippets.updateMemberRolePathParameters(),
            ProjectApiSnippets.updateMemberRoleRequestHeaders(),
            ProjectApiSnippets.updateMemberRoleRequest(),
            ProjectApiSnippets.updateMemberRoleResponseHeaders(),
            ProjectApiSnippets.updateMemberRoleResponse()))
        .jsonPath("$..role").isEqualTo("EDITOR");

    ProjectMember updatedMember = projectMemberRepository
        .findById(targetMember.getId()).block();
    assertThat(updatedMember.getRole()).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("ADMIN 멤버는 다른 멤버의 역할을 변경할 수 있다")
  void updateMemberRole_Success2() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember targetMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    UpdateProjectMemberRoleRequest request = updateProjectMemberRoleRequest(
        "ADMIN");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}/role",
            project.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request).exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.role")
        .isEqualTo(ProjectRole.ADMIN.name());

    ProjectMember updatedMember = projectMemberRepository
        .findById(targetMember.getId()).block();
    assertThat(updatedMember.getRole())
        .isEqualTo(ProjectRole.ADMIN.name());
  }

  @Test
  @DisplayName("자기 자신의 역할은 변경할 수 없다")
  void updateMemberRole_SelfModification_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    UpdateProjectMemberRoleRequest request = updateProjectMemberRoleRequest(
        "VIEWER");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}/role",
            project.getId(), testUserId)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("ADMIN 권한 없이 역할 변경은 실패한다")
  void updateMemberRole_NoAdminAccess_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember viewerMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    UpdateProjectMemberRoleRequest request = updateProjectMemberRoleRequest(
        "EDITOR");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}/role",
            project.getId(), testUserId)
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("존재하지 않는 멤버 역할 변경 시도시 실패한다")
  void updateMemberRole_MemberNotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    UpdateProjectMemberRoleRequest request = updateProjectMemberRoleRequest(
        "EDITOR");

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}/role",
            project.getId(), "nonexistent123")
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("멤버 제거에 성공한다")
  void removeMember_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember targetMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}",
            project.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("project-member-remove",
            ProjectApiSnippets.removeMemberPathParameters(),
            ProjectApiSnippets.removeMemberRequestHeaders()));

    ProjectMember deletedMember = projectMemberRepository
        .findById(targetMember.getId()).block();
    assertThat(deletedMember.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("프로젝트 ADMIN은 다른 ADMIN을 제거할 수 있다")
  void removeMember_AdminRemovesAnotherAdmin_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember1 = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember adminMember2 = addProjectMember(project.getId(), testUser2Id, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}",
            project.getId(), testUser2Id)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent();

    ProjectMember deletedMember = projectMemberRepository
        .findById(adminMember2.getId()).block();
    assertThat(deletedMember).isNotNull();
    assertThat(deletedMember.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("ADMIN 권한 없이 멤버 제거할 수 없다")
  void removeMember_NoAdminAccess_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember viewerMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}",
            project.getId(), testUserId)
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("존재하지 않는 멤버를 제거할 수 없다")
  void removeMember_MemberNotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/{userId}",
            project.getId(), "nonexistent123")
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("사용자는 프로젝트를 자발적으로 탈퇴할 수 있다")
  void leaveProject_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember viewerMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/me",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isNoContent().expectBody()
        .consumeWith(document("project-member-leave",
            ProjectApiSnippets.leaveProjectPathParameters(),
            ProjectApiSnippets.leaveProjectRequestHeaders()));

    ProjectMember deletedMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(),
            testUser2Id)
        .block();
    assertThat(deletedMember).isNull();
  }

  @Test
  @DisplayName("마지막 멤버 탈퇴시 프로젝트도 삭제된다")
  void leaveProject_LastMember_ProjectDeleted() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/me",
            project.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent();

    Project deletedProject = projectRepository
        .findByIdAndNotDeleted(project.getId()).block();
    assertThat(deletedProject).isNull();
  }

  @Test
  @DisplayName("마지막 ADMIN도 워크스페이스 내에 멤버가 존재하면 탈퇴할 수 있다")
  void leaveProject_LastAdmin_Allowed() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember viewerMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/me",
            project.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent();

    ProjectMember deletedAdmin = projectMemberRepository.findById(adminMember.getId()).block();
    assertThat(deletedAdmin).isNotNull();
    assertThat(deletedAdmin.isDeleted()).isTrue();

    ProjectMember remainedViewer = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), testUser2Id)
        .block();
    assertThat(remainedViewer).isNotNull();

    Project remainedProject = projectRepository.findByIdAndNotDeleted(project.getId()).block();
    assertThat(remainedProject).isNotNull();
  }

  @Test
  @DisplayName("프로젝트 멤버가 아닌 경우 탈퇴 시도시 실패한다")
  void leaveProject_NotMember_NotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/members/me",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("공유 링크 생성에 성공한다")
  void createShareLink_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .consumeWith(document("share-link-create",
            ShareLinkApiSnippets.createShareLinkPathParameters(),
            ShareLinkApiSnippets.createShareLinkRequestHeaders(),
            ShareLinkApiSnippets.createShareLinkResponseHeaders(),
            ShareLinkApiSnippets.createShareLinkResponse()))
        .jsonPath("$.projectId").isEqualTo(project.getId())
        .jsonPath("$.url").value(url -> assertThat(url.toString()).contains(PUBLIC_API_PREFIX + "/share/"))
        .jsonPath("$.isRevoked").isEqualTo(false)
        .jsonPath("$.accessCount").isEqualTo(0);
  }

  @Test
  @DisplayName("공유 링크 목록 조회에 성공한다")
  void getShareLinks_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links?page=0&size=10",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("share-link-list",
            ShareLinkApiSnippets.getShareLinksPathParameters(),
            ShareLinkApiSnippets.getShareLinksRequestHeaders(),
            ShareLinkApiSnippets.getShareLinksQueryParameters(),
            ShareLinkApiSnippets.getShareLinksResponseHeaders(),
            ShareLinkApiSnippets.getShareLinksResponse()))
        .jsonPath("$.content").isArray()
        .jsonPath("$.content[0].url").value(url -> assertThat(url.toString()).contains(PUBLIC_API_PREFIX + "/share/"))
        .jsonPath("$.totalElements").isEqualTo(1);
  }

  @Test
  @DisplayName("공유 링크 상세 조회에 성공한다")
  void getShareLink_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    String shareLinkCode = generateShareLinkCode();
    ShareLink shareLink = saveShareLink(project.getId(), shareLinkCode);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("share-link-get",
            ShareLinkApiSnippets.getShareLinkPathParameters(),
            ShareLinkApiSnippets.getShareLinkRequestHeaders(),
            ShareLinkApiSnippets.getShareLinkResponseHeaders(),
            ShareLinkApiSnippets.getShareLinkResponse()))
        .jsonPath("$.id").isEqualTo(shareLink.getId())
        .jsonPath("$.url").value(url -> assertThat(url.toString()).contains(PUBLIC_API_PREFIX + "/share/"))
        .jsonPath("$.isRevoked").isEqualTo(false);
  }

  @Test
  @DisplayName("공유 링크 비활성화에 성공한다")
  void revokeShareLink_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}/revoke",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("share-link-revoke",
            ShareLinkApiSnippets.revokeShareLinkPathParameters(),
            ShareLinkApiSnippets.revokeShareLinkRequestHeaders(),
            ShareLinkApiSnippets.revokeShareLinkResponseHeaders(),
            ShareLinkApiSnippets.revokeShareLinkResponse()))
        .jsonPath("$.id").isEqualTo(shareLink.getId())
        .jsonPath("$.isRevoked").isEqualTo(true);
  }

  @Test
  @DisplayName("공유 링크 삭제에 성공한다")
  void deleteShareLink_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("share-link-delete",
            ShareLinkApiSnippets.deleteShareLinkPathParameters(),
            ShareLinkApiSnippets.deleteShareLinkRequestHeaders()));
  }

  @Test
  @DisplayName("EDITOR는 공유 링크를 생성할 수 없다")
  void createShareLink_EditorRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUser2Id, ProjectRole.EDITOR);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("VIEWER는 공유 링크를 생성할 수 없다")
  void createShareLink_ViewerRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 멤버가 아닌 사용자는 공유 링크를 생성할 수 없다")
  void createShareLink_NotMember_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("VIEWER는 공유 링크 목록을 조회할 수 없다")
  void getShareLinks_ViewerRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("EDITOR는 공유 링크 상세를 조회할 수 없다")
  void getShareLink_EditorRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember editorMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.EDITOR);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("VIEWER는 공유 링크를 비활성화할 수 없다")
  void revokeShareLink_ViewerRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember viewerMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.VIEWER);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}/revoke",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("EDITOR는 공유 링크를 삭제할 수 없다")
  void deleteShareLink_EditorRole_Forbidden() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember adminMember = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ProjectMember editorMember = addProjectMember(project.getId(), testUser2Id, ProjectRole.EDITOR);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken2)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("존재하지 않는 공유 링크 조회 시 실패한다")
  void getShareLink_NotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), "nonexistent-id")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("존재하지 않는 공유 링크 비활성화 시 실패한다")
  void revokeShareLink_NotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}/revoke",
            project.getId(), "nonexistent-id")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("존재하지 않는 공유 링크 삭제 시 실패한다")
  void deleteShareLink_NotFound() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}",
            project.getId(), "nonexistent-id")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("존재하지 않는 프로젝트에 공유 링크 생성 시 실패한다")
  void createShareLink_ProjectNotFound() {
    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            "nonexistent-project-id")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("공유 링크 목록 조회 시 페이징이 정상 동작한다")
  void getShareLinks_Pagination_Success() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    for (int i = 0; i < 5; i++) {
      ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());
    }

    // first page
    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links?page=0&size=3",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isArray()
        .jsonPath("$.content.length()").isEqualTo(3)
        .jsonPath("$.page").isEqualTo(0)
        .jsonPath("$.size").isEqualTo(3)
        .jsonPath("$.totalElements").isEqualTo(5)
        .jsonPath("$.totalPages").isEqualTo(2);

    // second page
    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links?page=1&size=3",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isArray()
        .jsonPath("$.content.length()").isEqualTo(2)
        .jsonPath("$.page").isEqualTo(1)
        .jsonPath("$.totalElements").isEqualTo(5);
  }

  @Test
  @DisplayName("비활성화된 공유 링크도 목록에 포함된다")
  void getShareLinks_IncludesRevokedLinks() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ShareLink activeLink = saveShareLink(project.getId(), generateShareLinkCode());

    ShareLink revokedLink = saveShareLink(project.getId(), generateShareLinkCode());
    revokedLink.revoke();
    shareLinkRepository.save(revokedLink).block();

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isArray()
        .jsonPath("$.totalElements").isEqualTo(2);
  }

  @Test
  @DisplayName("이미 비활성화된 공유 링크를 다시 비활성화하려고 하면 실패한다")
  void revokeShareLink_AlreadyRevoked_Fail() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    ShareLink shareLink = saveShareLink(project.getId(), generateShareLinkCode());
    shareLink.revoke();
    shareLink = shareLinkRepository.save(shareLink).block();

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links/{shareLinkId}/revoke",
            project.getId(), shareLink.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("공유 링크가 없는 프로젝트의 목록 조회는 빈 배열을 반환한다")
  void getShareLinks_EmptyProject_ReturnsEmptyList() {
    Project project = saveProject(testWorkspaceId, "Test Project", "Description");

    ProjectMember member = addProjectMember(project.getId(), testUserId, ProjectRole.ADMIN);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/projects/{projectId}/share-links",
            project.getId())
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content").isArray()
        .jsonPath("$.content").isEmpty()
        .jsonPath("$.totalElements").isEqualTo(0);
  }

  private UpdateProjectMemberRoleRequest updateProjectMemberRoleRequest(
      String role) {
    return new UpdateProjectMemberRoleRequest(
        resolveRecordEnum(UpdateProjectMemberRoleRequest.class, 0, role));
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
