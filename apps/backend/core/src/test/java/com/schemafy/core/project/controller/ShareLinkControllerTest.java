package com.schemafy.core.project.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.RestDocsConfiguration;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.project.controller.dto.request.CreateShareLinkRequest;
import com.schemafy.core.project.docs.ShareLinkApiSnippets;
import com.schemafy.core.project.repository.*;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.project.service.ShareLinkTokenService;
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
@Import(RestDocsConfiguration.class)
@DisplayName("ShareLinkController 통합 테스트")
class ShareLinkControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private ShareLinkRepository shareLinkRepository;

  @Autowired
  private ShareLinkAccessLogRepository accessLogRepository;

  @Autowired
  private ShareLinkTokenService tokenService;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtProvider jwtProvider;

  private User testUser;
  private User testUser2;
  private Workspace testWorkspace;
  private Project testProject;
  private String accessToken;
  private String accessToken2;
  private String apiBasePath;

  @BeforeEach
  void setUp() {
    // Clean up in order of dependencies
    Mono.when(accessLogRepository.deleteAll(),
        shareLinkRepository.deleteAll(),
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(), userRepository.deleteAll())
        .block();

    // Create test users
    testUser = User
        .signUp(new UserInfo("owner@example.com", "Owner",
            "password"), new BCryptPasswordEncoder())
        .flatMap(userRepository::save).block();

    testUser2 = User
        .signUp(new UserInfo("member@example.com", "Member",
            "password"), new BCryptPasswordEncoder())
        .flatMap(userRepository::save).block();

    accessToken = generateAccessToken(testUser.getId());
    accessToken2 = generateAccessToken(testUser2.getId());

    // Create workspace
    testWorkspace = Workspace.create(testUser.getId(), "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    // Add workspace members
    WorkspaceMember member1 = WorkspaceMember.create(testWorkspace.getId(),
        testUser.getId(), WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member1).block();

    WorkspaceMember member2 = WorkspaceMember.create(testWorkspace.getId(),
        testUser2.getId(), WorkspaceRole.MEMBER);
    workspaceMemberRepository.save(member2).block();

    // Create project
    testProject = Project.create(testWorkspace.getId(),
        "Test Project", "Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();

    // Add project member (testUser as ADMIN)
    ProjectMember projectMember = ProjectMember.create(testProject.getId(),
        testUser.getId(), ProjectRole.ADMIN);
    projectMemberRepository.save(projectMember).block();

    apiBasePath = ApiPath.API.replace("{version}", "v1.0") + "/workspaces/"
        + testWorkspace.getId() + "/projects/" + testProject.getId()
        + "/share-links";
  }

  private String generateAccessToken(String userId) {
    return jwtProvider.generateAccessToken(userId, new HashMap<>(),
        System.currentTimeMillis());
  }

  @Test
  @DisplayName("공유 링크 생성에 성공한다")
  void createShareLink_Success() {
    CreateShareLinkRequest request = new CreateShareLinkRequest("viewer",
        null);

    webTestClient.post()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects/{projectId}/share-links",
            testWorkspace.getId(), testProject.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .consumeWith(document("share-link-create",
            ShareLinkApiSnippets.createShareLinkPathParameters(),
            ShareLinkApiSnippets.createShareLinkRequestHeaders(),
            ShareLinkApiSnippets.createShareLinkRequest(),
            ShareLinkApiSnippets.createShareLinkResponseHeaders(),
            ShareLinkApiSnippets.createShareLinkResponse()))
        .jsonPath("$.projectId").isEqualTo(testProject.getId())
        .jsonPath("$.role").isEqualTo("viewer")
        .jsonPath("$.token").isNotEmpty()
        .jsonPath("$.isRevoked").isEqualTo(false);
  }

  @Test
  @DisplayName("만료 시간을 설정하여 공유 링크를 생성할 수 있다")
  void createShareLink_WithExpiration() {
    Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
    CreateShareLinkRequest request = new CreateShareLinkRequest("editor",
        expiresAt);

    webTestClient.post().uri(apiBasePath)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isCreated().expectBody()
        .jsonPath("$.role").isEqualTo("editor")
        .jsonPath("$.expiresAt").isNotEmpty();
  }

  @Test
  @DisplayName("프로젝트 소유자가 아니면 공유 링크 생성에 실패한다")
  void createShareLink_FailsWhenNotOwner() {
    CreateShareLinkRequest request = new CreateShareLinkRequest("viewer",
        null);

    webTestClient.post().uri(apiBasePath)
        .header("Authorization", "Bearer " + accessToken2)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isForbidden();
  }

  @Test
  @DisplayName("인증 없이 공유 링크 생성에 실패한다")
  void createShareLink_FailsWithoutAuth() {
    CreateShareLinkRequest request = new CreateShareLinkRequest("viewer",
        null);

    webTestClient.post().uri(apiBasePath)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
        .exchange().expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("공유 링크 목록 조회에 성공한다")
  void getShareLinks_Success() {
    // Create share links
    createTestShareLink(ShareLinkRole.VIEWER);
    createTestShareLink(ShareLinkRole.EDITOR);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects/{projectId}/share-links?page=0&size=20",
            testWorkspace.getId(), testProject.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("share-link-list",
            ShareLinkApiSnippets.getShareLinksPathParameters(),
            ShareLinkApiSnippets.getShareLinksRequestHeaders(),
            ShareLinkApiSnippets.getShareLinksQueryParameters(),
            ShareLinkApiSnippets.getShareLinksResponseHeaders(),
            ShareLinkApiSnippets.getShareLinksResponse()))
        .jsonPath("$.totalElements").isEqualTo(2)
        .jsonPath("$.content").isArray();
  }

  @Test
  @DisplayName("공유 링크 상세 조회에 성공한다")
  void getShareLink_Success() {
    ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER);

    webTestClient.get()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}",
            testWorkspace.getId(), testProject.getId(),
            shareLink.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("share-link-get",
            ShareLinkApiSnippets.getShareLinkPathParameters(),
            ShareLinkApiSnippets.getShareLinkRequestHeaders(),
            ShareLinkApiSnippets.getShareLinkResponseHeaders(),
            ShareLinkApiSnippets.getShareLinkResponse()))
        .jsonPath("$.id")
        .isEqualTo(shareLink.getId()).jsonPath("$.role")
        .isEqualTo("viewer");
  }

  @Test
  @DisplayName("존재하지 않는 공유 링크 조회에 실패한다")
  void getShareLink_NotFound() {
    webTestClient.get().uri(apiBasePath + "/nonexistent-id")
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("공유 링크 비활성화에 성공한다")
  void revokeShareLink_Success() {
    ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER);

    webTestClient.patch()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke",
            testWorkspace.getId(), testProject.getId(),
            shareLink.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("share-link-revoke",
            ShareLinkApiSnippets.revokeShareLinkPathParameters(),
            ShareLinkApiSnippets.revokeShareLinkRequestHeaders()));

    // Verify revoked
    ShareLink revoked = shareLinkRepository
        .findByIdAndNotDeleted(shareLink.getId()).block();
    assertThat(revoked.getIsRevoked()).isTrue();
  }

  @Test
  @DisplayName("프로젝트 소유자가 아니면 공유 링크 비활성화에 실패한다")
  void revokeShareLink_FailsWhenNotOwner() {
    ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER);

    webTestClient.patch()
        .uri(apiBasePath + "/" + shareLink.getId() + "/revoke")
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("공유 링크 삭제에 성공한다")
  void deleteShareLink_Success() {
    ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER);

    webTestClient.delete()
        .uri(ApiPath.API.replace("{version}", "v1.0")
            + "/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}",
            testWorkspace.getId(), testProject.getId(),
            shareLink.getId())
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("share-link-delete",
            ShareLinkApiSnippets.deleteShareLinkPathParameters(),
            ShareLinkApiSnippets.deleteShareLinkRequestHeaders()));

    // Verify soft deleted
    ShareLink deleted = shareLinkRepository.findById(shareLink.getId())
        .block();
    assertThat(deleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("프로젝트 소유자가 아니면 공유 링크 삭제에 실패한다")
  void deleteShareLink_FailsWhenNotOwner() {
    ShareLink shareLink = createTestShareLink(ShareLinkRole.VIEWER);

    webTestClient.delete().uri(apiBasePath + "/" + shareLink.getId())
        .header("Authorization", "Bearer " + accessToken2).exchange()
        .expectStatus().isForbidden();
  }

  private ShareLink createTestShareLink(ShareLinkRole role) {
    String token = tokenService.generateToken();
    byte[] tokenHash = tokenService.hashToken(token);
    ShareLink shareLink = ShareLink.create(testProject.getId(), tokenHash,
        role, null);
    return shareLinkRepository.save(shareLink).block();
  }

}
