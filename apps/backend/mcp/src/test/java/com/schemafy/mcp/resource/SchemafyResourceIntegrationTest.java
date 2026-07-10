package com.schemafy.mcp.resource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.memo.application.port.in.GetMemoCommentsQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoCommentsUseCase;
import com.schemafy.core.erd.memo.application.port.in.GetMemoQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoUseCase;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.MemoComment;
import com.schemafy.core.erd.memo.domain.MemoDetail;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.port.in.ListDbVendorsUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;
import com.schemafy.core.mcp.application.port.in.GetMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpToken;
import com.schemafy.core.mcp.domain.McpTokenClaimSupport;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsQuery;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.core.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.core.project.application.port.in.GetProjectQuery;
import com.schemafy.core.project.application.port.in.GetProjectUseCase;
import com.schemafy.core.project.application.port.in.GetProjectsQuery;
import com.schemafy.core.project.application.port.in.GetProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspacesQuery;
import com.schemafy.core.project.application.port.in.GetWorkspacesUseCase;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.ProjectSummary;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.mcp.common.security.McpRateLimiter;
import com.schemafy.mcp.common.security.McpSecurityProperties;
import com.schemafy.mcp.common.security.McpTokenRevocationCache;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(SchemafyResourceIntegrationTest.TestResourceConfiguration.class)
class SchemafyResourceIntegrationTest {

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  McpSecurityProperties properties;

  @Autowired
  TestReadUseCases readUseCases;

  TestTokenFactory tokenFactory;

  @BeforeEach
  void setUp() {
    tokenFactory = new TestTokenFactory(properties);
    readUseCases.reset();
  }

  @Test
  @DisplayName("MCP resources/list와 resources/templates/list가 read-only resource surface를 노출한다")
  void listsResourcesAndTemplates() {
    String sessionId = initialize(tokenFactory.validToken());

    String tools = mcp(sessionId, tokenFactory.validToken(), "tools-1",
        "tools/list", Map.of());
    assertThat(tools)
        .contains("schemafy_list_workspaces")
        .contains(
            "Use first when the user asks about their Schemafy workspaces, projects, schemas, ERDs, or permissions")
        .contains("schemafy_list_projects")
        .contains("Use after schemafy_list_workspaces when a workspaceId is known")
        .contains("schemafy_list_schemas")
        .contains("Use after schemafy_list_schemas when a schemaId is known to enumerate ERD tables")
        .contains("schemafy_get_schema")
        .contains("readOnlyHint")
        .doesNotContain("schemafy_read_resource");

    String resources = mcp(sessionId, tokenFactory.validToken(), "resources-1",
        "resources/list", Map.of());
    assertThat(resources)
        .contains("schemafy://me")
        .contains("schemafy://vendors")
        .contains("schemafy://workspaces")
        .contains("schemafy://projects/shared/me");

    String templates = mcp(sessionId, tokenFactory.validToken(), "templates-1",
        "resources/templates/list", Map.of());
    assertThat(templates)
        .contains("schemafy://workspaces/{workspaceId}/projects")
        .contains("schemafy://projects/{projectId}/schemas")
        .contains("schemafy://tables/{tableId}/columns")
        .contains("schemafy://memos/{memoId}/comments")
        .doesNotContain("schemafy://schemas/{schemaId}/snapshot")
        .doesNotContain("schemafy://tables/{tableId}/snapshot");
  }

  @Test
  @DisplayName("schemafy://me와 workspace project 리소스를 읽을 수 있다")
  void readsResources() {
    String token = tokenFactory.validToken();
    String sessionId = initialize(token);

    String me = readResource(sessionId, token, "schemafy://me");
    assertThat(me)
        .contains("schemafy://me")
        .contains("\\\"userId\\\":\\\"user-1\\\"")
        .contains("\\\"mcp\\\"");

    String projects = readResource(sessionId, token,
        "schemafy://workspaces/workspace-1/projects");
    assertThat(projects)
        .contains("schemafy://workspaces/workspace-1/projects")
        .contains("project-1")
        .contains("Orders");
    assertThat(readUseCases.lastWorkspaceProjectsQuery.get().requesterId())
        .isEqualTo("user-1");
  }

