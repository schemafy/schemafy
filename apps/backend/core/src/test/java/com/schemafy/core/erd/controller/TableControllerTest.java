package com.schemafy.core.erd.controller;

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

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Table;
import com.schemafy.core.erd.service.TableService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("TableController 통합 테스트")
class TableControllerTest {

    private static final String API_BASE_PATH = ApiPath.AUTH_API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TableService tableService;

    private String toJson(Message message) throws Exception {
        return JsonFormat.printer()
                .print(message);
    }

    @Test
    @DisplayName("테이블 생성 API 문서화")
    void createTable() throws Exception {
        // Given
        Validation.CreateTableRequest request = Validation.CreateTableRequest
                .newBuilder()
                .setTable(Validation.Table.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                        .setName("users")
                        .setComment("사용자 테이블")
                        .setTableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
                        .build())
                .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .build())
                        .build())
                .build();

        AffectedMappingResponse mockResponse = new AffectedMappingResponse(
                Collections.emptyMap(),
                Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAX"),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        given(tableService.createTable(any()))
                .willReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.tables.01ARZ3NDEKTSV4RRFFQ69G5FAV").isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                .consumeWith(document("table-create",
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
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
                                        .description("테이블 ID 매핑 (FE ID -> BE ID)"),
                                fieldWithPath("result.tables.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                        .description("백엔드에서 생성된 테이블 ID"),
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
                                fieldWithPath("result.propagated.constraintColumns")
                                        .description("전파된 제약조건 컬럼 목록"),
                                fieldWithPath("result.propagated.indexColumns")
                                        .description("전파된 인덱스 컬럼 목록"))));
    }

    @Test
    @DisplayName("스키마별 테이블 목록 조회 API 문서화")
    void getTablesBySchemaId() throws Exception {
        // Given
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        
        Table table1 = Table.builder()
                .schemaId(schemaId)
                .name("users")
                .comment("사용자 테이블")
                .tableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
                .extra("{}")
                .build();
        ReflectionTestUtils.setField(table1, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAV");
        
        Table table2 = Table.builder()
                .schemaId(schemaId)
                .name("orders")
                .comment("주문 테이블")
                .tableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
                .extra("{}")
                .build();
        ReflectionTestUtils.setField(table2, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAX");

        given(tableService.getTablesBySchemaId(schemaId))
                .willReturn(Flux.just(table1, table2));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/schema/{schemaId}", schemaId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(2)
                .jsonPath("$.result[0].name").isEqualTo("users")
                .jsonPath("$.result[1].name").isEqualTo("orders")
                .consumeWith(document("table-list-by-schema",
                        pathParameters(
                                parameterWithName("schemaId")
                                        .description("조회할 스키마의 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("테이블 목록"),
                                fieldWithPath("result[].id")
                                        .description("테이블 ID"),
                                fieldWithPath("result[].schemaId")
                                        .description("스키마 ID"),
                                fieldWithPath("result[].name")
                                        .description("테이블 이름"),
                                fieldWithPath("result[].comment")
                                        .description("테이블 설명"),
                                fieldWithPath("result[].tableOptions")
                                        .description("테이블 옵션"),
                                fieldWithPath("result[].extra")
                                        .description("추가 정보"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result[].deletedAt")
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블 단건 조회 API 문서화")
    void getTable() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Table table = Table.builder()
                .schemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("users")
                .comment("사용자 테이블")
                .tableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
                .extra("생성일: 2024-01-01")
                .build();
        ReflectionTestUtils.setField(table, "id", tableId);

        given(tableService.getTable(tableId))
                .willReturn(Mono.just(table));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/tables/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("users")
                .consumeWith(document("table-get",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("조회할 테이블의 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
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
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블 이름 변경 API 문서화")
    void updateTableName() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Validation.ChangeTableNameRequest request = Validation.ChangeTableNameRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .build())
                                .build())
                        .build())
                .setTableId(tableId)
                .setNewName("members")
                .build();

        Table updated = Table.builder()
                .schemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("members")
                .comment("회원 테이블")
                .tableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
                .extra("변경일: 2024-06-15")
                .build();
        ReflectionTestUtils.setField(updated, "id", tableId);

        given(tableService.updateTableName(request))
                .willReturn(Mono.just(updated));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/tables/{tableId}/name", tableId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("members")
                .consumeWith(document("table-update-name",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
                        requestHeaders(
                                headerWithName("Content-Type")
                                        .description("요청 본문 타입 (application/json)"),
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("수정된 테이블 정보"),
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
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("테이블 삭제 API 문서화")
    void deleteTable() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Validation.DeleteTableRequest request = Validation.DeleteTableRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setName("temp_table")
                                        .setComment("임시 테이블")
                                        .build())
                                .build())
                        .build())
                .setTableId(tableId)
                .build();

        given(tableService.deleteTable(request)).willReturn(Mono.empty());

        // When & Then
        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/tables/{tableId}", tableId)
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
                                        .description("요청 본문 타입 (application/json)"),
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
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("응답 데이터 (null)"))));
    }

}

