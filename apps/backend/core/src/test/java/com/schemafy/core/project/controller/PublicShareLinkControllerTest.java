package com.schemafy.core.project.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.RestDocsConfiguration;
import com.schemafy.core.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ShareLink;
import com.schemafy.domain.project.domain.Workspace;
import com.schemafy.domain.project.domain.WorkspaceRole;
import com.schemafy.domain.project.domain.exception.ProjectErrorCode;
import com.schemafy.domain.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.domain.user.domain.User;

import static com.schemafy.core.project.docs.PublicShareLinkApiSnippets.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
@DisplayName("PublicShareLinkController 통합 테스트")
class PublicShareLinkControllerTest extends ProjectHttpTestSupport {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Autowired
  private WebTestClient webTestClient;

  private User testUser;
  private Workspace testWorkspace;
  private Project testProject;
  private String accessToken;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    testUser = createUser("admin@example.com", "Admin");
    accessToken = generateAccessToken(testUser.id());
    testWorkspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(testWorkspace.getId(), testUser.id(),
        WorkspaceRole.ADMIN);
    testProject = saveProject(testWorkspace.getId(), "Test Project",
        "Description");
  }

  private String generateLinkCode() {
    byte[] bytes = new byte[24];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Test
  @DisplayName("유효한 코드로 프로젝트에 접근할 수 있다")
  void accessByLink_Success() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    webTestClient.get()
        .uri(PUBLIC_API_PATH + "/{code}", code)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("share-link-public-access",
            accessByLinkPathParameters(),
            accessByLinkResponseHeaders(),
            accessByLinkResponse()))
        .jsonPath("$.projectId").isEqualTo(testProject.getId())
        .jsonPath("$.projectName").isEqualTo("Test Project");
  }

  @Test
  @DisplayName("로그인 사용자도 공유 링크로 접근할 수 있다")
  void accessByLink_Authenticated() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code)
        .header("Authorization", "Bearer " + accessToken).exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.projectId").isEqualTo(testProject.getId());
  }

  @Test
  @DisplayName("유효하지 않은 코드로 접근하면 실패한다")
  void accessByLink_InvalidCode() {
    webTestClient.get().uri(PUBLIC_API_PATH + "/invalid-code").exchange()
        .expectStatus().isNotFound().expectBody()
        .jsonPath("$.status").isEqualTo(404).jsonPath("$.reason")
        .isEqualTo(ShareLinkErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("비활성화된 공유 링크로 접근하면 실패한다")
  void accessByLink_RevokedLink() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);
    shareLink.revoke();
    shareLinkRepository.save(shareLink).block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isBadRequest().expectBody()
        .jsonPath("$.status").isEqualTo(400).jsonPath("$.reason")
        .isEqualTo(ShareLinkErrorCode.INVALID_LINK.code());
  }

  @Test
  @DisplayName("만료된 공유 링크로 접근하면 실패한다")
  void accessByLink_ExpiredLink() {
    String code = generateLinkCode();
    Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
    ShareLink shareLink = createShareLink(code, pastDate);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isBadRequest().expectBody()
        .jsonPath("$.status").isEqualTo(400).jsonPath("$.reason")
        .isEqualTo(ShareLinkErrorCode.INVALID_LINK.code());
  }

  @Test
  @DisplayName("삭제된 공유 링크로 접근하면 실패한다")
  void accessByLink_DeletedLink() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);
    shareLink.delete();
    saveShareLink(shareLink);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isNotFound().expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason").isEqualTo(ShareLinkErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("프로젝트가 삭제된 경우 접근하면 실패한다")
  void accessByLink_DeletedProject() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    testProject.delete();
    saveProject(testProject);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isNotFound().expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason").isEqualTo(ProjectErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("공유 링크 접근 시 accessCount가 증가한다")
  void accessByLink_IncrementAccessCount() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    ShareLink initial = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(initial).isNotNull();
    assertThat(initial.getAccessCount()).isZero();

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isOk();

    // Check access count increased (no need to wait - then() blocks until complete)
    ShareLink updated = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(updated).isNotNull();
    assertThat(updated.getAccessCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("공유 링크를 여러 번 접근하면 accessCount가 누적된다")
  void accessByLink_MultipleAccessIncrements() {
    String code = generateLinkCode();
    ShareLink shareLink = createShareLink(code, null);

    for (int i = 0; i < 3; i++) {
      webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
          .expectStatus().isOk();
    }

    ShareLink updated = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(updated).isNotNull();
    assertThat(updated.getAccessCount()).isEqualTo(3L);
  }

  @Test
  @DisplayName("만료 시간 경계값 - 정확히 만료 시간에는 접근할 수 없다")
  void accessByLink_ExactExpirationTime() {
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
        .expectStatus().isBadRequest().expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason").isEqualTo(ShareLinkErrorCode.INVALID_LINK.code());
  }

  @Test
  @DisplayName("만료되지 않은 링크는 접근할 수 있다")
  void accessByLink_NotExpiredYet() {
    String code = generateLinkCode();
    Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
    ShareLink shareLink = createShareLink(code, futureExpiry);

    webTestClient.get().uri(PUBLIC_API_PATH + "/" + code).exchange()
        .expectStatus().isOk().expectBody()
        .jsonPath("$.projectId").isEqualTo(testProject.getId());
  }

  private ShareLink createShareLink(String code, Instant expiresAt) {
    return saveShareLink(testProject.getId(), code, expiresAt);
  }

}
