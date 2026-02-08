package com.schemafy.core.erd.controller;

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
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.erd.controller.dto.request.ChangeSchemaNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateSchemaRequest;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;

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
  private GetSchemaUseCase getSchemaUseCase;

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
        .willReturn(Mono.just(MutationResult.empty(result)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/schemas")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
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
                fieldWithPath("success").description("요청 성공 여부"),
                fieldWithPath("result").description("뮤테이션 결과"),
                fieldWithPath("result.data").description("생성된 스키마 정보").optional(),
                fieldWithPath("result.data.id").description("스키마 ID"),
                fieldWithPath("result.data.projectId").description("프로젝트 ID"),
                fieldWithPath("result.data.dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("result.data.name").description("스키마 이름"),
                fieldWithPath("result.data.charset").description("문자셋").optional(),
                fieldWithPath("result.data.collation").description("콜레이션").optional(),
                fieldWithPath("result.affectedTableIds").description("영향받은 테이블 ID 목록"),
                fieldWithPath("error")
                    .type(JsonFieldType.NULL)
                    .description("에러 정보 (성공 시 null)").optional())));
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

    given(getSchemaUseCase.getSchema(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(schema));

    webTestClient.get()
        .uri(API_BASE_PATH + "/schemas/{schemaId}", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
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
                fieldWithPath("success").description("요청 성공 여부"),
                fieldWithPath("result").description("스키마 정보"),
                fieldWithPath("result.id").description("스키마 ID"),
                fieldWithPath("result.projectId").description("프로젝트 ID"),
                fieldWithPath("result.dbVendorName").description("DB 벤더 이름"),
                fieldWithPath("result.name").description("스키마 이름"),
                fieldWithPath("result.charset").description("문자셋"),
                fieldWithPath("result.collation").description("콜레이션"))));
  }

  @Test
  @DisplayName("스키마 이름 변경 API 문서화")
  void changeSchemaName() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    ChangeSchemaNameRequest request = new ChangeSchemaNameRequest("new_schema_name");

    given(changeSchemaNameUseCase.changeSchemaName(any(ChangeSchemaNameCommand.class)))
        .willReturn(Mono.just(MutationResult.empty(null)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/schemas/{schemaId}/name", schemaId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
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
                fieldWithPath("success")
                    .type(JsonFieldType.BOOLEAN)
                    .description("요청 성공 여부"),
                fieldWithPath("result")
                    .type(JsonFieldType.OBJECT)
                    .description("뮤테이션 결과"),
                fieldWithPath("result.data")
                    .type(JsonFieldType.NULL)
                    .description("응답 데이터 (없음)")
                    .optional(),
                fieldWithPath("result.affectedTableIds")
                    .type(JsonFieldType.ARRAY)
                    .description("영향받은 테이블 ID 목록"),
                fieldWithPath("error")
                    .type(JsonFieldType.NULL)
                    .description("에러 정보 (성공 시 null)").optional())));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("스키마 삭제 API 문서화")
  void deleteSchema() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    given(deleteSchemaUseCase.deleteSchema(any(DeleteSchemaCommand.class)))
        .willReturn(Mono.just(MutationResult.empty(null)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/schemas/{schemaId}", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
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
                fieldWithPath("success")
                    .type(JsonFieldType.BOOLEAN)
                    .description("요청 성공 여부"),
                fieldWithPath("result")
                    .type(JsonFieldType.OBJECT)
                    .description("뮤테이션 결과"),
                fieldWithPath("result.data")
                    .type(JsonFieldType.NULL)
                    .description("응답 데이터 (없음)")
                    .optional(),
                fieldWithPath("result.affectedTableIds")
                    .type(JsonFieldType.ARRAY)
                    .description("영향받은 테이블 ID 목록"),
                fieldWithPath("error")
                    .type(JsonFieldType.NULL)
                    .description("에러 정보 (성공 시 null)").optional())));
  }

}
