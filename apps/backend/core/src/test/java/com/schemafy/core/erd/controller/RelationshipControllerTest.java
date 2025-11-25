package com.schemafy.core.erd.controller;

import java.util.Collections;
import java.util.Map;

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
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.service.RelationshipService;

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
@DisplayName("RelationshipController RestDocs 테스트")
@WithMockCustomUser(roles = "EDITOR")
class RelationshipControllerTest {

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RelationshipService relationshipService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("관계 생성 API 문서화")
    void createRelationship() throws Exception {
        Validation.CreateRelationshipRequest.Builder builder = Validation.CreateRelationshipRequest
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
                            "relationship": {
                                "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                "name": "FK_recommend_user",
                                "kind": "NON_IDENTIFYING",
                                "cardinality": "ONE_TO_MANY",
                                "onDelete": "CASCADE",
                                "onUpdate": "CASCADE_UPDATE",
                                "fkEnforced": false,
                                "columns": [{
                                    "id": "06D4YK995770K0J8539XGNHNW0",
                                    "relationshipId": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                    "fkColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                    "refColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                    "seqNo": 1
                                }]
                            }
                        }
                        """,
                        builder);
        Validation.CreateRelationshipRequest request = builder.build();

        String mockResponseJson = """
                {
                    "schemas": {},
                    "tables": {},
                    "columns": {},
                    "indexes": {},
                    "indexColumns": {},
                    "constraints": {},
                    "constraintColumns": {},
                    "relationships": {
                        "06D6W8HDY79QFZX39RMX62KSX4": {
                            "01ARZ3NDEKTSV4RRFFQ69G5FAV": "06D6WCH677C3FCC2Q9SD5M1Y5W"
                        }
                    },
                    "relationshipColumns": {
                        "06D6WCH677C3FCC2Q9SD5M1Y5W": {
                            "06D4YK995770K0J8539XGNHNW0": "06D4YK995770K0J8539XGNHNW0"
                        }
                    },
                    "propagated": {
                        "columns": [],
                        "constraintColumns": [],
                        "indexColumns": []
                    }
                }
                """;
        AffectedMappingResponse mockResponse = objectMapper.readValue(
                mockResponseJson, AffectedMappingResponse.class);

        when(relationshipService.createRelationship(any()))
                .thenReturn(Mono.just(mockResponse));

        String requestJson = toJson(request);

        webTestClient.post()
                .uri(API_BASE_PATH + "/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.relationships['06D6W8HDY79QFZX39RMX62KSX4']['01ARZ3NDEKTSV4RRFFQ69G5FAV']")
                .isEqualTo("06D6WCH677C3FCC2Q9SD5M1Y5W")
                .jsonPath(
                        "$.result.relationshipColumns['06D6WCH677C3FCC2Q9SD5M1Y5W']['06D4YK995770K0J8539XGNHNW0']")
                .isEqualTo("06D4YK995770K0J8539XGNHNW0")
                .consumeWith(document("relationship-create",
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
                                                "관계 ID 매핑 (Source Table BE ID -> { FE ID -> BE ID })"),
                                subsectionWithPath("result.relationshipColumns")
                                        .description(
                                                "관계 컬럼 ID 매핑 (Relationship BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.propagated")
                                        .description("전파된 엔티티 정보"),
                                fieldWithPath("result.propagated.columns")
                                        .description(
                                                "전파된 컬럼 목록 (식별 관계 시 자식 테이블로 전파)"),
                                fieldWithPath(
                                        "result.propagated.constraintColumns")
                                        .description("전파된 제약조건 컬럼 목록"),
                                fieldWithPath("result.propagated.indexColumns")
                                        .description("전파된 인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("관계 단건 조회 API 문서화")
    void getRelationship() throws Exception {
        String mockResponseJson = """
                {
                    "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                    "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "FK_recommend_user",
                    "kind": "NON_IDENTIFYING",
                    "cardinality": "ONE_TO_MANY",
                    "onDelete": "CASCADE",
                    "onUpdate": "CASCADE_UPDATE",
                    "extra": "{}",
                    "columns": [
                        {
                            "id": "06D6WCH68V89ZSPWVZ8WMBQWW8",
                            "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                            "srcColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "tgtColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                            "seqNo": 1
                        }
                    ]
                }
                """;

        when(relationshipService.getRelationship("06D6WCH677C3FCC2Q9SD5M1Y5W"))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        RelationshipResponse.class)));

        webTestClient.get()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}",
                        "06D6WCH677C3FCC2Q9SD5M1Y5W")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WCH677C3FCC2Q9SD5M1Y5W")
                .jsonPath("$.result.name").isEqualTo("FK_recommend_user")
                .jsonPath("$.result.kind").isEqualTo("NON_IDENTIFYING")
                .jsonPath("$.result.columns[0].id")
                .isEqualTo("06D6WCH68V89ZSPWVZ8WMBQWW8")
                .consumeWith(document("relationship-get",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("조회할 관계의 ID")),
                        requestHeaders(
                                headerWithName("Accept").description("응답 포맷")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("관계 정보"),
                                fieldWithPath("result.id").description("관계 ID"),
                                fieldWithPath("result.srcTableId")
                                        .description("소스 테이블 ID (FK를 가진 테이블)"),
                                fieldWithPath("result.tgtTableId")
                                        .description("타겟 테이블 ID (PK를 가진 테이블)"),
                                fieldWithPath("result.name")
                                        .description("관계 이름"),
                                fieldWithPath("result.kind").description(
                                        "관계 종류 (IDENTIFYING: 식별 관계, NON_IDENTIFYING: 비식별 관계)"),
                                fieldWithPath("result.cardinality").description(
                                        "카디널리티 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)"),
                                fieldWithPath("result.onDelete").description(
                                        "DELETE 시 액션 (CASCADE, SET_NULL, RESTRICT, NO_ACTION)"),
                                fieldWithPath("result.onUpdate").description(
                                        "UPDATE 시 액션 (CASCADE, SET_NULL, RESTRICT, NO_ACTION)"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보 (JSON 형식)"),
                                fieldWithPath("result.columns")
                                        .description("관계 컬럼 목록"),
                                fieldWithPath("result.columns[].id")
                                        .description("관계 컬럼 ID"),
                                fieldWithPath("result.columns[].relationshipId")
                                        .description("관계 ID"),
                                fieldWithPath("result.columns[].srcColumnId")
                                        .description("소스 컬럼 ID (FK 컬럼)"),
                                fieldWithPath("result.columns[].tgtColumnId")
                                        .description("타겟 컬럼 ID (참조되는 컬럼)"),
                                fieldWithPath("result.columns[].seqNo")
                                        .description("순서 번호"))));
    }

    @Test
    @DisplayName("테이블별 관계 목록 조회 API 문서화")
    void getRelationshipsByTableId() throws Exception {
        String mockResponseJson = """
                [
                    {
                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                        "name": "FK_recommend_user",
                        "kind": "NON_IDENTIFYING",
                        "cardinality": "ONE_TO_MANY",
                        "onDelete": "CASCADE",
                        "onUpdate": "CASCADE_UPDATE",
                        "extra": "{}",
                        "columns": [
                            {
                                "id": "06D6WCH68V89ZSPWVZ8WMBQWW8",
                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                "srcColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                "tgtColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                "seqNo": 1
                            }
                        ]
                    }
                ]
                """;

        when(relationshipService
                .getRelationshipsByTableId("06D6W8HDY79QFZX39RMX62KSX4"))
                .thenReturn(
                        Flux.fromArray(objectMapper.readValue(mockResponseJson,
                                RelationshipResponse[].class)));

        webTestClient.get()
                .uri(API_BASE_PATH + "/relationships/table/{tableId}",
                        "06D6W8HDY79QFZX39RMX62KSX4")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .jsonPath("$.result[0].id")
                .isEqualTo("06D6WCH677C3FCC2Q9SD5M1Y5W")
                .jsonPath("$.result[0].name").isEqualTo("FK_recommend_user")
                .consumeWith(document("relationship-get-by-table",
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
                                fieldWithPath("result").description("관계 목록"),
                                fieldWithPath("result[].id")
                                        .description("관계 ID"),
                                fieldWithPath("result[].srcTableId")
                                        .description("소스 테이블 ID"),
                                fieldWithPath("result[].tgtTableId")
                                        .description("타겟 테이블 ID"),
                                fieldWithPath("result[].name")
                                        .description("관계 이름"),
                                fieldWithPath("result[].kind")
                                        .description("관계 종류"),
                                fieldWithPath("result[].cardinality")
                                        .description("카디널리티"),
                                fieldWithPath("result[].onDelete")
                                        .description("DELETE 액션"),
                                fieldWithPath("result[].onUpdate")
                                        .description("UPDATE 액션"),
                                fieldWithPath("result[].extra")
                                        .description("추가 정보"),
                                fieldWithPath("result[].columns")
                                        .description("관계 컬럼 목록"),
                                fieldWithPath("result[].columns[].id")
                                        .description("관계 컬럼 ID"),
                                fieldWithPath(
                                        "result[].columns[].relationshipId")
                                        .description("관계 ID"),
                                fieldWithPath("result[].columns[].srcColumnId")
                                        .description("소스 컬럼 ID (FK 컬럼)"),
                                fieldWithPath("result[].columns[].tgtColumnId")
                                        .description("타겟 컬럼 ID (참조되는 컬럼)"),
                                fieldWithPath("result[].columns[].seqNo")
                                        .description("순서 번호"))));
    }

    @Test
    @DisplayName("관계 이름 변경 API 문서화")
    void updateRelationshipName() throws Exception {
        Validation.ChangeRelationshipNameRequest.Builder builder = Validation.ChangeRelationshipNameRequest
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
                                                "relationships": [
                                                    {
                                                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "FK_recommend_user",
                                                        "kind": "NON_IDENTIFYING",
                                                        "cardinality": "ONE_TO_MANY",
                                                        "onDelete": "CASCADE",
                                                        "onUpdate": "CASCADE_UPDATE",
                                                        "fkEnforced": false,
                                                        "columns": [
                                                            {
                                                                "id": "06D4YK995770K0J8539XGNHNW0",
                                                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                                "fkColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "refColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
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
                            "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                            "newName": "FK_recommend_other_user"
                        }
                        """,
                        builder);
        Validation.ChangeRelationshipNameRequest request = builder.build();

        String mockResponseJson = """
                {
                    "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                    "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "FK_recommend_other_user",
                    "kind": "NON_IDENTIFYING",
                    "cardinality": "ONE_TO_MANY",
                    "onDelete": "CASCADE",
                    "onUpdate": "CASCADE_UPDATE",
                    "extra": "{}",
                    "columns": []
                }
                """;

        when(relationshipService.updateRelationshipName(
                any(Validation.ChangeRelationshipNameRequest.class)))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        RelationshipResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}/name",
                        "06D6WCH677C3FCC2Q9SD5M1Y5W")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WCH677C3FCC2Q9SD5M1Y5W")
                .jsonPath("$.result.name").isEqualTo("FK_recommend_other_user")
                .jsonPath("$.result.kind").isEqualTo("NON_IDENTIFYING")
                .consumeWith(document("relationship-update-name",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("관계 ID")),
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
                                        .description("수정된 관계 정보"),
                                fieldWithPath("result.id").description("관계 ID"),
                                fieldWithPath("result.srcTableId")
                                        .description("소스 테이블 ID"),
                                fieldWithPath("result.tgtTableId")
                                        .description("타겟 테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 관계 이름"),
                                fieldWithPath("result.kind")
                                        .description("관계 종류"),
                                fieldWithPath("result.cardinality")
                                        .description("카디널리티"),
                                fieldWithPath("result.onDelete")
                                        .description("DELETE 액션"),
                                fieldWithPath("result.onUpdate")
                                        .description("UPDATE 액션"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보"),
                                fieldWithPath("result.columns")
                                        .description("관계 컬럼 목록"))));
    }

    @Test
    @DisplayName("관계 카디널리티 변경 API 문서화")
    void updateRelationshipCardinality() throws Exception {
        Validation.ChangeRelationshipCardinalityRequest.Builder builder = Validation.ChangeRelationshipCardinalityRequest
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
                                                "relationships": [
                                                    {
                                                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "FK_recommend_other_user",
                                                        "kind": "NON_IDENTIFYING",
                                                        "cardinality": "ONE_TO_MANY",
                                                        "onDelete": "CASCADE",
                                                        "onUpdate": "CASCADE_UPDATE",
                                                        "fkEnforced": false,
                                                        "columns": [
                                                            {
                                                                "id": "06D4YK995770K0J8539XGNHNW0",
                                                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                                "fkColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "refColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
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
                            "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                            "cardinality": "ONE_TO_ONE"
                        }
                        """,
                        builder);
        Validation.ChangeRelationshipCardinalityRequest request = builder
                .build();

        String mockResponseJson = """
                {
                    "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                    "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                    "name": "FK_recommend_other_user",
                    "kind": "NON_IDENTIFYING",
                    "cardinality": "ONE_TO_ONE",
                    "onDelete": "CASCADE",
                    "onUpdate": "CASCADE_UPDATE",
                    "extra": "{}",
                    "columns": []
                }
                """;

        when(relationshipService.updateRelationshipCardinality(
                any(Validation.ChangeRelationshipCardinalityRequest.class)))
                .thenReturn(Mono.just(objectMapper.readValue(mockResponseJson,
                        RelationshipResponse.class)));

        webTestClient.put()
                .uri(API_BASE_PATH
                        + "/relationships/{relationshipId}/cardinality",
                        "06D6WCH677C3FCC2Q9SD5M1Y5W")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6WCH677C3FCC2Q9SD5M1Y5W")
                .jsonPath("$.result.cardinality").isEqualTo("ONE_TO_ONE")
                .jsonPath("$.result.name").isEqualTo("FK_recommend_other_user")
                .consumeWith(document("relationship-update-cardinality",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("관계 ID")),
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
                                        .description("수정된 관계 정보"),
                                fieldWithPath("result.id").description("관계 ID"),
                                fieldWithPath("result.srcTableId")
                                        .description("소스 테이블 ID"),
                                fieldWithPath("result.tgtTableId")
                                        .description("타겟 테이블 ID"),
                                fieldWithPath("result.name")
                                        .description("관계 이름"),
                                fieldWithPath("result.kind")
                                        .description("관계 종류"),
                                fieldWithPath("result.cardinality")
                                        .description("변경된 카디널리티"),
                                fieldWithPath("result.onDelete")
                                        .description("DELETE 액션"),
                                fieldWithPath("result.onUpdate")
                                        .description("UPDATE 액션"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보"),
                                fieldWithPath("result.columns")
                                        .description("관계 컬럼 목록"))));
    }

    @Test
    @DisplayName("관계에 컬럼 추가 API 문서화")
    void addColumnToRelationship() throws Exception {
        Validation.AddColumnToRelationshipRequest.Builder builder = Validation.AddColumnToRelationshipRequest
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
                                                "relationships": [
                                                    {
                                                        "id": "06D590QBYGE6K2TQ8JK514GGP4",
                                                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "FK_recommend_users",
                                                        "kind": "NON_IDENTIFYING",
                                                        "cardinality": "ONE_TO_ONE",
                                                        "onDelete": "CASCADE",
                                                        "onUpdate": "CASCADE_UPDATE",
                                                        "extra": "{}",
                                                        "columns": [
                                                            {
                                                                "id": "06D590QC452PA8W5NQ2BDXNEDG",
                                                                "relationshipId": "06D590QBYGE6K2TQ8JK514GGP4",
                                                                "srcColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "tgtColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
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
                            "relationshipId": "06D590QBYGE6K2TQ8JK514GGP4",
                            "relationshipColumn": {
                                "id": "06D5JGQY03RKQPMC9CJ3B0EGM8",
                                "relationshipId": "06D590QBYGE6K2TQ8JK514GGP4",
                                "fkColumnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                "refColumnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                "seqNo": 2
                            }
                        }
                        """,
                        builder);
        Validation.AddColumnToRelationshipRequest request = builder.build();

        when(relationshipService.addColumnToRelationship(
                any(Validation.AddColumnToRelationshipRequest.class)))
                .thenReturn(Mono.just(new AffectedMappingResponse(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Map.of("06D590QBYGE6K2TQ8JK514GGP4",
                                Map.of("06D590QBYGE6K2TQ8JK514GGP4",
                                        "06D590QBYGE6K2TQ8JK514GGP4")),
                        Map.of("06D590QBYGE6K2TQ8JK514GGP4",
                                Map.of(
                                        "06D6WGN2ZWCGM2SVWRW2GEP8WW",
                                        "06D6WGN2ZWCGM2SVWRW2GEP8WW")),
                        AffectedMappingResponse.PropagatedEntities.empty())));

        webTestClient.post()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}/columns",
                        "06D590QBYGE6K2TQ8JK514GGP4")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath(
                        "$.result.relationships['06D590QBYGE6K2TQ8JK514GGP4']['06D590QBYGE6K2TQ8JK514GGP4']")
                .isEqualTo("06D590QBYGE6K2TQ8JK514GGP4")
                .jsonPath(
                        "$.result.relationshipColumns['06D590QBYGE6K2TQ8JK514GGP4']['06D6WGN2ZWCGM2SVWRW2GEP8WW']")
                .isEqualTo("06D6WGN2ZWCGM2SVWRW2GEP8WW")
                .consumeWith(document("relationship-add-column",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("관계 ID")),
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
    @DisplayName("관계에서 컬럼 제거 API 문서화")
    void removeColumnFromRelationship() throws Exception {
        Validation.RemoveColumnFromRelationshipRequest.Builder builder = Validation.RemoveColumnFromRelationshipRequest
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
                                                "relationships": [
                                                    {
                                                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "FK_recommend_users",
                                                        "kind": "NON_IDENTIFYING",
                                                        "cardinality": "ONE_TO_ONE",
                                                        "onDelete": "CASCADE",
                                                        "onUpdate": "CASCADE_UPDATE",
                                                        "extra": "{}",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WCH68V89ZSPWVZ8WMBQWW8",
                                                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                                "srcColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "tgtColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
                                                            },
                                                            {
                                                                "id": "06D6WH7CC5YVNWKVB888ZB42EM",
                                                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                                "srcColumnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                                "tgtColumnId": "06D6WG72ZPAK38RNWP3DRK7W8C",
                                                                "seqNo": 2
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
                            "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                            "relationshipColumnId": "06D6WH7CC5YVNWKVB888ZB42EM"
                        }
                        """,
                        builder);
        Validation.RemoveColumnFromRelationshipRequest request = builder
                .build();

        when(relationshipService.removeColumnFromRelationship(
                any(Validation.RemoveColumnFromRelationshipRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH
                        + "/relationships/{relationshipId}/columns/{columnId}",
                        "06D6WCH677C3FCC2Q9SD5M1Y5W",
                        "06D6WH7CC5YVNWKVB888ZB42EM")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("relationship-remove-column",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("관계 ID"),
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("응답 데이터 (null)"))));
    }

    @Test
    @DisplayName("관계 삭제 API 문서화")
    void deleteRelationship() throws Exception {
        Validation.DeleteRelationshipRequest.Builder builder = Validation.DeleteRelationshipRequest
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
                                                "relationships": [
                                                    {
                                                        "id": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                        "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                                        "name": "FK_recommend_users",
                                                        "kind": "NON_IDENTIFYING",
                                                        "cardinality": "ONE_TO_ONE",
                                                        "onDelete": "CASCADE",
                                                        "onUpdate": "CASCADE_UPDATE",
                                                        "extra": "{}",
                                                        "columns": [
                                                            {
                                                                "id": "06D6WCH68V89ZSPWVZ8WMBQWW8",
                                                                "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W",
                                                                "srcColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "tgtColumnId": "06D6W90RSE1VPFRMM4XPKYGM9M",
                                                                "seqNo": 1
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
                            "relationshipId": "06D6WCH677C3FCC2Q9SD5M1Y5W"
                        }
                        """,
                        builder);
        Validation.DeleteRelationshipRequest request = builder.build();

        when(relationshipService.deleteRelationship(
                any(Validation.DeleteRelationshipRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/relationships/{relationshipId}",
                        "06D6WCH677C3FCC2Q9SD5M1Y5W")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("relationship-delete",
                        pathParameters(
                                parameterWithName("relationshipId")
                                        .description("삭제할 관계의 ID")),
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
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("응답 데이터 (null)"))));
    }

}