  @Test
  @DisplayName("에이전트가 MCP tool로 각 조회 UseCase를 호출할 수 있다")
  void callsReadOnlyResourceTools() {
    String token = tokenFactory.validToken();
    String sessionId = initialize(token);

    String workspaces = callTool(sessionId, token, "schemafy_list_workspaces", Map.of());
    assertThat(workspaces)
        .contains("workspace-1")
        .contains("Main Workspace")
        .doesNotContain("\"isError\":true");

    String projects = callTool(sessionId, token, "schemafy_list_projects",
        Map.of("workspaceId", "workspace-1"));
    assertThat(projects)
        .contains("project-1")
        .contains("Orders")
        .doesNotContain("\"isError\":true");
    assertThat(readUseCases.lastWorkspaceProjectsQuery.get().requesterId())
        .isEqualTo("user-1");

    String schemas = callTool(sessionId, token, "schemafy_list_schemas",
        Map.of("projectId", "project-1"));
    assertThat(schemas)
        .contains("schema-1")
        .contains("commerce")
        .doesNotContain("\"isError\":true");

    String schema = callTool(sessionId, token, "schemafy_get_schema",
        Map.of("schemaId", "schema-1"));
    assertThat(schema)
        .contains("schema-1")
        .contains("commerce")
        .doesNotContain("\"isError\":true");
  }

  @Test
  @DisplayName("project membership 거부는 MCP resources/read 오류로 반환된다")
  void deniesProjectReadWhenMembershipFails() {
    String token = tokenFactory.validToken();
    String sessionId = initialize(token);

    String response = readResource(sessionId, token,
        "schemafy://projects/project-denied");

    assertThat(response)
        .contains("\"error\"")
        .contains("ACCESS_DENIED");
    assertThat(readUseCases.lastProjectQuery.get().requesterId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("MCP scope가 부족한 토큰은 resources/read 요청도 거부된다")
  void rejectsResourceReadWithoutRequiredScope() {
    String sessionId = initialize(tokenFactory.validToken());

    webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFactory.tokenWithoutRequiredScope())
        .header("Mcp-Session-Id", sessionId)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(Map.of(
            "jsonrpc", "2.0",
            "id", "read-without-scope",
            "method", "resources/read",
            "params", Map.of("uri", "schemafy://me")))
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.reason").isEqualTo("MCP_INSUFFICIENT_SCOPE")
        .jsonPath("$.status").isEqualTo(403)
        .jsonPath("$.instance").isEqualTo("/mcp");
  }

  private String initialize(String token) {
    EntityExchangeResult<byte[]> result = webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(Map.of(
            "jsonrpc", "2.0",
            "id", "init-1",
            "method", "initialize",
            "params", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                    "name", "schemafy-resource-test-client",
                    "version", "0.0.1"))))
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Mcp-Session-Id")
        .expectBody()
        .returnResult();

    assertThat(new String(result.getResponseBody(), StandardCharsets.UTF_8))
        .contains("Schemafy MCP exposes read-only access")
        .contains("schemafy_list_workspaces");

