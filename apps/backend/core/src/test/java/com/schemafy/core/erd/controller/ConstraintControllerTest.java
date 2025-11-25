package com.schemafy.core.erd.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.service.ConstraintService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("ConstraintController RestDocs 테스트")
@WithMockUser(roles = "EDITOR")
class ConstraintControllerTest {

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ConstraintService constraintService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("제약조건 생성 API 문서화")
    void createConstraint() throws Exception {
        Validation.CreateConstraintRequest.Builder builder = Validation.CreateConstraintRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6VZBWHSDJBBG0H7D156YZ98",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null,
                                        "tables": [
                                            {
                                                "id": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "columns": [
                                                    {
                                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "uid",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "autoIncrement": false
                                                    },
                                                    {
                                                        "id": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "visit_count",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "방문 횟수",
                                                        "autoIncrement": false
                                                    }
                                                ],
                                                "indexes": [],
                                                "constraints": [],
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "constraint": {
                                "id": "06D5XSF8RRRKMCHVNX68TDX1K4",
                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                "name": "PK",
                                "type": "PRIMARY_KEY",
                                "columns": [
                                    {
                                        "id": "06D5XST4N38Z9QANKMEDMCXAYG",
                                        "constraintId": "06D5XSF8RRRKMCHVNX68TDX1K4",
                                        "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                        "seqNo": 1
                                    }
                                ]
                            }
                        }
                        """,
                        builder);
        Validation.CreateConstraintRequest request = builder.build();

        AffectedMappingResponse mockResponse = new AffectedMappingResponse(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of("06D6W8HDY79QFZX39RMX62KSX4",
                        Map.of("06D5XSF8RRRKMCHVNX68TDX1K4",
                                "06D6WWYRQCEXN1ACZ4ZJ12DFTG")),
                Map.of("06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                        Map.of("06D5XST4N38Z9QANKMEDMCXAYG",
                                "06D5XST4N38Z9QANKMEDMCXAYG")),
                Collections.emptyMap(),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        when(constraintService.createConstraint(
                any(Validation.CreateConstraintRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri(API_BASE_PATH + "/constraints")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.constraints['06D6W8HDY79QFZX39RMX62KSX4']['06D5XSF8RRRKMCHVNX68TDX1K4']")
                .isEqualTo("06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .jsonPath(
                        "$.result.constraintColumns['06D6WWYRQCEXN1ACZ4ZJ12DFTG']['06D5XST4N38Z9QANKMEDMCXAYG']")
                .isEqualTo("06D5XST4N38Z9QANKMEDMCXAYG")
                .consumeWith(document("constraint-create",
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
                        relaxedResponseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("응답 데이터"),
                                subsectionWithPath("result.schemas")
                                        .description(
                                                "스키마 ID 매핑 (FE ID -> BE ID)"),
                                subsectionWithPath("result.tables")
                                        .description(
                                                "테이블 ID 매핑 (FE ID -> BE ID)"),
                                subsectionWithPath("result.columns")
                                        .description(
                                                "컬럼 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.indexes")
                                        .description(
                                                "인덱스 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.indexColumns")
                                        .description(
                                                "인덱스 컬럼 ID 매핑 (Index BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.constraints")
                                        .description(
                                                "제약조건 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.constraintColumns")
                                        .description(
                                                "제약조건 컬럼 ID 매핑 (Constraint BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.relationships")
                                        .description(
                                                "관계 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.relationshipColumns")
                                        .description(
                                                "관계 컬럼 ID 매핑 (Relationship BE ID -> { FE ID -> BE ID })"),
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
    @DisplayName("제약조건 단건 조회 API 문서화")
    void getConstraint() throws Exception {
        String mockResponseJson = """
                {
                    "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                    "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "PK",
                    "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                    "columns": [
                        {
                            "id": "06D6WWYRTER122SPQBVSX4T3MR",
                            "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "seqNo": 1
                        }
                    ]
                }
                """;

        when(constraintService.getConstraint("06D6WWYRQCEXN1ACZ4ZJ12DFTG"))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        ConstraintResponse.class)));

        webTestClient.get()
                .uri(API_BASE_PATH + "/constraints/{constraintId}",
                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .jsonPath("$.result.name").isEqualTo("PK")
                .consumeWith(document("constraint-get",
                        pathParameters(
                                parameterWithName("constraintId")
                                        .description("조회할 제약조건의 ID")),
                        requestHeaders(
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("제약조건 정보"),
                                fieldWithPath("result.id")
                                        .description("제약조건 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("제약조건 이름"),
                                fieldWithPath("result.kind").description(
                                        "제약조건 종류 (PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK)"),
                                fieldWithPath("result.columns")
                                        .description("제약조건 컬럼 목록"),
                                fieldWithPath("result.columns[].id")
                                        .description("제약조건 컬럼 ID"),
                                fieldWithPath("result.columns[].constraintId")
                                        .description("제약조건 ID"),
                                fieldWithPath("result.columns[].columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.columns[].seqNo")
                                        .description("순서 번호"))));
    }

    @Test
    @DisplayName("테이블별 제약조건 목록 조회 API 문서화")
    void getConstraintsByTableId() throws Exception {
        String mockResponseJson = """
                [
                    {
                        "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                        "name": "PK",
                        "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                        "columns": [
                            {
                                "id": "06D6WWYRTER122SPQBVSX4T3MR",
                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                "seqNo": 1
                            }
                        ]
                    }
                ]
                """;

        when(constraintService
                .getConstraintsByTableId("06D6W8HDY79QFZX39RMX62KSX4"))
                .thenReturn(Flux
                        .fromIterable(objectMapper.readValue(mockResponseJson,
                                new TypeReference<List<ConstraintResponse>>() {
                                })));

        webTestClient.get()
                .uri(API_BASE_PATH + "/constraints/table/{tableId}",
                        "06D6W8HDY79QFZX39RMX62KSX4")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .consumeWith(document("constraint-get-by-table",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
                        requestHeaders(
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("제약조건 목록"),
                                fieldWithPath("result[].id")
                                        .description("제약조건 ID"),
                                fieldWithPath("result[].tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result[].name")
                                        .description("제약조건 이름"),
                                fieldWithPath("result[].kind")
                                        .description("제약조건 종류"),
                                fieldWithPath("result[].columns")
                                        .description("제약조건 컬럼 목록"),
                                fieldWithPath("result[].columns[].id")
                                        .description("제약조건 컬럼 ID"),
                                fieldWithPath("result[].columns[].constraintId")
                                        .description("제약조건 ID"),
                                fieldWithPath("result[].columns[].columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result[].columns[].seqNo")
                                        .description("순서 번호"))));
    }

    @Test
    @DisplayName("제약조건 이름 변경 API 문서화")
    void updateConstraintName() throws Exception {
        Validation.ChangeConstraintNameRequest.Builder builder = Validation.ChangeConstraintNameRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6VZBWHSDJBBG0H7D156YZ98",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null,
                                        "tables": [
                                            {
                                                "id": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "columns": [
                                                    {
                                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "uid",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "autoIncrement": false
                                                    },
                                                    {
                                                        "id": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "visit_count",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "방문 횟수",
                                                        "autoIncrement": false
                                                    }
                                                ],
                                                "indexes": [],
                                                "constraints": [
                                                    {
                                                        "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "PK",
                                                        "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WWYRTER122SPQBVSX4T3MR",
                                                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
                                                            }
                                                        ]
                                                    }
                                                ],
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                            "newName": "PK_users"
                        }
                        """,
                        builder);
        Validation.ChangeConstraintNameRequest request = builder.build();

        String mockResponseJson = """
                {
                    "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                    "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "PK_users",
                    "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                    "columns": []
                }
                """;

        when(constraintService.updateConstraintName(
                any(Validation.ChangeConstraintNameRequest.class)))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        ConstraintResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH + "/constraints/{constraintId}/name",
                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .jsonPath("$.result.name").isEqualTo("PK_users")
                .consumeWith(document("constraint-update-name",
                        pathParameters(
                                parameterWithName("constraintId")
                                        .description("제약조건 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 제약조건 정보"),
                                fieldWithPath("result.id")
                                        .description("제약조건 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 제약조건 이름"),
                                fieldWithPath("result.kind")
                                        .description("제약조건 종류"),
                                fieldWithPath("result.columns")
                                        .description("제약조건 컬럼 목록"))));
    }

    @Test
    @DisplayName("제약조건에 컬럼 추가 API 문서화")
    void addColumnToConstraint() throws Exception {
        Validation.AddColumnToConstraintRequest.Builder builder = Validation.AddColumnToConstraintRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6VZBWHSDJBBG0H7D156YZ98",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null,
                                        "tables": [
                                            {
                                                "id": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "columns": [
                                                    {
                                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "uid",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "autoIncrement": false
                                                    },
                                                    {
                                                        "id": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "visit_count",
                                                        "ordinalPosition": 2,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "방문 횟수",
                                                        "autoIncrement": false
                                                    }
                                                ],
                                                "indexes": [],
                                                "constraints": [
                                                    {
                                                        "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "PK_users",
                                                        "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WWYRTER122SPQBVSX4T3MR",
                                                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
                                                            }
                                                        ]
                                                    }
                                                ],
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                            "constraintColumn": {
                                "id": "06D6X1234567890ABCDEFGHIJK",
                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                "seqNo": 2
                            }
                        }
                        """,
                        builder);
        Validation.AddColumnToConstraintRequest request = builder.build();

        when(constraintService.addColumnToConstraint(
                any(Validation.AddColumnToConstraintRequest.class)))
                .thenReturn(Mono.just(new AffectedMappingResponse(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Map.of("06D6W8HDY79QFZX39RMX62KSX4",
                                Map.of("06D5XSF8RRRKMCHVNX68TDX1K4",
                                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG")),
                        Map.of("06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                Map.of("06D6X1234567890ABCDEFGHIJK",
                                        "06D6X2345678901BCDEFGHIJKL")),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        AffectedMappingResponse.PropagatedEntities.empty())));

        webTestClient.post()
                .uri(API_BASE_PATH + "/constraints/{constraintId}/columns",
                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.constraints['06D6W8HDY79QFZX39RMX62KSX4']['06D5XSF8RRRKMCHVNX68TDX1K4']")
                .isEqualTo("06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .jsonPath(
                        "$.result.constraintColumns['06D6WWYRQCEXN1ACZ4ZJ12DFTG']['06D6X1234567890ABCDEFGHIJK']")
                .isEqualTo("06D6X2345678901BCDEFGHIJKL")
                .consumeWith(document("constraint-add-column",
                        pathParameters(
                                parameterWithName("constraintId")
                                        .description("제약조건 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        relaxedResponseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("응답 데이터"),
                                subsectionWithPath("result.schemas")
                                        .description(
                                                "스키마 ID 매핑 (FE ID -> BE ID)"),
                                subsectionWithPath("result.tables")
                                        .description(
                                                "테이블 ID 매핑 (FE ID -> BE ID)"),
                                subsectionWithPath("result.columns")
                                        .description(
                                                "컬럼 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.indexes")
                                        .description(
                                                "인덱스 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.indexColumns")
                                        .description(
                                                "인덱스 컬럼 ID 매핑 (Index BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.constraints")
                                        .description(
                                                "제약조건 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.constraintColumns")
                                        .description(
                                                "제약조건 컬럼 ID 매핑 (Constraint BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.relationships")
                                        .description(
                                                "관계 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.relationshipColumns")
                                        .description(
                                                "관계 컬럼 ID 매핑 (Relationship BE ID -> { FE ID -> BE ID })"),
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
    @DisplayName("제약조건에서 컬럼 제거 API 문서화")
    void removeColumnFromConstraint() throws Exception {
        Validation.RemoveColumnFromConstraintRequest.Builder builder = Validation.RemoveColumnFromConstraintRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6VZBWHSDJBBG0H7D156YZ98",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null,
                                        "tables": [
                                            {
                                                "id": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "columns": [
                                                    {
                                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "uid",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "autoIncrement": false
                                                    },
                                                    {
                                                        "id": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "visit_count",
                                                        "ordinalPosition": 2,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "방문 횟수",
                                                        "autoIncrement": false
                                                    }
                                                ],
                                                "indexes": [],
                                                "constraints": [
                                                    {
                                                        "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "PK_users",
                                                        "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WWYRTER122SPQBVSX4T3MR",
                                                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
                                                            },
                                                            {
                                                                "id": "06D6X2345678901BCDEFGHIJKL",
                                                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                                "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                                "seqNo": 2
                                                            }
                                                        ]
                                                    }
                                                ],
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                            "constraintColumnId": "06D6X2345678901BCDEFGHIJKL"
                        }
                        """,
                        builder);
        Validation.RemoveColumnFromConstraintRequest request = builder.build();

        when(constraintService.removeColumnFromConstraint(
                any(Validation.RemoveColumnFromConstraintRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH
                        + "/constraints/{constraintId}/columns/{columnId}",
                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                        "06D6X2345678901BCDEFGHIJKL")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("constraint-remove-column",
                        pathParameters(
                                parameterWithName("constraintId")
                                        .description("제약조건 ID"),
                                parameterWithName("columnId")
                                        .description("제약조건 컬럼 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"))));
    }

    @Test
    @DisplayName("제약조건 삭제 API 문서화")
    void deleteConstraint() throws Exception {
        Validation.DeleteConstraintRequest.Builder builder = Validation.DeleteConstraintRequest
                .newBuilder();
        JsonFormat.parser()
                .ignoringUnknownFields()
                .merge("""
                        {
                            "database": {
                                "id": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                "schemas": [
                                    {
                                        "id": "06D6VZBWHSDJBBG0H7D156YZ98",
                                        "projectId": "06D4K6TTEWXW8VQR8EZXDPWP3C",
                                        "dbVendorId": "MYSQL",
                                        "name": "test",
                                        "charset": "utf8mb4",
                                        "collation": "utf8mb4_unicode_ci",
                                        "vendorOption": "",
                                        "canvasViewport": null,
                                        "tables": [
                                            {
                                                "id": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "columns": [
                                                    {
                                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "uid",
                                                        "ordinalPosition": 1,
                                                        "dataType": "INTEGER",
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "autoIncrement": false
                                                    }
                                                ],
                                                "indexes": [],
                                                "constraints": [
                                                    {
                                                        "id": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "PK_users",
                                                        "kind": "CONSTRAINT_KIND_UNSPECIFIED",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WWYRTER122SPQBVSX4T3MR",
                                                                "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
                                                            }
                                                        ]
                                                    }
                                                ],
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "constraintId": "06D6WWYRQCEXN1ACZ4ZJ12DFTG"
                        }
                        """,
                        builder);
        Validation.DeleteConstraintRequest request = builder.build();

        when(constraintService.deleteConstraint(
                any(Validation.DeleteConstraintRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/constraints/{constraintId}",
                        "06D6WWYRQCEXN1ACZ4ZJ12DFTG")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("constraint-delete",
                        pathParameters(
                                parameterWithName("constraintId")
                                        .description("삭제할 제약조건의 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"))));
    }

}
