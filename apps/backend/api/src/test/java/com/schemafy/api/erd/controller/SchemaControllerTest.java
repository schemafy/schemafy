package com.schemafy.api.erd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.WithMockCustomUser;
import com.schemafy.api.erd.controller.dto.request.ChangeSchemaNameRequest;
import com.schemafy.api.erd.controller.dto.request.CreateSchemaRequest;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionResult;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.api.erd.controller.ErdOperationFixtures.CLIENT_OPERATION_ID;
import static com.schemafy.api.erd.controller.ErdOperationFixtures.COMMITTED_REVISION;
import static com.schemafy.api.erd.controller.ErdOperationFixtures.OP_ID;
import static com.schemafy.api.erd.controller.ErdOperationFixtures.committedOperation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("SchemaController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class SchemaControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CreateSchemaUseCase createSchemaUseCase;

  @MockitoBean
  private GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;

  @MockitoBean
  private GetSchemasByProjectIdUseCase getSchemasByProjectIdUseCase;

  @MockitoBean
  private ChangeSchemaNameUseCase changeSchemaNameUseCase;

  @MockitoBean
  private DeleteSchemaUseCase deleteSchemaUseCase;

  @Test
  @DisplayName("스키마 생성 API 문서화")
  void createSchema() throws Exception {
    CreateSchemaRequest request = new CreateSchemaRequest(
        "06D6VZBWHSDJBBG0H7D156YZ98",
        "mariadb",
        "test_schema",
        "utf8mb4",
        "utf8mb4_general_ci");

    CreateSchemaResult result = new CreateSchemaResult(
        "06D6W1GAHD51T5NJPK29Q6BCR8",
        "06D6VZBWHSDJBBG0H7D156YZ98",
        "mariadb",
        "test_schema",
        "utf8mb4",
        "utf8mb4_general_ci");

    given(createSchemaUseCase.createSchema(any(CreateSchemaCommand.class)))
        .willReturn(Mono.just(
            MutationResult.empty(result)
                .withOperation(committedOperation())));

    webTestClient.post()
        .uri(API_BASE_PATH + "/schemas")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.operation.opId").isEqualTo(OP_ID)
        .jsonPath("$.operation.clientOperationId")
        .isEqualTo(CLIENT_OPERATION_ID)
        .jsonPath("$.operation.committedRevision")
        .isEqualTo((int) COMMITTED_REVISION)
        .jsonPath("$.operation.derivationKind").isEqualTo("ORIGINAL")
        .consumeWith(document("schema-create",
            requestHeaders(
                headerWithName("Content-Type")
                    .description("요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("projectId").description("프로젝트 ID"),
                fieldWithPath("dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("name").description("스키마 이름"),
                fieldWithPath("charset").description("문자셋 (선택)").optional(),
                fieldWithPath("collation").description("콜레이션 (선택)").optional()),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("data").description("생성된 스키마 정보").optional(),
                fieldWithPath("data.id").description("스키마 ID"),
                fieldWithPath("data.projectId").description("프로젝트 ID"),
                fieldWithPath("data.dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("data.name").description("스키마 이름"),
                fieldWithPath("data.charset").description("문자셋").optional(),
                fieldWithPath("data.collation").description("콜레이션").optional(),
                fieldWithPath("affectedTableIds").description("영향받은 테이블 ID 목록"),
                fieldWithPath("operation").type(JsonFieldType.OBJECT)
                    .description("커밋된 ERD operation 메타데이터"),
                fieldWithPath("operation.opId").description("커밋된 operation ID"),
                fieldWithPath("operation.clientOperationId")
                    .description("클라이언트가 보낸 operation ID"),
                fieldWithPath("operation.committedRevision")
                    .description("커밋 후 schema revision"),
                fieldWithPath("operation.derivationKind")
                    .description("operation derivation kind"))));
  }

  @Test
  @DisplayName("존재하지 않는 프로젝트로 스키마 생성 시 404를 반환한다")
  void createSchemaReturnsNotFoundForMissingProject() throws Exception {
    CreateSchemaRequest request = new CreateSchemaRequest(
        "06D6VZBWHSDJBBG0H7D156YZ98",
        "mariadb",
        "test_schema",
        "utf8mb4",
        "utf8mb4_general_ci");

    given(createSchemaUseCase.createSchema(any(CreateSchemaCommand.class)))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND,
            "Project not found: " + request.projectId())));

    webTestClient.post()
        .uri(API_BASE_PATH + "/schemas")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason").isEqualTo(ProjectErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("스키마 조회 API 문서화")
  void getSchema() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    Schema schema = new Schema(
        schemaId,
        "06D6VZBWHSDJBBG0H7D156YZ98",
        "mariadb",
        "test_schema",
        "utf8mb4",
        "utf8mb4_general_ci");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 0L)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/schemas/{schemaId}", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.currentRevision").isEqualTo(0)
        .consumeWith(document("schema-get",
            pathParameters(
                parameterWithName("schemaId")
                    .description("조회할 스키마 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id").description("스키마 ID"),
                fieldWithPath("projectId").description("프로젝트 ID"),
                fieldWithPath("dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("name").description("스키마 이름"),
                fieldWithPath("charset").description("문자셋"),
                fieldWithPath("collation").description("콜레이션"),
                fieldWithPath("currentRevision").description("현재 schema revision"))));
  }

  @Test
  @DisplayName("프로젝트별 스키마 목록 조회 API 문서화")
  void getSchemasByProjectId() throws Exception {
    String projectId = "06D6VZBWHSDJBBG0H7D156YZ98";

    Schema schema1 = new Schema(
        "06D6W1GAHD51T5NJPK29Q6BCR8",
        projectId,
        "mariadb",
        "test_schema_1",
        "utf8mb4",
        "utf8mb4_general_ci");

    Schema schema2 = new Schema(
        "06D6W1GAHD51T5NJPK29Q6BCR9",
        projectId,
        "mariadb",
        "test_schema_2",
        "utf8mb4",
        "utf8mb4_general_ci");

    given(getSchemasByProjectIdUseCase.getSchemasByProjectId(any(GetSchemasByProjectIdQuery.class)))
        .willReturn(Flux.just(schema1, schema2));

    webTestClient.get()
        .uri(API_BASE_PATH + "/projects/{projectId}/schemas", projectId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("schema-list-by-project",
            pathParameters(
                parameterWithName("projectId")
                    .description("프로젝트 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("[]").description("스키마 목록"),
                fieldWithPath("[].id").description("스키마 ID"),
                fieldWithPath("[].projectId").description("프로젝트 ID"),
                fieldWithPath("[].dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("[].name").description("스키마 이름"),
                fieldWithPath("[].charset").description("문자셋"),
                fieldWithPath("[].collation").description("콜레이션"))));
  }

  @Test
  @DisplayName("존재하지 않는 프로젝트의 스키마 목록 조회 시 404를 반환한다")
  void getSchemasByProjectIdReturnsNotFoundForMissingProject() {
    String projectId = "06D6VZBWHSDJBBG0H7D156YZ98";

    given(getSchemasByProjectIdUseCase.getSchemasByProjectId(any(GetSchemasByProjectIdQuery.class)))
        .willReturn(Flux.error(new DomainException(ProjectErrorCode.NOT_FOUND,
            "Project not found: " + projectId)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/projects/{projectId}/schemas", projectId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason").isEqualTo(ProjectErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("스키마 이름 변경 API 문서화")
  void changeSchemaName() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    ChangeSchemaNameRequest request = new ChangeSchemaNameRequest("new_schema_name");

    given(changeSchemaNameUseCase.changeSchemaName(any(ChangeSchemaNameCommand.class)))
        .willReturn(Mono.just(
            MutationResult.<Void>empty(null)
                .withOperation(committedOperation())));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/schemas/{schemaId}/name", schemaId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").isEqualTo(null)
        .jsonPath("$.operation.opId").isEqualTo(OP_ID)
        .consumeWith(document("schema-change-name",
            pathParameters(
                parameterWithName("schemaId")
                    .description("변경할 스키마 ID")),
            requestHeaders(
                headerWithName("Content-Type")
                    .description("요청 본문 타입 (application/json)"),
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            requestFields(
                fieldWithPath("newName").description("변경할 스키마 이름")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("data")
                    .type(JsonFieldType.NULL)
                    .description("응답 데이터 (없음)")
                    .optional(),
                fieldWithPath("affectedTableIds")
                    .type(JsonFieldType.ARRAY)
                    .description("영향받은 테이블 ID 목록"),
                fieldWithPath("operation")
                    .type(JsonFieldType.OBJECT)
                    .description("커밋된 ERD operation 메타데이터"),
                fieldWithPath("operation.opId")
                    .type(JsonFieldType.STRING)
                    .description("커밋된 operation ID"),
                fieldWithPath("operation.clientOperationId")
                    .type(JsonFieldType.STRING)
                    .description("클라이언트가 보낸 operation ID"),
                fieldWithPath("operation.committedRevision")
                    .type(JsonFieldType.NUMBER)
                    .description("커밋 후 schema revision"),
                fieldWithPath("operation.derivationKind")
                    .type(JsonFieldType.STRING)
                    .description("operation derivation kind"))));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("스키마 삭제 API 문서화")
  void deleteSchema() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    given(deleteSchemaUseCase.deleteSchema(any(DeleteSchemaCommand.class)))
        .willReturn(Mono.just(
            MutationResult.<Void>empty(null)
                .withOperation(committedOperation())));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/schemas/{schemaId}", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").isEqualTo(null)
        .jsonPath("$.operation.opId").isEqualTo(OP_ID)
        .consumeWith(document("schema-delete",
            pathParameters(
                parameterWithName("schemaId")
                    .description("삭제할 스키마 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("data")
                    .type(JsonFieldType.NULL)
                    .description("응답 데이터 (없음)")
                    .optional(),
                fieldWithPath("affectedTableIds")
                    .type(JsonFieldType.ARRAY)
                    .description("영향받은 테이블 ID 목록"),
                fieldWithPath("operation")
                    .type(JsonFieldType.OBJECT)
                    .description("커밋된 ERD operation 메타데이터"),
                fieldWithPath("operation.opId")
                    .type(JsonFieldType.STRING)
                    .description("커밋된 operation ID"),
                fieldWithPath("operation.clientOperationId")
                    .type(JsonFieldType.STRING)
                    .description("클라이언트가 보낸 operation ID"),
                fieldWithPath("operation.committedRevision")
                    .type(JsonFieldType.NUMBER)
                    .description("커밋 후 schema revision"),
                fieldWithPath("operation.derivationKind")
                    .type(JsonFieldType.STRING)
                    .description("operation derivation kind"))));
  }

}
