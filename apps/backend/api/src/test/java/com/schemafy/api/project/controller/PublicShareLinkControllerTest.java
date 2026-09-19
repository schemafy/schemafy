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
import com.schemafy.api.project.docs.PublicShareLinkApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import static com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureWebTestClient
@DisplayName("PublicShareLinkController 통합 테스트")
class PublicShareLinkControllerTest extends ProjectHttpTestSupport {

  private static final String PUBLIC_API_PATH = "/public/api/v1.0/share";
  private static final String PROJECT_API_PATH = ApiPath.API.replace("{version}", "v1.0")
      + "/projects";

  @Autowired
  private WebTestClient webTestClient;

  private Project testProject;
  private String accessToken;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();
    User testUser = createUser("admin@example.com", "Admin");
    Workspace workspace = saveWorkspace("Test Workspace", "Description");
    addWorkspaceMember(workspace.getId(), testUser.id(), WorkspaceRole.ADMIN);
    testProject = saveProject(workspace.getId(), "Test Project", "Description");
    addProjectMember(testProject.getId(), testUser.id(), ProjectRole.ADMIN);
    accessToken = generateAccessToken(testUser.id());
  }

  @Test
  @DisplayName("활성화한 공유 링크로 프로젝트에 접근할 수 있다")
  void accessByActivatedLink() {
    updateActivation(true);
    ShareLink shareLink = shareLinkRepository.findByProjectIdAndNotDeleted(testProject.getId())
        .block();

    webTestClient.get().uri(PUBLIC_API_PATH + "/{code}", shareLink.getCode()).exchange()
        .expectStatus().isOk().expectBody()
        .consumeWith(document("public-share-link-access",
            PublicShareLinkApiSnippets.accessByLinkPathParameters(),
            PublicShareLinkApiSnippets.accessByLinkResponseHeaders(),
            PublicShareLinkApiSnippets.accessByLinkResponse()))
        .jsonPath("$.projectId").isEqualTo(testProject.getId());

    webTestClient.get().uri(PUBLIC_API_PATH + "/{code}", shareLink.getId()).exchange()
        .expectStatus().isNotFound().expectBody()
        .jsonPath("$.reason").isEqualTo(ShareLinkErrorCode.NOT_FOUND.code());

    webTestClient.get().uri(PUBLIC_API_PATH + "/{code}", "not-a-valid-code").exchange()
        .expectStatus().isNotFound().expectBody()
        .consumeWith(document("public-share-link-access-not-found",
            PublicShareLinkApiSnippets.accessByLinkPathParameters(),
            PublicShareLinkApiSnippets.accessByLinkResponseHeaders(),
            PublicShareLinkApiSnippets.accessByLinkErrorResponse()))
        .jsonPath("$.reason").isEqualTo(ShareLinkErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("활성화했던 공유 링크를 비활성화하면 프로젝트에 접근할 수 없다")
  void accessByDeactivatedLinkFails() {
    updateActivation(true);
    String shareLinkCode = shareLinkRepository.findByProjectIdAndNotDeleted(testProject.getId())
        .block().getCode();
    updateActivation(false);

    webTestClient.get().uri(PUBLIC_API_PATH + "/{code}", shareLinkCode).exchange()
        .expectStatus().isBadRequest().expectBody()
        .consumeWith(document("public-share-link-access-invalid-link",
            PublicShareLinkApiSnippets.accessByLinkPathParameters(),
            PublicShareLinkApiSnippets.accessByLinkResponseHeaders(),
            PublicShareLinkApiSnippets.accessByLinkErrorResponse()))
        .jsonPath("$.reason").isEqualTo(ShareLinkErrorCode.INVALID_LINK.code());
  }

  private void updateActivation(boolean isActive) {
    webTestClient.patch().uri(PROJECT_API_PATH + "/{projectId}/share-link", testProject.getId())
        .header("Authorization", "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"isActive\":" + isActive + "}")
        .exchange().expectStatus().isOk();
  }

}
