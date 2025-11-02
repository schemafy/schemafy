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
import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.erd.service.ColumnService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ColumnController 통합 테스트")
class ColumnControllerTest {

    private static final String API_BASE_PATH = ApiPath.AUTH_API
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
        // Given
        Validation.CreateColumnRequest request = Validation.CreateColumnRequest
                .newBuilder()
                .setColumn(Validation.Column.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .setTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                        .setName("user_id")
                        .setDataType("BIGINT")
                        .setLengthScale("20")
                        .setIsAutoIncrement(false)
                        .setCharset("utf8mb4")
                        .setCollation("utf8mb4_unicode_ci")
                        .setComment("사용자 ID")
                        .setOrdinalPosition(1)
                        .build())
                .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FA2")
                .setTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("01ARZ3NDEKTSV4RRFFQ69G5FA2")
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FA2")
                                        .setName("users")
                                        .build())
                                .build())
                        .build())
                .build();

        AffectedMappingResponse mockResponse = new AffectedMappingResponse(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAW", Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAX")),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        given(columnService.createColumn(any(Validation.CreateColumnRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/columns")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.columns.01ARZ3NDEKTSV4RRFFQ69G5FAW.01ARZ3NDEKTSV4RRFFQ69G5FAV").isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                .consumeWith(document("column-create",
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
                                        .description("테이블 ID 매핑"),
                                fieldWithPath("result.columns")
                                        .description("컬럼 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.columns.01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .description("테이블별 컬럼 ID 매핑"),
                                fieldWithPath("result.columns.01ARZ3NDEKTSV4RRFFQ69G5FAW.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                        .description("백엔드에서 생성된 컬럼 ID"),
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
    @DisplayName("컬럼 단건 조회 API 문서화")
    void getColumn() throws Exception {
        // Given
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Column column = Column.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("user_id")
                .dataType("BIGINT")
                .lengthScale("20")
                .isAutoIncrement(true)
                .charset("utf8mb4")
                .collation("utf8mb4_unicode_ci")
                .comment("사용자 고유 ID (자동증가)")
                .ordinalPosition(1)
                .build();
        ReflectionTestUtils.setField(column, "id", columnId);

        given(columnService.getColumn(columnId))
                .willReturn(Mono.just(column));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/columns/{columnId}", columnId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("user_id")
                .consumeWith(document("column-get",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("조회할 컬럼의 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
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
                                fieldWithPath("result.autoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블별 컬럼 목록 조회 API 문서화")
    void getColumnsByTableId() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Column column1 = Column.builder()
                .tableId(tableId)
                .name("user_id")
                .dataType("BIGINT")
                .lengthScale("20")
                .isAutoIncrement(true)
                .charset("utf8mb4")
                .collation("utf8mb4_unicode_ci")
                .comment("사용자 고유 ID (PK)")
                .ordinalPosition(1)
                .build();
        Column column2 = Column.builder()
                .tableId(tableId)
                .name("email")
                .dataType("VARCHAR")
                .lengthScale("255")
                .isAutoIncrement(false)
                .charset("utf8mb4")
                .collation("utf8mb4_unicode_ci")
                .comment("이메일 주소 (UK)")
                .ordinalPosition(2)
                .build();
        ReflectionTestUtils.setField(column1, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAW");
        ReflectionTestUtils.setField(column2, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAX");

        given(columnService.getColumnsByTableId(tableId))
                .willReturn(Flux.just(column1, column2));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/columns/table/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(2)
                .consumeWith(document("column-list-by-table",
                        pathParameters(
                                parameterWithName("tableId")
                                        .description("테이블 ID")),
                        requestHeaders(
                                headerWithName("Accept")
                                        .description("응답 포맷 (application/json)")),
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
                                fieldWithPath("result[].autoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result[].charset")
                                        .description("문자 집합"),
                                fieldWithPath("result[].collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result[].comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result[].ordinalPosition")
                                        .description("컬럼 위치"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result[].deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("컬럼 이름 변경 API 문서화")
    void updateColumnName() throws Exception {
        // Given
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.ChangeColumnNameRequest request = Validation.ChangeColumnNameRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId(schemaId)
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId(columnId)
                                                .setTableId(tableId)
                                                .setName("email")
                                                .setDataType("VARCHAR")
                                                .setLengthScale("255")
                                                .setComment("이메일 주소")
                                                .setOrdinalPosition(2)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setColumnId(columnId)
                .setNewName("email_address")
                .build();

        Column updated = Column.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("email_address")
                .dataType("VARCHAR")
                .lengthScale("255")
                .charset("utf8mb4")
                .collation("utf8mb4_unicode_ci")
                .comment("이메일 주소 (변경됨)")
                .ordinalPosition(2)
                .build();
        ReflectionTestUtils.setField(updated, "id", columnId);

        given(columnService.updateColumnName(request))
                .willReturn(Mono.just(updated));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/name", columnId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("email_address")
                .consumeWith(document("column-update-name",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                fieldWithPath("result").description("수정된 컬럼 정보"),
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
                                fieldWithPath("result.autoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("컬럼 타입 변경 API 문서화")
    void updateColumnType() throws Exception {
        // Given
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.ChangeColumnTypeRequest request = Validation.ChangeColumnTypeRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId(schemaId)
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId(columnId)
                                                .setTableId(tableId)
                                                .setName("user_id")
                                                .setDataType("BIGINT")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setColumnId(columnId)
                .setDataType("INT")
                .build();

        Column updated = Column.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("user_id")
                .dataType("INT")
                .build();
        ReflectionTestUtils.setField(updated, "id", columnId);

        given(columnService.updateColumnType(request))
                .willReturn(Mono.just(updated));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/type", columnId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.dataType").isEqualTo("INT")
                .consumeWith(document("column-update-type",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                fieldWithPath("result").description("수정된 컬럼 정보"),
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
                                fieldWithPath("result.autoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("컬럼 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("컬럼 위치 변경 API 문서화")
    void updateColumnPosition() throws Exception {
        // Given
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.ChangeColumnPositionRequest request = Validation.ChangeColumnPositionRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId(schemaId)
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId(columnId)
                                                .setTableId(tableId)
                                                .setName("user_id")
                                                .setDataType("BIGINT")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setColumnId(columnId)
                .setNewPosition(5)
                .build();

        Column updated = Column.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("user_id")
                .dataType("BIGINT")
                .ordinalPosition(5)
                .build();
        ReflectionTestUtils.setField(updated, "id", columnId);

        given(columnService.updateColumnPosition(request))
                .willReturn(Mono.just(updated));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/columns/{columnId}/position", columnId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.ordinalPosition").isEqualTo(5)
                .consumeWith(document("column-update-position",
                        pathParameters(
                                parameterWithName("columnId")
                                        .description("컬럼 ID")),
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
                                fieldWithPath("result").description("수정된 컬럼 정보"),
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
                                fieldWithPath("result.autoIncrement")
                                        .description("자동 증가 여부"),
                                fieldWithPath("result.charset")
                                        .description("문자 집합"),
                                fieldWithPath("result.collation")
                                        .description("정렬 규칙"),
                                fieldWithPath("result.comment")
                                        .description("컬럼 설명"),
                                fieldWithPath("result.ordinalPosition")
                                        .description("변경된 컬럼 위치"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("컬럼 삭제 API 문서화")
    void deleteColumn() throws Exception {
        // Given
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.DeleteColumnRequest request = Validation.DeleteColumnRequest
                .newBuilder()
                .setDatabase(Validation.Database.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FA0")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId(schemaId)
                                .setProjectId("01ARZ3NDEKTSV4RRFFQ69G5FA1")
                                .setDbVendorId(Validation.DbVendor.MYSQL)
                                .setName("my_database")
                                .setCharset("utf8mb4")
                                .setCollation("utf8mb4_unicode_ci")
                                .addTables(Validation.Table.newBuilder()
                                        .setId(tableId)
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId(columnId)
                                                .setTableId(tableId)
                                                .setName("email")
                                                .setDataType("VARCHAR")
                                                .setLengthScale("255")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setColumnId(columnId)
                .build();

        given(columnService.deleteColumn(request)).willReturn(Mono.empty());

        // When & Then
        webTestClient.method(HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/columns/{columnId}", columnId)
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

