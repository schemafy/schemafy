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
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.service.ColumnService;

import reactor.core.publisher.Flux;
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
@DisplayName("ColumnController 통합 테스트")
class ColumnControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ColumnService columnService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("컬럼 생성 API 문서화")
    void createColumn() throws Exception {
        // given
        Validation.CreateColumnRequest.Builder builder = Validation.CreateColumnRequest
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
                                                "columns": [],
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
                            "column": {
                                "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                "name": "user_id",
                                "dataType": "BIGINT",
                                "ordinalPosition": 1,
                                "lengthScale": "20",
                                "charset": "utf8mb4",
                                "collation": "utf8mb4_unicode_ci",
                                "comment": "사용자 ID",
                                "isAutoIncrement": false
                            }
                        }
                        """,
                        builder);
        Validation.CreateColumnRequest request = builder.build();

        AffectedMappingResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "schemas": {},
                                                "tables": {},
                                                "columns": {
                                                    "06D6W8HDY79QFZX39RMX62KSX4": {
                                                        "01ARZ3NDEKTSV4RRFFQ69G5FAV": "06D6W90RSE1VPFRMM4XPKYGM9M"
                                                    }
                                                },
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

        given(columnService
                .createColumn(any(Validation.CreateColumnRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.post()
                .uri(API_BASE_PATH + "/columns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.columns.06D6W8HDY79QFZX39RMX62KSX4.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .consumeWith(document("column-create",
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
                                        .description("스키마 ID 매핑"),
                                fieldWithPath("result.tables")
                                        .description("테이블 ID 매핑"),
                                fieldWithPath("result.columns")
                                        .description(
                                                "컬럼 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath(
                                        "result.columns.06D6W8HDY79QFZX39RMX62KSX4")
                                        .description("테이블별 컬럼 ID 매핑"),
                                fieldWithPath(
                                        "result.columns.06D6W8HDY79QFZX39RMX62KSX4.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                        .description(
                                                "백엔드에서 생성된 컬럼 ID (06D6W90RSE1VPFRMM4XPKYGM9M)"),
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
    @DisplayName("컬럼 단건 조회 API 문서화")
    void getColumn() throws Exception {
        // given
        String columnId = "06D6W90RSE1VPFRMM4XPKYGM9M";

        ColumnResponse columnResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "name": "user_id",
                                                "dataType": "BIGINT",
                                                "ordinalPosition": 1,
                                                "lengthScale": "20",
                                                "isAutoIncrement": false,
                                                "charset": "utf8mb4",
                                                "collation": "utf8mb4_unicode_ci",
                                                "comment": "사용자 ID"
                                            }
                                        }
                                        """)
                        .get("result"),
                ColumnResponse.class);

        given(columnService.getColumn(columnId))
                .willReturn(Mono.just(columnResponse));

        // when & then
        webTestClient.get()
                .uri(API_BASE_PATH + "/columns/{columnId}", columnId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .jsonPath("$.result.tableId")
                .isEqualTo("06D6W8HDY79QFZX39RMX62KSX4")
                .jsonPath("$.result.name").isEqualTo("user_id")
                .jsonPath("$.result.dataType").isEqualTo("BIGINT")
                .jsonPath("$.result.ordinalPosition").isEqualTo(1)
                .consumeWith(document("column-get",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("조회할 컬럼의 ID")),
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
                                fieldWithPath("result").description("컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("컬럼 이름"),
                                fieldWithPath("result.dataType")
                                        .description("데이터 타입"),
                                fieldWithPath("result.lengthScale")
                                        .description("길이/스케일"),
                                fieldWithPath("result.isAutoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"))));
    }

    @Test
    @DisplayName("테이블별 컬럼 목록 조회 API 문서화")
    void getColumnsByTableId() throws Exception {
        // given
        String tableId = "06D6W8HDY79QFZX39RMX62KSX4";

        ColumnResponse columnResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                            "name": "user_id",
                                            "dataType": "BIGINT",
                                            "ordinalPosition": 1,
                                            "lengthScale": "20",
                                            "isAutoIncrement": false,
                                            "charset": "utf8mb4",
                                            "collation": "utf8mb4_unicode_ci",
                                            "comment": "사용자 ID"
                                        }
                                        """),
                ColumnResponse.class);

        given(columnService.getColumnsByTableId(tableId))
                .willReturn(Flux.just(columnResponse));

        // when & then
        webTestClient.get()
                .uri(API_BASE_PATH + "/columns/table/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .jsonPath("$.result[0].id")
                .isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .jsonPath("$.result[0].tableId")
                .isEqualTo("06D6W8HDY79QFZX39RMX62KSX4")
                .jsonPath("$.result[0].name").isEqualTo("user_id")
                .jsonPath("$.result[0].dataType").isEqualTo("BIGINT")
                .consumeWith(document("column-list-by-table",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
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
                                fieldWithPath("result").description("컬럼 목록"),
                                fieldWithPath("result[].id")
                                        .description("컬럼 ID"),
                                fieldWithPath("result[].tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result[].name")
                                        .description("컬럼 이름"),
                                fieldWithPath("result[].dataType")
                                        .description("데이터 타입"),
                                fieldWithPath("result[].lengthScale")
                                        .description("길이/스케일"),
                                fieldWithPath("result[].isAutoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result[].charset")
                                        .description("문자 집합"),
                                fieldWithPath("result[].collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result[].comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result[].ordinalPosition")
                                        .description("컬럼 위치"))));
    }

    @Test
    @DisplayName("컬럼 이름 변경 API 문서화")
    void updateColumnName() throws Exception {
        // given
        Validation.ChangeColumnNameRequest.Builder builder = Validation.ChangeColumnNameRequest
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
                                                        "name": "user_id",
                                                        "dataType": "BIGINT",
                                                        "ordinalPosition": 1,
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "isAutoIncrement": false
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
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "newName": "uid"
                        }
                        """,
                        builder);
        Validation.ChangeColumnNameRequest request = builder.build();

        ColumnResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "name": "uid",
                                                "dataType": "BIGINT",
                                                "ordinalPosition": 1,
                                                "lengthScale": "20",
                                                "isAutoIncrement": false,
                                                "charset": "utf8mb4",
                                                "collation": "utf8mb4_unicode_ci",
                                                "comment": "사용자 ID"
                                            }
                                        }
                                        """)
                        .get("result"),
                ColumnResponse.class);

        given(columnService.updateColumnName(request))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/name",
                        "06D6W90RSE1VPFRMM4XPKYGM9M")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .jsonPath("$.result.tableId")
                .isEqualTo("06D6W8HDY79QFZX39RMX62KSX4")
                .jsonPath("$.result.name").isEqualTo("uid")
                .jsonPath("$.result.dataType").isEqualTo("BIGINT")
                .consumeWith(document("column-update-name",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                        .description("수정된 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 컬럼 이름"),
                                fieldWithPath("result.dataType")
                                        .description("데이터 타입"),
                                fieldWithPath("result.lengthScale")
                                        .description("길이/스케일"),
                                fieldWithPath("result.isAutoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"))));
    }

    @Test
    @DisplayName("컬럼 타입 변경 API 문서화")
    void updateColumnType() throws Exception {
        // given
        Validation.ChangeColumnTypeRequest.Builder builder = Validation.ChangeColumnTypeRequest
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
                                                        "dataType": "BIGINT",
                                                        "ordinalPosition": 1,
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "isAutoIncrement": false
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
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "dataType": "INTEGER"
                        }
                        """,
                        builder);
        Validation.ChangeColumnTypeRequest request = builder.build();

        ColumnResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "name": "uid",
                                                "dataType": "INTEGER",
                                                "ordinalPosition": 1,
                                                "lengthScale": "20",
                                                "isAutoIncrement": false,
                                                "charset": "utf8mb4",
                                                "collation": "utf8mb4_unicode_ci",
                                                "comment": "사용자 ID"
                                            }
                                        }
                                        """)
                        .get("result"),
                ColumnResponse.class);

        given(columnService.updateColumnType(request))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/type",
                        "06D6W90RSE1VPFRMM4XPKYGM9M")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .jsonPath("$.result.name").isEqualTo("uid")
                .jsonPath("$.result.dataType").isEqualTo("INTEGER")
                .consumeWith(document("column-update-type",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                        .description("수정된 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("컬럼 이름"),
                                fieldWithPath("result.dataType")
                                        .description("변경된 데이터 타입"),
                                fieldWithPath("result.lengthScale")
                                        .description("길이/스케일"),
                                fieldWithPath("result.isAutoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"))));
    }

    @Test
    @DisplayName("컬럼 위치 변경 API 문서화")
    void updateColumnPosition() throws Exception {
        // given
        Validation.ChangeColumnPositionRequest.Builder builder = Validation.ChangeColumnPositionRequest
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
                                                        "dataType": "INTEGER",
                                                        "ordinalPosition": 1,
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "isAutoIncrement": false
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
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "newPosition": 1
                        }
                        """,
                        builder);
        Validation.ChangeColumnPositionRequest request = builder.build();

        ColumnResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                "name": "uid",
                                                "dataType": "INTEGER",
                                                "ordinalPosition": 1,
                                                "lengthScale": "20",
                                                "isAutoIncrement": false,
                                                "charset": "utf8mb4",
                                                "collation": "utf8mb4_unicode_ci",
                                                "comment": "사용자 ID"
                                            }
                                        }
                                        """)
                        .get("result"),
                ColumnResponse.class);

        given(columnService.updateColumnPosition(request))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/position",
                        "06D6W90RSE1VPFRMM4XPKYGM9M")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .jsonPath("$.result.name").isEqualTo("uid")
                .jsonPath("$.result.ordinalPosition").isEqualTo(1)
                .consumeWith(document("column-update-position",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                        .description("수정된 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("컬럼 이름"),
                                fieldWithPath("result.dataType")
                                        .description("데이터 타입"),
                                fieldWithPath("result.lengthScale")
                                        .description("길이/스케일"),
                                fieldWithPath("result.isAutoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("변경된 컬럼 위치"))));
    }

    @Test
    @DisplayName("컬럼 삭제 API 문서화")
    void deleteColumn() throws Exception {
        // given
        Validation.DeleteColumnRequest.Builder builder = Validation.DeleteColumnRequest
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
                                                        "dataType": "INTEGER",
                                                        "ordinalPosition": 1,
                                                        "lengthScale": "20",
                                                        "charset": "utf8mb4",
                                                        "collation": "utf8mb4_unicode_ci",
                                                        "comment": "사용자 ID",
                                                        "isAutoIncrement": false
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
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M"
                        }
                        """,
                        builder);
        Validation.DeleteColumnRequest request = builder.build();

        given(columnService.deleteColumn(request)).willReturn(Mono.empty());

        // when & then
        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/columns/{columnId}",
                        "06D6W90RSE1VPFRMM4XPKYGM9M")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("column-delete",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("삭제할 컬럼의 ID")),
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

}
