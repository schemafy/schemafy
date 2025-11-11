package com.schemafy.core.erd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.SchemaDetailResponse;
import com.schemafy.core.erd.controller.dto.response.SchemaResponse;
import com.schemafy.core.erd.service.SchemaService;

import reactor.core.publisher.Mono;
import validation.Validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("SchemaController 통합 테스트")
class SchemaControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SchemaService schemaService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("스키마 생성 API 문서화")
    void createSchema_success() throws Exception {
        // given
        Validation.CreateSchemaRequest.Builder builder = Validation.CreateSchemaRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C"
                            },
                            "schema": {
                                "id": "06D4K6XMCJ1NWKNV13HFZ8CVC0",
                                "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "dbVendorId": "MYSQL",
                                "vendorOption": "",
                                "name": "test",
                                "charset": "utf8mb4",
                                "collation": "utf8mb4_unicode_ci"
                            }
                        }
                        """, builder);
        Validation.CreateSchemaRequest request = builder.build();

        AffectedMappingResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "schemas": {
                                                    "06D4K6XMCJ1NWKNV13HFZ8CVC0": "06D6JVZ1NJ81RKSJMT8JWKNJNG"
                                                },
                                                "tables": {},
                                                "columns": {},
                                                "indexes": {},
                                                "indexColumns": {},
                                                "constraints": {},
                                                "constraintColumns": {},
                                                "relationships": {},
                                                "relationshipColumns": {},
                                                "propagated": {
                                                    "columns": [],
                                                    "constraintColumns": [],
                                                    "indexColumns": []
                                                }
                                            }
                                        }
                                        """)
                        .get("result"),
                AffectedMappingResponse.class);

        given(schemaService
                .createSchema(any(Validation.CreateSchemaRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.post()
                .uri(API_BASE_PATH + "/schemas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.schemas.06D4K6XMCJ1NWKNV13HFZ8CVC0")
                .isEqualTo("06D6JVZ1NJ81RKSJMT8JWKNJNG")
                .consumeWith(document("schema-create",
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description(
                                                "요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.schemas")
                                        .description(
                                                "스키마 ID 매핑 (FE ID -> BE ID)"),
                                fieldWithPath(
                                        "result.schemas.06D4K6XMCJ1NWKNV13HFZ8CVC0")
                                        .description(
                                                "백엔드에서 생성된 스키마 ID"),
                                fieldWithPath("result.tables")
                                        .description("테이블 ID 매핑"),
                                fieldWithPath("result.columns")
                                        .description("컬럼 ID 매핑"),
                                fieldWithPath("result.indexes")
                                        .description("인덱스 ID 매핑"),
                                fieldWithPath("result.indexColumns")
                                        .description("인덱스 컬럼 ID 매핑"),
                                fieldWithPath("result.constraints")
                                        .description("제약조건 ID 매핑"),
                                fieldWithPath("result.constraintColumns")
                                        .description("제약조건 컬럼 ID 매핑"),
                                fieldWithPath("result.relationships")
                                        .description("관계 ID 매핑"),
                                fieldWithPath("result.relationshipColumns")
                                        .description("관계 컬럼 ID 매핑"),
                                fieldWithPath("result.propagated")
                                        .description("전파된 엔티티 정보"),
                                fieldWithPath("result.propagated.columns")
                                        .description("전파된 컬럼 목록"),
                                fieldWithPath(
                                        "result.propagated.constraintColumns")
                                        .description("전파된 제약조건 컬럼 목록"),
                                fieldWithPath("result.propagated.indexColumns")
                                        .description("전파된 인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("스키마 단건 조회 API 문서화")
    void getSchema_success_returns_ok() throws Exception {
        String id = "06D6JVZ1NJ81RKSJMT8JWKNJNG";

        SchemaDetailResponse detailResponse = objectMapper
                .treeToValue(
                        objectMapper
                                .readTree(
                                        """
                                                    {
                                                        "success": true,
                                                        "result": {
                                                            "id": "06D6JVZ1NJ81RKSJMT8JWKNJNG",
                                                            "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                                            "dbVendorId": "MYSQL",
                                                            "name": "test-schema",
                                                            "charset": "utf8mb4",
                                                            "collation": "utf8mb4_unicode_ci",
                                                            "vendorOption": "",
                                                            "canvasViewport": null,
                                                            "createdAt": "2025-11-09T16:25:31Z",
                                                            "updatedAt": "2025-11-10T13:16:40Z",
                                                            "deletedAt": null,
                                                            "tables": []
                                                        }
                                                    }
                                                """)
                                .get("result"),
                        SchemaDetailResponse.class);

        given(schemaService.getSchema(id))
                .willReturn(Mono.just(detailResponse));

        // when & then
        webTestClient.get()
                .uri(API_BASE_PATH + "/schemas/{schemaId}", id)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6JVZ1NJ81RKSJMT8JWKNJNG")
                .jsonPath("$.result.projectId")
                .isEqualTo("06D4K6TTEWXW8VQR8EZXDPWP3C")
                .jsonPath("$.result.dbVendorId").isEqualTo("MYSQL")
                .jsonPath("$.result.name").isEqualTo("test-schema")
                .jsonPath("$.result.charset").isEqualTo("utf8mb4")
                .jsonPath("$.result.collation").isEqualTo("utf8mb4_unicode_ci")
                .consumeWith(document("schema-get",
                        pathParameters(
                                parameterWithName("schemaId")
                                        .description("조회할 스키마의 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("스키마 정보"),
                                fieldWithPath("result.id")
                                        .description("스키마 ID"),
                                fieldWithPath("result.projectId")
                                        .description("프로젝트 ID"),
                                fieldWithPath("result.dbVendorId")
                                        .description("데이터베이스 벤더 ID"),
                                fieldWithPath("result.name")
                                        .description("스키마 이름"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.vendorOption")
                                        .description("벤더별 옵션"),
                                fieldWithPath("result.canvasViewport")
                                        .description("캔버스 뷰포트 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시 (Soft Delete)"),
                                fieldWithPath("result.tables")
                                        .description("테이블 목록"))));
    }

    @Test
    @DisplayName("스키마 이름 변경 API 문서화")
    void updateSchemaName_success_returns_ok() throws Exception {
        // given
        Validation.ChangeSchemaNameRequest.Builder builder = Validation.ChangeSchemaNameRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6JVZ1NJ81RKSJMT8JWKNJNG",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test-schema",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null
                                    }
                                ]
                            },
                            "schemaId": "06D6JVZ1NJ81RKSJMT8JWKNJNG",
                            "newName": "test_schema"
                        }
                        """,
                        builder);
        Validation.ChangeSchemaNameRequest request = builder.build();

        SchemaResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6JVZ1NJ81RKSJMT8JWKNJNG",
                                                "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                                "dbVendorId": "MYSQL",
                                                "name": "test_schema",
                                                "charset": "utf8mb4",
                                                "collation": "utf8mb4_unicode_ci",
                                                "vendorOption": "",
                                                "canvasViewport": null,
                                                "createdAt": "2025-11-09T16:25:31Z",
                                                "updatedAt": "2025-11-10T13:26:24.595812Z",
                                                "deletedAt": null
                                            }
                                        }
                                        """)
                        .get("result"),
                SchemaResponse.class);

        given(schemaService.updateSchemaName(request))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.put()
                .uri(API_BASE_PATH + "/schemas/{schemaId}/name",
                        "06D6JVZ1NJ81RKSJMT8JWKNJNG")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6JVZ1NJ81RKSJMT8JWKNJNG")
                .jsonPath("$.result.projectId")
                .isEqualTo("06D4K6TTEWXW8VQR8EZXDPWP3C")
                .jsonPath("$.result.dbVendorId").isEqualTo("MYSQL")
                .jsonPath("$.result.name").isEqualTo("test_schema")
                .jsonPath("$.result.charset").isEqualTo("utf8mb4")
                .jsonPath("$.result.collation").isEqualTo("utf8mb4_unicode_ci")
                .consumeWith(document("schema-update-name",
                        pathParameters(
                                parameterWithName("schemaId")
                                        .description("스키마 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description(
                                                "요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 스키마 정보"),
                                fieldWithPath("result.id")
                                        .description("스키마 ID"),
                                fieldWithPath("result.projectId")
                                        .description("프로젝트 ID"),
                                fieldWithPath("result.dbVendorId")
                                        .description("데이터베이스 벤더 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 스키마 이름"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.vendorOption")
                                        .description("벤더별 옵션"),
                                fieldWithPath("result.canvasViewport")
                                        .description("캔버스 뷰포트 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("스키마 이름 변경: 경로와 바디의 schemaId 불일치 시 400")
    void updateSchemaName_mismatch_returns_bad_request() throws Exception {
        String pathId = "path-id";
        Validation.ChangeSchemaNameRequest request = Validation.ChangeSchemaNameRequest
                .newBuilder()
                .setSchemaId("body-id")
                .setNewName("new-name")
                .build();

        webTestClient.put()
                .uri(API_BASE_PATH + "/schemas/{schemaId}/name", pathId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("스키마 삭제 API 문서화")
    void deleteSchema_success() throws Exception {
        // given
        Validation.DeleteSchemaRequest.Builder builder = Validation.DeleteSchemaRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6JVZ1NJ81RKSJMT8JWKNJNG",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test_schema",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null
                                    }
                                ]
                            },
                            "schemaId": "06D6JVZ1NJ81RKSJMT8JWKNJNG"
                        }
                        """,
                        builder);
        Validation.DeleteSchemaRequest request = builder.build();

        given(schemaService.deleteSchema(request)).willReturn(Mono.empty());

        // when & then
        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/schemas/{schemaId}",
                        "06D6JVZ1NJ81RKSJMT8JWKNJNG")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("schema-delete",
                        pathParameters(
                                parameterWithName("schemaId")
                                        .description("삭제할 스키마의 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description(
                                                "요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("응답 데이터 (null)"))));
    }

    @Test
    @DisplayName("스키마 삭제: 경로와 바디의 schemaId 불일치 시 400")
    void deleteSchema_mismatch_returns_bad_request() throws Exception {
        String pathId = "path-id";
        Validation.DeleteSchemaRequest request = Validation.DeleteSchemaRequest
                .newBuilder()
                .setSchemaId("body-id")
                .build();

        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/schemas/{schemaId}", pathId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

}
