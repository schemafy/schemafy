package com.schemafy.core.project.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.RestDocsConfiguration;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.project.docs.ShareLinkApiSnippets;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.ShareLinkAccessLogRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.project.service.ShareLinkTokenService;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
@DisplayName("PublicShareLinkController 통합 테스트")
class PublicShareLinkControllerTest {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";

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
  private UserRepository userRepository;

  @Autowired
  private JwtProvider jwtProvider;

  private User testUser;
  private Workspace testWorkspace;
  private Project testProject;
  private String accessToken;

  @BeforeEach
  void setUp() {
    // Clean up in order of dependencies
    Mono.when(accessLogRepository.deleteAll(),
        shareLinkRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(), userRepository.deleteAll())
        .block();

    // Create test user
    testUser = User
        .signUp(new UserInfo("owner@example.com", "Owner",
            "password"), new BCryptPasswordEncoder())
        .flatMap(userRepository::save).block();

    accessToken = generateAccessToken(testUser.getId());

    // Create workspace
    testWorkspace = Workspace.create(testUser.getId(), "Test Workspace",
        "Description", WorkspaceSettings.defaultSettings());
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    // Add workspace member
    WorkspaceMember member = WorkspaceMember.create(testWorkspace.getId(),
        testUser.getId(), WorkspaceRole.ADMIN);
    workspaceMemberRepository.save(member).block();

    // Create project owned by testUser
    testProject = Project.create(testWorkspace.getId(),
        "Test Project", "Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();
  }

  private String generateAccessToken(String userId) {
    return jwtProvider.generateAccessToken(userId, new HashMap<>(),
        System.currentTimeMillis());
  }

  @Test
  @DisplayName("유효한 토큰으로 프로젝트에 접근할 수 있다 (익명)")
  void accessByToken_Anonymous_Success() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.EDITOR,
        null);

    webTestClient.get()
        .uri(PUBLIC_API_PATH + "/{token}", token)
        .exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("share-link-public-access",
            ShareLinkApiSnippets.accessShareLinkPublicPathParameters(),
            ShareLinkApiSnippets.accessShareLinkPublicResponseHeaders(),
            ShareLinkApiSnippets.accessShareLinkPublicResponse()))
        .jsonPath("$.success")
        .isEqualTo(true).jsonPath("$.result.projectId")
        .isEqualTo(testProject.getId()).jsonPath("$.result.projectName")
        .isEqualTo("Test Project").jsonPath("$.result.grantedRole")
        .isEqualTo("viewer") // Anonymous gets VIEWER
        .jsonPath("$.result.canEdit").isEqualTo(false)
        .jsonPath("$.result.canComment").isEqualTo(false);
  }

  @Test
  @DisplayName("로그인 사용자는 ShareLink의 role을 부여받는다")
  void accessByToken_Authenticated_GetsShareLinkRole() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.EDITOR,
        null);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody().jsonPath("$.success")
        .isEqualTo(true).jsonPath("$.result.grantedRole")
        .isEqualTo("editor") // Gets ShareLink's role
        .jsonPath("$.result.canEdit").isEqualTo(true)
        .jsonPath("$.result.canComment").isEqualTo(true);
  }

  @Test
  @DisplayName("VIEWER 권한의 ShareLink로 접근하면 읽기만 가능하다")
  void accessByToken_ViewerRole() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.VIEWER,
        null);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .jsonPath("$.result.grantedRole").isEqualTo("viewer")
        .jsonPath("$.result.canEdit").isEqualTo(false)
        .jsonPath("$.result.canComment").isEqualTo(false);
  }

  @Test
  @DisplayName("COMMENTER 권한의 ShareLink로 접근하면 댓글 작성이 가능하다")
  void accessByToken_CommenterRole() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.COMMENTER,
        null);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody()
        .jsonPath("$.result.grantedRole").isEqualTo("commenter")
        .jsonPath("$.result.canEdit").isEqualTo(false)
        .jsonPath("$.result.canComment").isEqualTo(true);
  }

  @Test
  @DisplayName("유효하지 않은 토큰으로 접근하면 실패한다")
  void accessByToken_InvalidToken() {
    webTestClient.get().uri(PUBLIC_API_PATH + "/invalid-token").exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("비활성화된 공유 링크로 접근하면 실패한다")
  void accessByToken_RevokedLink() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.VIEWER,
        null);
    shareLink.revoke();
    shareLinkRepository.save(shareLink).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("만료된 공유 링크로 접근하면 실패한다")
  void accessByToken_ExpiredLink() {
    String token = tokenService.generateToken();
    Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
    ShareLink shareLink = createShareLink(token, ShareLinkRole.VIEWER,
        pastDate);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("삭제된 공유 링크로 접근하면 실패한다")
  void accessByToken_DeletedLink() {
    String token = tokenService.generateToken();
    ShareLink shareLink = createShareLink(token, ShareLinkRole.VIEWER,
        null);
    shareLink.delete();
    shareLinkRepository.save(shareLink).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + token).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  private ShareLink createShareLink(String token, ShareLinkRole role,
      Instant expiresAt) {
    byte[] tokenHash = tokenService.hashToken(token);
    ShareLink shareLink;

    if (expiresAt != null && !Instant.now().isBefore(expiresAt)) {
      // Create with future date first, then change to past
      shareLink = ShareLink.create(testProject.getId(), tokenHash, role,
          Instant.now().plus(1, ChronoUnit.DAYS));
      shareLinkRepository.save(shareLink).block();

      // Change expiresAt to past using reflection
      try {
        var field = ShareLink.class.getDeclaredField("expiresAt");
        field.setAccessible(true);
        field.set(shareLink, expiresAt);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      shareLink = ShareLink.create(testProject.getId(), tokenHash, role,
          expiresAt);
    }
    return shareLinkRepository.save(shareLink).block();
  }

}