    return result.getResponseHeaders().getFirst("Mcp-Session-Id");
  }

  private String readResource(String sessionId, String token, String uri) {
    return mcp(sessionId, token, "read-" + uri, "resources/read",
        Map.of("uri", uri));
  }

  private String callTool(
      String sessionId,
      String token,
      String name,
      Map<String, Object> arguments) {
    return mcp(sessionId, token, "call-" + name, "tools/call",
        Map.of(
            "name", name,
            "arguments", arguments));
  }

  private String mcp(
      String sessionId,
      String token,
      String id,
      String method,
      Map<String, Object> params) {
    EntityExchangeResult<byte[]> result = webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .header("Mcp-Session-Id", sessionId)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "method", method,
            "params", params))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .returnResult();
    return new String(result.getResponseBody(), StandardCharsets.UTF_8);
  }

  static class TestResourceConfiguration {

    @Bean
    @Primary
    McpTokenRevocationCache tokenRevocationCache() {
      return tokenId -> Mono.just(false);
    }

    @Bean
    @Primary
    GetMcpTokenUseCase getMcpTokenUseCase(
        McpSecurityProperties properties,
        Clock clock) {
      McpToken token = McpToken.issue(
          "token-1",
          "user-1",
          properties.getToken().getRequiredScope(),
          clock.instant().minusSeconds(60),
          clock.instant().plusSeconds(3600));
      return query -> token.getId().equals(query.tokenId())
          ? Mono.just(token)
          : Mono.empty();
    }

    @Bean
    @Primary
    McpRateLimiter rateLimiter() {
      return claims -> Mono.just(true);
    }

    @Bean
    @Primary
    TestReadUseCases testReadUseCases() {
      return new TestReadUseCases();
    }

  }

  static class TestReadUseCases implements
      GetWorkspacesUseCase,
      GetWorkspaceUseCase,
      GetWorkspaceMembersUseCase,
      GetProjectsUseCase,
      GetProjectUseCase,
      GetProjectMembersUseCase,
      GetMySharedProjectsUseCase,
      ListDbVendorsUseCase,
      GetSchemasByProjectIdUseCase,
      GetSchemaUseCase,
      GetTablesBySchemaIdUseCase,
      GetTableUseCase,
      GetColumnsByTableIdUseCase,
      GetIndexesByTableIdUseCase,
      GetConstraintsByTableIdUseCase,
      GetRelationshipsByTableIdUseCase,
      GetMemosBySchemaIdUseCase,
      GetMemoUseCase,
      GetMemoCommentsUseCase {

    private final AtomicReference<GetProjectsQuery> lastWorkspaceProjectsQuery = new AtomicReference<>();
    private final AtomicReference<GetProjectQuery> lastProjectQuery = new AtomicReference<>();

    void reset() {
      lastWorkspaceProjectsQuery.set(null);
      lastProjectQuery.set(null);
    }

    @Override
    public Mono<PageResult<Workspace>> getWorkspaces(GetWorkspacesQuery query) {
      return Mono.just(PageResult.of(List.of(workspace()), query.page(), query.size(), 1));
    }

    @Override
    public Mono<WorkspaceDetail> getWorkspace(GetWorkspaceQuery query) {
      return Mono.just(new WorkspaceDetail(workspace(), 1L, WorkspaceRole.ADMIN.name()));
    }

    @Override
    public Mono<PageResult<WorkspaceMember>> getWorkspaceMembers(
        GetWorkspaceMembersQuery query) {
      WorkspaceMember member = WorkspaceMember.create(
          "workspace-member-1", query.workspaceId(), query.requesterId(), WorkspaceRole.ADMIN);
      return Mono.just(PageResult.of(List.of(member), query.page(), query.size(), 1));
    }

    @Override
    public Mono<PageResult<ProjectSummary>> getProjects(GetProjectsQuery query) {
      lastWorkspaceProjectsQuery.set(query);
      ProjectSummary summary = new ProjectSummary(project(query.workspaceId()), ProjectRole.VIEWER);
      return Mono.just(PageResult.of(List.of(summary), query.page(), query.size(), 1));
    }

    @Override
    public Mono<ProjectDetail> getProject(GetProjectQuery query) {
      lastProjectQuery.set(query);
      if ("project-denied".equals(query.projectId())) {
        return Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED));
      }
      return Mono.just(new ProjectDetail(project("workspace-1"), ProjectRole.VIEWER.name()));
    }

    @Override
    public Mono<PageResult<ProjectMember>> getProjectMembers(GetProjectMembersQuery query) {
      ProjectMember member = ProjectMember.create(
          "project-member-1", query.projectId(), query.requesterId(), ProjectRole.VIEWER);
      return Mono.just(PageResult.of(List.of(member), query.page(), query.size(), 1));
    }

    @Override
    public Mono<PageResult<ProjectSummary>> getMySharedProjects(
        GetMySharedProjectsQuery query) {
      ProjectSummary summary = new ProjectSummary(project("workspace-1"), ProjectRole.VIEWER);
      return Mono.just(PageResult.of(List.of(summary), query.page(), query.size(), 1));
    }

    @Override
    public Flux<DbVendorSummary> listDbVendors() {
      return Flux.just(new DbVendorSummary("MySQL 8", "mysql", "8.0"));
    }

    @Override
    public Flux<Schema> getSchemasByProjectId(GetSchemasByProjectIdQuery query) {
      return Flux.just(schema(query.projectId()));
    }

    @Override
    public Mono<Schema> getSchema(GetSchemaQuery query) {
      return Mono.just(schema("project-1"));
    }

    @Override
    public Flux<Table> getTablesBySchemaId(GetTablesBySchemaIdQuery query) {
      return Flux.just(table(query.schemaId()));
    }

    @Override
    public Mono<Table> getTable(GetTableQuery query) {
      return Mono.just(table("schema-1"));
    }

    @Override
    public Mono<List<Column>> getColumnsByTableId(GetColumnsByTableIdQuery query) {
      return Mono.just(List.of(new Column(
          "column-1", query.tableId(), "id", "BIGINT", null, 1, true,
          null, null, "primary key")));
    }

    @Override
    public Mono<List<Index>> getIndexesByTableId(GetIndexesByTableIdQuery query) {
      return Mono.just(List.of(new Index(
          "index-1", query.tableId(), "idx_orders_id", IndexType.BTREE)));
    }

    @Override
    public Mono<List<Constraint>> getConstraintsByTableId(
        GetConstraintsByTableIdQuery query) {
      return Mono.just(List.of(new Constraint(
          "constraint-1", query.tableId(), "pk_orders", ConstraintKind.PRIMARY_KEY, null, null)));
    }

    @Override
    public Mono<List<Relationship>> getRelationshipsByTableId(
        GetRelationshipsByTableIdQuery query) {
      return Mono.just(List.of(new Relationship(
          "relationship-1", query.tableId(), "table-2", "fk_orders_customer",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null)));
    }

    @Override
    public Flux<Memo> getMemosBySchemaId(GetMemosBySchemaIdQuery query) {
      return Flux.just(memo(query.schemaId()));
    }

    @Override
    public Mono<MemoDetail> getMemo(GetMemoQuery query) {
      Memo memo = memo("schema-1");
      MemoComment comment = comment(query.memoId());
      return Mono.just(new MemoDetail(memo, List.of(comment)));
    }

    @Override
    public Flux<MemoComment> getMemoComments(GetMemoCommentsQuery query) {
      return Flux.just(comment(query.memoId()));
    }

    private Workspace workspace() {
      return Workspace.create("workspace-1", "Main Workspace", "Primary workspace");
    }

    private Project project(String workspaceId) {
      return Project.create("project-1", workspaceId, "Orders", "Order domain");
    }

    private Schema schema(String projectId) {
      return new Schema("schema-1", projectId, "mysql", "commerce", null, null);
    }

    private Table table(String schemaId) {
      return new Table("table-1", schemaId, "orders", "utf8mb4", "utf8mb4_general_ci");
    }

    private Memo memo(String schemaId) {
      Instant now = Instant.parse("2026-01-01T00:00:00Z");
      return new Memo("memo-1", schemaId, "user-1", "{\"x\":1}", now, now, null);
    }

    private MemoComment comment(String memoId) {
      Instant now = Instant.parse("2026-01-01T00:00:00Z");
      return new MemoComment("memo-comment-1", memoId, "user-1", "Review note", now, now, null);
    }

  }

  static class TestTokenFactory {

    private final McpSecurityProperties properties;
    private final SecretKey secretKey;

    TestTokenFactory(McpSecurityProperties properties) {
      this.properties = properties;
      this.secretKey = Keys.hmacShaKeyFor(
          properties.getToken().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    String validToken() {
      return token(properties.getToken().getRequiredScope());
    }

    String tokenWithoutRequiredScope() {
      return token("schema:read");
    }

    private String token(String scope) {
      Instant now = Instant.now();
      return Jwts.builder()
          .id("token-1")
          .subject("user-1")
          .issuer(properties.getToken().getIssuer())
          .audience().add(properties.getToken().getAudience()).and()
          .issuedAt(Date.from(now.minusSeconds(60)))
          .expiration(Date.from(now.plusSeconds(3600)))
          .claim(McpTokenClaimSupport.TYPE, properties.getToken().getTokenType())
          .claim(McpTokenClaimSupport.SCOPE, scope)
          .signWith(secretKey)
          .compact();
    }

  }

}
