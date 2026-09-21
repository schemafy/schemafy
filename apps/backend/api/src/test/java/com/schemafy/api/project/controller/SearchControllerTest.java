package com.schemafy.api.project.controller;

import java.net.URI;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.project.docs.SearchApiSnippets;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.user.domain.User;

import static com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("SearchController 통합 테스트")
class SearchControllerTest extends ProjectHttpTestSupport {

  private static final String API_PREFIX = ApiPath.API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  private String aliceId;
  private String bobId;
  private String outsiderId;
  private String aliceToken;
  private String bobToken;
  private String outsiderToken;
  private String workspaceId;
  private String workspaceProjectId;
  private String bobSharedProjectId;
  private String aliceSharedProjectId;

  @BeforeEach
  void setUp() {
    cleanupProjectFixtures().block();

    User alice = createUser("alice@example.com", "Alice Lee");
    User bob = createUser("bob@example.com", "Bob Lee");
    User outsider = createUser("mallory@example.com", "Mallory");
    aliceId = alice.id();
    bobId = bob.id();
    outsiderId = outsider.id();
    aliceToken = generateAccessToken(aliceId);
    bobToken = generateAccessToken(bobId);
    outsiderToken = generateAccessToken(outsiderId);

    Workspace workspace = saveWorkspace("Search Workspace", "Description");
    workspaceId = workspace.getId();
    addWorkspaceMember(workspaceId, aliceId, WorkspaceRole.ADMIN);
    addWorkspaceMember(workspaceId, bobId, WorkspaceRole.MEMBER);

    Project workspaceProject = saveProject(
        workspaceId, "Schema Workspace", "Workspace project");
    workspaceProjectId = workspaceProject.getId();
    addProjectMember(workspaceProjectId, aliceId, ProjectRole.ADMIN);
    addProjectMember(workspaceProjectId, bobId, ProjectRole.VIEWER);
    Project unrelatedProject = saveProject(
        workspaceId, "Unrelated", "Not a search match");
    addProjectMember(unrelatedProject.getId(), aliceId, ProjectRole.ADMIN);

    Workspace bobSharedWorkspace = saveWorkspace(
        "Bob Shared Workspace", "Description");
    addWorkspaceMember(
        bobSharedWorkspace.getId(), aliceId, WorkspaceRole.ADMIN);
    Project bobSharedProject = saveProject(
        bobSharedWorkspace.getId(), "Schema Shared Bob", "Direct share");
    bobSharedProjectId = bobSharedProject.getId();
    addProjectMember(bobSharedProjectId, aliceId, ProjectRole.ADMIN);
    addProjectMember(bobSharedProjectId, bobId, ProjectRole.EDITOR);

    Workspace aliceSharedWorkspace = saveWorkspace(
        "Alice Shared Workspace", "Description");
    addWorkspaceMember(
        aliceSharedWorkspace.getId(), outsiderId, WorkspaceRole.ADMIN);
    Project aliceSharedProject = saveProject(
        aliceSharedWorkspace.getId(), "Schema Shared Alice", "Direct share");
    aliceSharedProjectId = aliceSharedProject.getId();
    addProjectMember(aliceSharedProjectId, outsiderId, ProjectRole.ADMIN);
    addProjectMember(aliceSharedProjectId, aliceId, ProjectRole.VIEWER);
  }

