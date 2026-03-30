package com.schemafy.api.project.controller;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.adapter.out.persistence.ShareLinkRepository;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("PublicShareLinkController 복원력 테스트 (Resilience Test)")
class PublicShareLinkControllerResilienceTest extends ProjectHttpTestSupport {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Autowired
  private WebTestClient webTestClient;

  @MockitoSpyBean
  private ShareLinkRepository shareLinkRepository;

  private User testUser;
  private Workspace testWorkspace;
  private Project testProject;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    testUser = createUser("admin@example.com", "Admin");
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

  private ShareLink createShareLink(String code) {
    return saveShareLink(testProject.getId(), code);
  }

  @Test
  @DisplayName("incrementAccessCount가 실패해도 프로젝트 조회는 성공한다")
  void accessByLink_IncrementAccessCountFailure_StillSucceeds() {
    String code = generateLinkCode();
    createShareLink(code);

    // incrementAccessCount만 실패하도록 설정
    doReturn(Mono.error(new RuntimeException("DB write failed")))
        .when(shareLinkRepository)
        .incrementAccessCount(anyString());

    // 접근은 성공해야 함. accessCount는 증가하지 않았지만, 응답은 성공
    webTestClient.get()
        .uri(PUBLIC_API_PATH + "/{code}", code)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.projectId").isEqualTo(testProject.getId())
        .jsonPath("$.projectName").isEqualTo("Test Project");
  }

}
