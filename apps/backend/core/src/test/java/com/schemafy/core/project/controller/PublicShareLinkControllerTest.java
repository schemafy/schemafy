package com.schemafy.core.project.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;

import static com.schemafy.core.project.docs.PublicShareLinkApiSnippets.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
@DisplayName("PublicShareLinkController 통합 테스트")
class PublicShareLinkControllerTest {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private ShareLinkRepository shareLinkRepository;

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
    Mono.when(shareLinkRepository.deleteAll(),
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
    testWorkspace = Workspace.create("Test Workspace",
        "Description");
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

  private String generateLinkCode() {
    byte[] bytes = new byte[24];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Test
  @DisplayName("유효한 코드로 프로젝트에 접근할 수 있다")
  void accessByCode_Success() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    webTestClient.get()
        .uri(PUBLIC_API_PATH + "/{code}", code)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("public-share-link/access-by-code",
            accessByCodePathParameters(),
            accessByCodeResponseHeaders(),
            accessByCodeResponse()))
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.projectId").isEqualTo(testProject.getId())
        .jsonPath("$.result.projectName").isEqualTo("Test Project");
  }

  @Test
  @DisplayName("로그인 사용자도 공유 링크로 접근할 수 있다")
  void accessByCode_Authenticated() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk().expectBody().jsonPath("$.success")
        .isEqualTo(true).jsonPath("$.result.projectId")
        .isEqualTo(testProject.getId());
  }

  @Test
  @DisplayName("유효하지 않은 코드로 접근하면 실패한다")
  void accessByCode_InvalidCode() {
    webTestClient.get().uri(PUBLIC_API_PATH + "/invalid-code").exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("비활성화된 공유 링크로 접근하면 실패한다")
  void accessByCode_RevokedLink() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);
    shareLink.revoke();
    shareLinkRepository.save(shareLink).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("만료된 공유 링크로 접근하면 실패한다")
  void accessByCode_ExpiredLink() {
    String code = generateLinkCode();
    Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
    ShareLink shareLink = createShareLink(code, pastDate);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("삭제된 공유 링크로 접근하면 실패한다")
  void accessByCode_DeletedLink() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);
    shareLink.delete();
    shareLinkRepository.save(shareLink).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("프로젝트가 삭제된 경우 접근하면 실패한다")
  void accessByCode_DeletedProject() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    // Delete project
    testProject.delete();
    projectRepository.save(testProject).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isNotFound().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("P001");
  }

  @Test
  @DisplayName("공유 링크 접근 시 accessCount가 증가한다")
  void accessByCode_IncrementAccessCount() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    // Initial access count should be 0
    ShareLink initial = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(initial).isNotNull();
    assertThat(initial.getAccessCount()).isZero();

    // Access the share link
    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isOk();

    // Check access count increased (no need to wait - then() blocks until complete)
    ShareLink updated = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(updated).isNotNull();
    assertThat(updated.getAccessCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("공유 링크를 여러 번 접근하면 accessCount가 누적된다")
  void accessByCode_MultipleAccessIncrements() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    // Access 3 times
    for (int i = 0; i < 3; i++) {
      webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
          .expectStatus().isOk();
    }

    // Check access count is 3 (no need to wait - then() blocks until complete)
    ShareLink updated = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(updated).isNotNull();
    assertThat(updated.getAccessCount()).isEqualTo(3L);
  }

  @Test
  @DisplayName("만료 시간 경계값 - 정확히 만료 시간에는 접근할 수 없다")
  void accessByCode_ExactExpirationTime() {
    String code = generateLinkCode();
    Instant expiryTime = Instant.now().plusSeconds(1);
    ShareLink shareLink = createShareLink(code, expiryTime);

    // Wait until expiry time passes
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isUnauthorized().expectBody()
        .jsonPath("$.success").isEqualTo(false).jsonPath("$.error.code")
        .isEqualTo("S004");
  }

  @Test
  @DisplayName("만료되지 않은 링크는 접근할 수 있다")
  void accessByCode_NotExpiredYet() {
    String code = generateLinkCode();
    Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
    ShareLink shareLink = createShareLink(code, futureExpiry);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isOk().expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.projectId").isEqualTo(testProject.getId());
  }

  private ShareLink createShareLink(String code, Instant expiresAt) {
    ShareLink shareLink;

    if (expiresAt != null && !Instant.now().isBefore(expiresAt)) {
      // Create with future date first, then change to past
      shareLink = ShareLink.create(testProject.getId(), code,
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
      shareLink = ShareLink.create(testProject.getId(), code, expiresAt);
    }
    return shareLinkRepository.save(shareLink).block();
  }

}
