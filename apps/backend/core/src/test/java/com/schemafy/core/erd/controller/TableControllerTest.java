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
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.erd.controller.dto.request.UpdateTableExtraRequest;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.service.ColumnService;
import com.schemafy.core.erd.service.ConstraintService;
import com.schemafy.core.erd.service.IndexService;
import com.schemafy.core.erd.service.RelationshipService;
import com.schemafy.core.erd.service.TableService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("TableController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class TableControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private static final String API_BASE_PATH = ApiPath.API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private ColumnService columnService;

    @MockitoBean
    private RelationshipService relationshipService;

    @MockitoBean
    private IndexService indexService;

    @MockitoBean
    private ConstraintService constraintService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("테이블 생성 API 문서화")
    void createTable() throws Exception {
        Validation.CreateTableRequest.Builder builder = Validation.CreateTableRequest
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
                                        "tables": []
                                    }
                                ]
                            },
                            "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                            "table": {
                                "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                "name": "users",
                                "comment": "사용자 테이블",
                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                "columns": [],
                                "indexes": [],
                                "constraints": [],
                                "relationships": []
                            }
                        }
                        """,
                        builder);
        Validation.CreateTableRequest request = builder.build();

        AffectedMappingResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "schemas": {},
                                                "tables": {
                                                    "01ARZ3NDEKTSV4RRFFQ69G5FAV": "06D6W1GAHD51T5NJPK29Q6BCR8"
                                                },
                                                "columns": {},
                                                "indexes": {},
                                                "indexColumns": {},
                                                "constraints": {},
                                                "constraintColumns": {},
                                                "relationships": {},
                                                "relationshipColumns": {},
                                                "propagated": {
                                                    "columns": [],
                                                    "relationshipColumns": [],
                                                    "constraintColumns": [],
                                                    "indexColumns": []
                                                }
                                            }
                                        }
                                        """)
                        .get("result"),
                AffectedMappingResponse.class);

        given(tableService.createTable(any()))
                .willReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri(API_BASE_PATH + "/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.tables.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                .isEqualTo("06D6W1GAHD51T5NJPK29Q6BCR8")
                .consumeWith(document("table-create",
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
                                fieldWithPath("table.id")
                                        .description("테이블 ID (FE ID)"),
                                fieldWithPath("table.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("table.name")
                                        .description("테이블 이름"),
                                fieldWithPath("table.comment")
                                        .description("테이블 설명"),
                                fieldWithPath("table.tableOptions")
                                        .description("테이블 옵션")),
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
                                        "result.propagated.relationshipColumns")
                                        .description("전파된 관계 컬럼 목록"),
                                fieldWithPath(
                                        "result.propagated.constraintColumns")
                                        .description("전파된 제약조건 컬럼 목록"),
                                fieldWithPath("result.propagated.indexColumns")
                                        .description("전파된 인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("테이블 단건 조회 API 문서화")
    void getTable() throws Exception {
        String tableId = "06D6W1GAHD51T5NJPK29Q6BCR8";

        TableDetailResponse tableDetailResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "extra": "{}",
                                                "createdAt": "2025-11-10T13:48:01Z",
                                                "updatedAt": "2025-11-10T13:48:01Z",
                                                "columns": [],
                                                "constraints": [],
                                                "indexes": [],
                                                "relationships": []
                                            }
                                        }
                                        """)
                        .get("result"),
                TableDetailResponse.class);

        given(tableService.getTable(tableId))
                .willReturn(Mono.just(tableDetailResponse));

        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W1GAHD51T5NJPK29Q6BCR8")
                .jsonPath("$.result.schemaId")
                .isEqualTo("06D6VZBWHSDJBBG0H7D156YZ98")
                .jsonPath("$.result.name").isEqualTo("users")
                .jsonPath("$.result.comment").isEqualTo("사용자 테이블")
                .jsonPath("$.result.tableOptions")
                .isEqualTo("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
                .consumeWith(document("table-get",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("조회할 테이블의 ID")),
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
                                fieldWithPath("result").description("테이블 정보"),
                                fieldWithPath("result.id")
                                        .description("테이블 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.name")
                                        .description("테이블 이름"),
                                fieldWithPath("result.comment")
                                        .description("테이블 설명"),
                                fieldWithPath("result.tableOptions")
                                        .description("테이블 옵션"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.columns")
                                        .description("컨럼 목록"),
                                fieldWithPath("result.constraints")
                                        .description("제약조건 목록"),
                                fieldWithPath("result.indexes")
                                        .description("인덱스 목록"),
                                fieldWithPath("result.relationships")
                                        .description("관계 목록"))));
    }

    @Test
    @DisplayName("테이블 이름 변경 API 문서화")
    void updateTableName() throws Exception {
        Validation.ChangeTableNameRequest.Builder builder = Validation.ChangeTableNameRequest
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
                                                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
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
                            "tableId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                            "newName": "user"
                        }
                        """,
                        builder);
        Validation.ChangeTableNameRequest request = builder.build();

        TableResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "user",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "extra": "{}",
                                                "createdAt": "2025-11-10T13:48:01Z",
                                                "updatedAt": "2025-11-10T14:15:08.399530Z"
                                            }
                                        }
                                        """)
                        .get("result"),
                TableResponse.class);

        given(tableService.updateTableName(request))
                .willReturn(Mono.just(mockResponse));

        webTestClient.put()
                .uri(API_BASE_PATH + "/tables/{tableId}/name",
                        "06D6W1GAHD51T5NJPK29Q6BCR8")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W1GAHD51T5NJPK29Q6BCR8")
                .jsonPath("$.result.schemaId")
                .isEqualTo("06D6VZBWHSDJBBG0H7D156YZ98")
                .jsonPath("$.result.name").isEqualTo("user")
                .jsonPath("$.result.comment").isEqualTo("사용자 테이블")
                .jsonPath("$.result.tableOptions")
                .isEqualTo("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
                .consumeWith(document("table-update-name",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
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
                                        .description("변경할 테이블 ID"),
                                fieldWithPath("newName")
                                        .description("새 테이블 이름")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 테이블 정보"),
                                fieldWithPath("result.id")
                                        .description("테이블 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.name")
                                        .description("변경된 테이블 이름"),
                                fieldWithPath("result.comment")
                                        .description("테이블 설명"),
                                fieldWithPath("result.tableOptions")
                                        .description("테이블 옵션"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("테이블 추가 정보 변경 API 문서화")
    void updateTableExtra() throws Exception {
        String tableId = "06D6W1GAHD51T5NJPK29Q6BCR8";
        String extra = """
                {"uiColor":"blue"}
                """;

        UpdateTableExtraRequest request = new UpdateTableExtraRequest(extra);

        TableResponse mockResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "success": true,
                                            "result": {
                                                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
                                                "schemaId": "06D6VZBWHSDJBBG0H7D156YZ98",
                                                "name": "users",
                                                "comment": "사용자 테이블",
                                                "tableOptions": "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                                                "extra": "{\\"uiColor\\":\\"blue\\"}",
                                                "createdAt": "2025-11-10T13:48:01Z",
                                                "updatedAt": "2025-11-10T14:15:08.399530Z"
                                            }
                                        }
                                        """)
                        .get("result"),
                TableResponse.class);

        given(tableService.updateTableExtra(eq(tableId), eq(extra)))
                .willReturn(Mono.just(mockResponse));

        webTestClient.put()
                .uri(API_BASE_PATH + "/tables/{tableId}/extra", tableId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo("06D6W1GAHD51T5NJPK29Q6BCR8")
                .jsonPath("$.result.extra").isEqualTo("{\"uiColor\":\"blue\"}")
                .consumeWith(document("table-update-extra",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description(
                                                "요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description(
                                                "응답 포맷 (application/json)")),
                        requestFields(
                                fieldWithPath("extra")
                                        .type(JsonFieldType.STRING)
                                        .description("추가 정보(JSON 문자열)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("수정된 테이블 정보"),
                                fieldWithPath("result.id")
                                        .description("테이블 ID"),
                                fieldWithPath("result.schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result.name")
                                        .description("테이블 이름"),
                                fieldWithPath("result.comment")
                                        .description("테이블 설명"),
                                fieldWithPath("result.tableOptions")
                                        .description("테이블 옵션"),
                                fieldWithPath("result.extra")
                                        .description("추가 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"))));
    }

    @Test
    @DisplayName("테이블 삭제 API 문서화")
    void deleteTable() throws Exception {
        Validation.DeleteTableRequest.Builder builder = Validation.DeleteTableRequest
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
                                                "id": "06D6W1GAHD51T5NJPK29Q6BCR8",
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
                            "tableId": "06D6W1GAHD51T5NJPK29Q6BCR8"
                        }
                        """,
                        builder);
        Validation.DeleteTableRequest request = builder.build();

        given(tableService.deleteTable(request)).willReturn(Mono.empty());

        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/tables/{tableId}",
                        "06D6W1GAHD51T5NJPK29Q6BCR8")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(document("table-delete",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("삭제할 테이블의 ID")),
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
                                        .description("삭제할 테이블 ID")),
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
    @DisplayName("테이블별 컬럼 목록 조회 API 문서화")
    void getColumnsByTableId() throws Exception {
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

        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}/columns", tableId)
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
    @DisplayName("테이블별 관계 목록 조회 API 문서화")
    void getRelationshipsByTableId() throws Exception {
        String tableId = "06D6W8HDY79QFZX39RMX62KSX4";

        RelationshipResponse relationshipResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "id": "06D6WJQP78M1RKSJMT8JWKNJNG",
                                            "srcTableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                            "tgtTableId": "06D6W8HDY79QFZX39RMX62KSX5",
                                            "name": "fk_users_orders",
                                            "kind": "FOREIGN_KEY",
                                            "cardinality": "ONE_TO_MANY",
                                            "onDelete": "CASCADE",
                                            "onUpdate": "CASCADE",
                                            "extra": "{}",
                                            "columns": []
                                        }
                                        """),
                RelationshipResponse.class);

        given(relationshipService.getRelationshipsByTableId(tableId))
                .willReturn(Flux.just(relationshipResponse));

        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}/relationships", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .consumeWith(document("relationship-list-by-table",
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
                                        .description("삭제 시 동작"),
                                fieldWithPath("result[].onUpdate")
                                        .description("수정 시 동작"),
                                fieldWithPath("result[].extra")
                                        .description("추가 정보"),
                                fieldWithPath("result[].columns")
                                        .description("관계 컬럼 목록"))));
    }

    @Test
    @DisplayName("테이블별 인덱스 목록 조회 API 문서화")
    void getIndexesByTableId() throws Exception {
        String tableId = "06D6W8HDY79QFZX39RMX62KSX4";

        IndexResponse indexResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "id": "06D6WKQP78M1RKSJMT8JWKNJNG",
                                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                            "name": "idx_user_id",
                                            "type": "BTREE",
                                            "comment": "인덱스 코멘트",
                                            "columns": []
                                        }
                                        """),
                IndexResponse.class);

        given(indexService.getIndexesByTableId(tableId))
                .willReturn(Flux.just(indexResponse));

        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}/indexes", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .consumeWith(document("index-list-by-table",
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
                                fieldWithPath("result").description("인덱스 목록"),
                                fieldWithPath("result[].id")
                                        .description("인덱스 ID"),
                                fieldWithPath("result[].tableId")
                                        .description("테이블 ID"),
                                fieldWithPath("result[].name")
                                        .description("인덱스 이름"),
                                fieldWithPath("result[].type")
                                        .description("인덱스 타입"),
                                fieldWithPath("result[].comment")
                                        .description("인덱스 코멘트"),
                                fieldWithPath("result[].columns")
                                        .description("인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("테이블별 제약조건 목록 조회 API 문서화")
    void getConstraintsByTableId() throws Exception {
        String tableId = "06D6W8HDY79QFZX39RMX62KSX4";

        ConstraintResponse constraintResponse = objectMapper.treeToValue(
                objectMapper
                        .readTree(
                                """
                                        {
                                            "id": "06D6WLQP78M1RKSJMT8JWKNJNG",
                                            "tableId": "06D6W8HDY79QFZX39RMX62KSX4",
                                            "name": "pk_user_id",
                                            "kind": "PRIMARY_KEY",
                                            "columns": []
                                        }
                                        """),
                ConstraintResponse.class);

        given(constraintService.getConstraintsByTableId(tableId))
                .willReturn(Flux.just(constraintResponse));

        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}/constraints", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(1)
                .consumeWith(document("constraint-list-by-table",
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
                                        .description("제약조건 컬럼 목록"))));
    }

}