  @Test
  @DisplayName("워크스페이스 프로젝트를 검색한다")
  void searchWorkspaceProjects() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/projects?category=WORKSPACE&workspaceId="
            + workspaceId + "&search=schema&page=0&size=5")
        .header("Authorization", "Bearer " + bobToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("search-projects-workspace",
            SearchApiSnippets.searchRequestHeaders(),
            SearchApiSnippets.projectQueryParameters(),
            SearchApiSnippets.searchResponseHeaders(),
            SearchApiSnippets.projectResponse()))
        .jsonPath("$.content[0].id").isEqualTo(workspaceProjectId)
        .jsonPath("$.content[0].workspaceId").isEqualTo(workspaceId)
        .jsonPath("$.content[0].dbVendorId").isEqualTo(DB_VENDOR_ID)
        .jsonPath("$.content[0].name").isEqualTo("Schema Workspace")
        .jsonPath("$.content[0].description")
        .isEqualTo("Workspace project")
        .jsonPath("$.content[0].myRole")
        .isEqualTo(ProjectRole.VIEWER.name())
        .jsonPath("$.content[0].createdAt").isNotEmpty()
        .jsonPath("$.content[0].updatedAt").isNotEmpty()
        .jsonPath("$.page").isEqualTo(0)
        .jsonPath("$.size").isEqualTo(5)
        .jsonPath("$.totalElements").isEqualTo(1)
        .jsonPath("$.totalPages").isEqualTo(1);
  }

  @Test
  @DisplayName("직접 공유된 프로젝트를 검색한다")
  void searchSharedProjects() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/projects?category=SHARED&search=schema&page=0&size=5")
        .header("Authorization", "Bearer " + bobToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("search-projects-shared",
            SearchApiSnippets.searchRequestHeaders(),
            SearchApiSnippets.projectQueryParameters(),
            SearchApiSnippets.searchResponseHeaders(),
            SearchApiSnippets.projectResponse()))
        .jsonPath("$.content[0].id").isEqualTo(bobSharedProjectId)
        .jsonPath("$.content[0].myRole")
        .isEqualTo(ProjectRole.EDITOR.name())
        .jsonPath("$.totalElements").isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 멤버를 검색한다")
  void searchWorkspaceMembers() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/members?category=WORKSPACE&workspaceId="
            + workspaceId + "&search=lee&page=0&size=5")
        .header("Authorization", "Bearer " + aliceToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("search-members-workspace",
            SearchApiSnippets.searchRequestHeaders(),
            SearchApiSnippets.memberQueryParameters(),
            SearchApiSnippets.searchResponseHeaders(),
            SearchApiSnippets.memberResponse()))
        .jsonPath("$.content[?(@.userId == '" + aliceId + "')].userId")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of(aliceId)))
        .jsonPath("$.content[?(@.userId == '" + aliceId + "')].userName")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of("Alice Lee")))
        .jsonPath("$.content[?(@.userId == '" + aliceId + "')].userEmail")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of("alice@example.com")))
        .jsonPath("$.content[?(@.userId == '" + aliceId + "')].role")
        .value(value -> assertThat(value).isEqualTo(
            java.util.List.of(WorkspaceRole.ADMIN.name())))
        .jsonPath("$.content[?(@.userId == '" + bobId + "')].userId")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of(bobId)))
        .jsonPath("$.content[0].joinedAt").isNotEmpty()
        .jsonPath("$.content[0].workspaceId").doesNotExist()
        .jsonPath("$.content[0].projectId").doesNotExist()
        .jsonPath("$.totalElements").isEqualTo(2);
  }

  @Test
  @DisplayName("프로젝트 멤버를 검색한다")
  void searchProjectMembers() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/members?category=PROJECT&projectId="
            + workspaceProjectId + "&search=lee&page=0&size=5")
        .header("Authorization", "Bearer " + bobToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("search-members-project",
            SearchApiSnippets.searchRequestHeaders(),
            SearchApiSnippets.memberQueryParameters(),
            SearchApiSnippets.searchResponseHeaders(),
            SearchApiSnippets.memberResponse()))
        .jsonPath("$.content[?(@.userId == '" + aliceId + "')].userId")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of(aliceId)))
        .jsonPath("$.content[?(@.userId == '" + bobId + "')].userId")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of(bobId)))
        .jsonPath("$.content[?(@.userId == '" + bobId + "')].userName")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of("Bob Lee")))
        .jsonPath("$.content[?(@.userId == '" + bobId + "')].userEmail")
        .value(value -> assertThat(value).isEqualTo(java.util.List.of("bob@example.com")))
        .jsonPath("$.content[?(@.userId == '" + bobId + "')].role")
        .value(value -> assertThat(value).isEqualTo(
            java.util.List.of(ProjectRole.VIEWER.name())))
        .jsonPath("$.content[0].joinedAt").isNotEmpty()
        .jsonPath("$.content[0].workspaceId").doesNotExist()
        .jsonPath("$.content[0].projectId").doesNotExist()
        .jsonPath("$.totalElements").isEqualTo(2);
  }

  @Test
  @DisplayName("공유 프로젝트 검색의 requesterId는 인증 주체에서만 가져온다")
  void sharedSearchUsesOnlyAuthenticatedPrincipal() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/projects?category=SHARED&search=schema"
            + "&requesterId=" + aliceId)
        .header("Authorization", "Bearer " + bobToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.content.length()").isEqualTo(1)
        .jsonPath("$.content[0].id").isEqualTo(bobSharedProjectId)
        .jsonPath("$.content[?(@.id == '" + aliceSharedProjectId + "')]")
        .isEmpty()
        .jsonPath("$.page").isEqualTo(0)
        .jsonPath("$.size").isEqualTo(5);
  }

  @DisplayName("잘못된 검색 요청은 400 Bad Request를 반환한다")
  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidSearchRequests")
  void rejectInvalidSearchRequests(String description, String uri) {
    webTestClient.get()
        .uri(URI.create(uri))
        .header("Authorization", "Bearer " + aliceToken)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());
  }

  @Test
  @DisplayName("워크스페이스 외부 요청자는 워크스페이스 검색을 할 수 없다")
  void rejectRequesterOutsideWorkspace() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/projects?category=WORKSPACE&workspaceId="
            + workspaceId + "&search=schema")
        .header("Authorization", "Bearer " + outsiderToken)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("프로젝트 외부 요청자는 프로젝트 멤버 검색을 할 수 없다")
  void rejectRequesterOutsideProject() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/members?category=PROJECT&projectId="
            + workspaceProjectId + "&search=lee")
        .header("Authorization", "Bearer " + outsiderToken)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  @DisplayName("인증하지 않은 요청은 거부한다")
  void rejectUnauthenticatedRequest() {
    webTestClient.get()
        .uri(API_PREFIX
            + "/search/projects?category=SHARED&search=schema")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  private static Stream<Arguments> invalidSearchRequests() {
    String projects = API_PREFIX + "/search/projects";
    String members = API_PREFIX + "/search/members";
    return Stream.of(
        Arguments.of("missing search",
            projects + "?category=SHARED"),
        Arguments.of("empty search",
            projects + "?category=SHARED&search="),
        Arguments.of("blank search",
            projects + "?category=SHARED&search=%20%20"),
        Arguments.of("over 100 Unicode code points",
            projects + "?category=SHARED&search=" + "a".repeat(101)),
        Arguments.of("NUL search",
            projects + "?category=SHARED&search=%00"),
        Arguments.of("control-character search",
            projects + "?category=SHARED&search=%0A"),
        Arguments.of("negative page",
            projects + "?category=SHARED&search=schema&page=-1"),
        Arguments.of("page over 10000",
            projects + "?category=SHARED&search=schema&page=10001"),
        Arguments.of("zero size",
            projects + "?category=SHARED&search=schema&size=0"),
        Arguments.of("size over 100",
            projects + "?category=SHARED&search=schema&size=101"),
        Arguments.of("unknown project category",
            projects + "?category=UNKNOWN&search=schema"),
        Arguments.of("workspace project search without workspaceId",
            projects + "?category=WORKSPACE&search=schema"),
        Arguments.of("workspace project search with blank workspaceId",
            projects
                + "?category=WORKSPACE&workspaceId=%20&search=schema"),
        Arguments.of("shared search with workspaceId",
            projects
                + "?category=SHARED&workspaceId=" + "0".repeat(26)
                + "&search=schema"),
        Arguments.of("unknown member category",
            members + "?category=UNKNOWN&search=lee"),
        Arguments.of("workspace member search without workspaceId",
            members + "?category=WORKSPACE&search=lee"),
        Arguments.of("workspace member search with projectId",
            members
                + "?category=WORKSPACE&workspaceId=" + "0".repeat(26)
                + "&projectId=" + "1".repeat(26) + "&search=lee"),
        Arguments.of("project member search without projectId",
            members + "?category=PROJECT&search=lee"),
        Arguments.of("project member search with workspaceId",
            members
                + "?category=PROJECT&projectId=" + "1".repeat(26)
                + "&workspaceId=" + "0".repeat(26) + "&search=lee"));
  }

}
