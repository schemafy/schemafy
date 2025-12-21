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
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.erd.controller.dto.request.UpdateIndexColumnSortDirRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateIndexTypeRequest;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.service.IndexService;

import reactor.core.publisher.Mono;
import validation.Validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
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
@DisplayName("IndexController RestDocs 테스트")
@WithMockCustomUser(roles = "EDITOR")
class IndexControllerTest {

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private IndexService indexService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("인덱스 생성 API 문서화")
    void createIndex() throws Exception {
        Validation.CreateIndexRequest.Builder builder = Validation.CreateIndexRequest
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
                                                "relationships": []
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "index": {
                                "id": "06D5KGSC0HJ9CPPYGMGYDA2PAG",
                                "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                "name": "IDX",
                                "type": "BTREE",
                                "comment": "",
                                "columns": [
                                    {
                                        "id": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                        "indexId": "06D5KGSC0HJ9CPPYGMGYDA2PAG",
                                        "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                        "seqNo": 1,
                                        "sortDir": "ASC"
                                    }
                                ]
                            }
                        }
                        """,
                        builder);
        Validation.CreateIndexRequest request = builder.build();

        String mockResponseJson = """
                {
                    "schemas": {},
                    "tables": {},
                    "columns": {},
                                "indexes": {
                                    "06D6W8HDY79QFZX39RMX62KSX4": {
                            "06D5KGSC0HJ9CPPYGMGYDA2PAG": "06D6WNJS8XWZT41HWZ226ZS904"
                        }
                    },
                    "indexColumns": {
                        "06D6WNJS8XWZT41HWZ226ZS904": {
                            "06D6W90RSE1VPFRMM4XPKYGM9M": "06D6W90RSE1VPFRMM4XPKYGM9M"
                        }
                    },
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
                """;
        AffectedMappingResponse mockResponse = objectMapper.readValue(
                mockResponseJson, AffectedMappingResponse.class);

        when(indexService.createIndex(any(Validation.CreateIndexRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri(API_BASE_PATH + "/indexes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.indexes['06D6W8HDY79QFZX39RMX62KSX4']['06D5KGSC0HJ9CPPYGMGYDA2PAG']")
                .isEqualTo("06D6WNJS8XWZT41HWZ226ZS904")
                .jsonPath(
                        "$.result.indexColumns['06D6WNJS8XWZT41HWZ226ZS904']['06D6W90RSE1VPFRMM4XPKYGM9M']")
                .isEqualTo("06D6W90RSE1VPFRMM4XPKYGM9M")
                .consumeWith(document("index-create",
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description(
                                                "요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        relaxedRequestFields(
                                fieldWithPath("database.id")
                                        .description("데이터베이스 ID"),
                                fieldWithPath("schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("index.id")
                                        .description("인덱스 ID (FE ID)"),
                                fieldWithPath("index.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("index.name")
                                        .description("인덱스 이름"),
                                fieldWithPath("index.type")
                                        .description("인덱스 타입"),
                                fieldWithPath("index.columns")
                                        .description("인덱스 컬럼 목록"),
                                fieldWithPath("index.columns[].columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("index.columns[].seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("index.columns[].sortDir")
                                        .description("정렬 방향 (ASC/DESC)")),
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
    @DisplayName("인덱스 단건 조회 API 문서화")
    void getIndex() throws Exception {
        String mockResponseJson = """
                {
                    "id": "06D6WNJS8XWZT41HWZ226ZS904",
                    "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "IDX",
                    "type": "BTREE",
                    "comment": "",
                    "columns": [
                        {
                            "id": "06D6WNJSBMS6AWBM1ZD7EDJP4M",
                            "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                            "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "seqNo": 1,
                            "sortDir": "ASC"
                        }
                    ]
                }
                """;

        when(indexService.getIndex("06D6WNJS8XWZT41HWZ226ZS904"))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        IndexResponse.class)));

        webTestClient.get()
                .uri(API_BASE_PATH + "/indexes/{indexId}",
                        "06D6WNJS8XWZT41HWZ226ZS904")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WNJS8XWZT41HWZ226ZS904")
                .jsonPath("$.result.name").isEqualTo("IDX")
                .jsonPath("$.result.columns[0].id")
                .isEqualTo("06D6WNJSBMS6AWBM1ZD7EDJP4M")
                .consumeWith(document("index-get",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("조회할 인덱스의 ID")),
                        requestHeaders(
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("인덱스 정보"),
                                fieldWithPath("result.id")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("인덱스 이름"),
                                fieldWithPath("result.type").description(
                                        "인덱스 타입 (BTREE, HASH, FULLTEXT 등)"),
                                fieldWithPath("result.comment")
                                        .description("인덱스 설명"),
                                fieldWithPath("result.columns")
                                        .description("인덱스 컬럼 목록"),
                                fieldWithPath("result.columns[].id")
                                        .description("인덱스 컬럼 ID"),
                                fieldWithPath("result.columns[].indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.columns[].columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.columns[].seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("result.columns[].sortDir")
                                        .description("정렬 방향 (ASC/DESC)"))));
    }

    @Test
    @DisplayName("인덱스 이름 변경 API 문서화")
    void updateIndexName() throws Exception {
        Validation.ChangeIndexNameRequest.Builder builder = Validation.ChangeIndexNameRequest
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
                                                "relationships": [],
                                                "indexes": [
                                                    {
                                                        "id": "06D6WNJS8XWZT41HWZ226ZS904",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "IDX_UID",
                                                        "type": "BTREE",
                                                        "comment": "",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WNJSBMS6AWBM1ZD7EDJP4M",
                                                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1,
                                                                "sortDir": "ASC"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                            "newName": "IDX1"
                        }
                        """,
                        builder);
        Validation.ChangeIndexNameRequest request = builder.build();

        String mockResponseJson = """
                {
                    "id": "06D6WNJS8XWZT41HWZ226ZS904",
                    "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "IDX1",
                    "type": "BTREE",
                    "comment": "",
                    "columns": []
                }
                """;

        when(indexService
                .updateIndexName(any(Validation.ChangeIndexNameRequest.class)))
                .thenReturn(Mono
                        .just(objectMapper.readValue(mockResponseJson,
                                IndexResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH + "/indexes/{indexId}/name",
                        "06D6WNJS8XWZT41HWZ226ZS904")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WNJS8XWZT41HWZ226ZS904")
                .jsonPath("$.result.name").isEqualTo("IDX1")
                .consumeWith(document("index-update-name",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("database.id")
                                        .description("데이터베이스 ID"),
                                fieldWithPath("schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("indexId")
                                        .description("변경할 인덱스 ID"),
                                fieldWithPath("newName")
                                        .description("새 인덱스 이름")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 인덱스 정보"),
                                fieldWithPath("result.id")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 인덱스 이름"),
                                fieldWithPath("result.type")
                                        .description("인덱스 타입"),
                                fieldWithPath("result.comment")
                                        .description("인덱스 설명"),
                                fieldWithPath("result.columns")
                                        .description("인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("인덱스 타입 변경 API 문서화")
    void updateIndexType() throws Exception {
        String indexId = "06D6WNJS8XWZT41HWZ226ZS904";
        UpdateIndexTypeRequest request = new UpdateIndexTypeRequest("HASH");

        String mockResponseJson = """
                {
                    "id": "06D6WNJS8XWZT41HWZ226ZS904",
                    "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "IDX1",
                    "type": "HASH",
                    "comment": "",
                    "columns": []
                }
                """;

        when(indexService.updateIndexType(any(String.class),
                any(UpdateIndexTypeRequest.class)))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        IndexResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH + "/indexes/{indexId}/type", indexId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(indexId)
                .jsonPath("$.result.type").isEqualTo("HASH")
                .consumeWith(document("index-update-type",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("type")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "변경할 인덱스 타입 (BTREE, HASH, FULLTEXT, SPATIAL, OTHER)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 인덱스 정보"),
                                fieldWithPath("result.id")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("인덱스 이름"),
                                fieldWithPath("result.type")
                                        .description("변경된 인덱스 타입"),
                                fieldWithPath("result.comment")
                                        .description("인덱스 설명"),
                                fieldWithPath("result.columns")
                                        .description("인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("인덱스에 컬럼 추가 API 문서화")
    void addColumnToIndex() throws Exception {
        Validation.AddColumnToIndexRequest.Builder builder = Validation.AddColumnToIndexRequest
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
                                                "relationships": [],
                                                "indexes": [
                                                    {
                                                        "id": "06D6WNJS8XWZT41HWZ226ZS904",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "IDX1",
                                                        "type": "BTREE",
                                                        "comment": "",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WNJSBMS6AWBM1ZD7EDJP4M",
                                                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1,
                                                                "sortDir": "ASC"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                            "indexColumn": {
                                "id": "06D5KR6B9S11TWPG1PFNQYSD6C",
                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                "seqNo": 2,
                                "sortDir": "ASC"
                            }
                        }
                        """,
                        builder);
        Validation.AddColumnToIndexRequest request = builder.build();

        String mockResponseJson = """
                {
                    "id": "06D6WPTT2EJ7S1CJATWGEYPNS4",
                    "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                    "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                    "seqNo": 2,
                    "sortDir": "ASC"
                }
                """;

        when(indexService.addColumnToIndex(
                any(Validation.AddColumnToIndexRequest.class)))
                .thenReturn(Mono
                        .just(objectMapper.readValue(mockResponseJson,
                                IndexColumnResponse.class)));

        webTestClient.post()
                .uri(API_BASE_PATH + "/indexes/{indexId}/columns",
                        "06D6WNJS8XWZT41HWZ226ZS904")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WPTT2EJ7S1CJATWGEYPNS4")
                .jsonPath("$.result.indexId")
                .isEqualTo("06D6WNJS8XWZT41HWZ226ZS904")
                .jsonPath("$.result.seqNo").isEqualTo(2)
                .consumeWith(document("index-add-column",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("database.id")
                                        .description("데이터베이스 ID"),
                                fieldWithPath("schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("indexColumn.indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("indexColumn.columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("indexColumn.seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("indexColumn.sortDir")
                                        .description("정렬 방향 (ASC/DESC)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("생성된 인덱스 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("인덱스 컬럼 ID"),
                                fieldWithPath("result.indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("result.sortDir")
                                        .description("정렬 방향 (ASC, DESC)"))));
    }

    @Test
    @DisplayName("인덱스 컬럼 정렬 방향 변경 API 문서화")
    void updateIndexColumnSortDir() throws Exception {
        String indexId = "06D6WNJS8XWZT41HWZ226ZS904";
        String indexColumnId = "06D6WPTT2EJ7S1CJATWGEYPNS4";
        UpdateIndexColumnSortDirRequest request = new UpdateIndexColumnSortDirRequest(
                "DESC");

        String mockResponseJson = """
                {
                    "id": "06D6WPTT2EJ7S1CJATWGEYPNS4",
                    "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                    "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                    "seqNo": 1,
                    "sortDir": "DESC"
                }
                """;

        when(indexService.updateIndexColumnSortDir(any(String.class),
                any(String.class), any(UpdateIndexColumnSortDirRequest.class)))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        IndexColumnResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH
                        + "/indexes/{indexId}/columns/{indexColumnId}/sort-dir",
                        indexId, indexColumnId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(indexColumnId)
                .jsonPath("$.result.sortDir").isEqualTo("DESC")
                .consumeWith(document("index-column-update-sort-dir",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID"),
                                parameterWithName("indexColumnId")
                                        .description("인덱스 컬럼 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("sortDir")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "변경할 정렬 방향 (ASC/DESC)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 인덱스 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("인덱스 컬럼 ID"),
                                fieldWithPath("result.indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("result.columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("result.sortDir")
                                        .description(
                                                "정렬 방향 (ASC, DESC)"))));
    }

    @Test
    @DisplayName("인덱스에서 컬럼 제거 API 문서화")
    void removeColumnFromIndex() throws Exception {
        Validation.RemoveColumnFromIndexRequest.Builder builder = Validation.RemoveColumnFromIndexRequest
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
                                                "relationships": [],
                                                "indexes": [
                                                    {
                                                        "id": "06D6WNJS8XWZT41HWZ226ZS904",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "IDX1",
                                                        "type": "BTREE",
                                                        "comment": "",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WNJSBMS6AWBM1ZD7EDJP4M",
                                                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1,
                                                                "sortDir": "ASC"
                                                            },
                                                            {
                                                                "id": "06D6WPTT2EJ7S1CJATWGEYPNS4",
                                                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                                                "columnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                                "seqNo": 2,
                                                                "sortDir": "ASC"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                            "indexColumnId": "06D6WPTT2EJ7S1CJATWGEYPNS4"
                        }
                        """,
                        builder);
        Validation.RemoveColumnFromIndexRequest request = builder.build();

        when(indexService.removeColumnFromIndex(
                any(Validation.RemoveColumnFromIndexRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/indexes/{indexId}/columns/{columnId}",
                        "06D6WNJS8XWZT41HWZ226ZS904",
                        "06D6WPTT2EJ7S1CJATWGEYPNS4")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("index-remove-column",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID"),
                                parameterWithName("columnId")
                                        .description("인덱스 컬럼 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("database.id")
                                        .description("데이터베이스 ID"),
                                fieldWithPath("schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("indexId")
                                        .description("인덱스 ID"),
                                fieldWithPath("indexColumnId")
                                        .description("삭제할 인덱스 컬럼 ID")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"))));
    }

    @Test
    @DisplayName("인덱스 삭제 API 문서화")
    void deleteIndex() throws Exception {
        Validation.DeleteIndexRequest.Builder builder = Validation.DeleteIndexRequest
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
                                                "relationships": [],
                                                "indexes": [
                                                    {
                                                        "id": "06D6WNJS8XWZT41HWZ226ZS904",
                                                        "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "IDX1",
                                                        "type": "BTREE",
                                                        "comment": "",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WNJSBMS6AWBM1ZD7EDJP4M",
                                                                "indexId": "06D6WNJS8XWZT41HWZ226ZS904",
                                                                "columnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1,
                                                                "sortDir": "ASC"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                            "indexId": "06D6WNJS8XWZT41HWZ226ZS904"
                        }
                        """,
                        builder);
        Validation.DeleteIndexRequest request = builder.build();

        when(indexService.deleteIndex(any(Validation.DeleteIndexRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/indexes/{indexId}",
                        "06D6WNJS8XWZT41HWZ226ZS904")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("index-delete",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("삭제할 인덱스의 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입"),
                                headerWithName("Accept").description("응답 포맷")),
                        relaxedRequestFields(
                                fieldWithPath("database.id")
                                        .description("데이터베이스 ID"),
                                fieldWithPath("schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("indexId")
                                        .description("삭제할 인덱스 ID")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"))));
    }

}
