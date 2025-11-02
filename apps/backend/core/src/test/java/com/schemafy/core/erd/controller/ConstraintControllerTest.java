package com.schemafy.core.erd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import org.springframework.restdocs.payload.JsonFieldType;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
import com.schemafy.core.erd.service.ConstraintService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("ConstraintController RestDocs 테스트")
class ConstraintControllerTest {

    private static final String API_BASE_PATH = ApiPath.AUTH_API
            .replace("{version}", "v1.0");

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
        // Given
        Validation.CreateConstraintRequest request = Validation.CreateConstraintRequest
                .newBuilder()
                .setConstraint(Validation.Constraint.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .setTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                        .setName("PK_users")
                        .setKind(Validation.ConstraintKind.PRIMARY_KEY)
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
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAW",
                        Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAX")),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        when(constraintService.createConstraint(
                any(Validation.CreateConstraintRequest.class)))
                        .thenReturn(Mono.just(mockResponse));

        // When & Then
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
                        "$.result.constraints['01ARZ3NDEKTSV4RRFFQ69G5FAW']['01ARZ3NDEKTSV4RRFFQ69G5FAV']")
                .isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAX")
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
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.schemas").description(
                                        "스키마 ID 매핑 (FE ID -> BE ID)"),
                                fieldWithPath("result.tables").description(
                                        "테이블 ID 매핑 (FE ID -> BE ID)"),
                                fieldWithPath("result.columns").description(
                                        "컬럼 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.indexes").description(
                                        "인덱스 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.indexColumns")
                                        .description(
                                                "인덱스 컬럼 ID 매핑 (Index BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.constraints").description(
                                        "제약조건 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.constraints.01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .description("테이블별 제약조건 ID 매핑"),
                                fieldWithPath(
                                        "result.constraints.01ARZ3NDEKTSV4RRFFQ69G5FAW.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                                .description(
                                                        "백엔드에서 생성된 제약조건 ID"),
                                fieldWithPath("result.constraintColumns")
                                        .description(
                                                "제약조건 컬럼 ID 매핑 (Constraint BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.relationships")
                                        .description(
                                                "관계 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.relationshipColumns")
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
        // Given
        String constraintId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Constraint constraint = Constraint.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("PK_users")
                .kind("PRIMARY_KEY")
                .build();
        ReflectionTestUtils.setField(constraint, "id", constraintId);

        when(constraintService.getConstraint(constraintId))
                .thenReturn(Mono.just(constraint));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/constraints/{constraintId}",
                        constraintId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(constraintId)
                .jsonPath("$.result.name").isEqualTo("PK_users")
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
                                fieldWithPath("result.checkExpr")
                                        .optional()
                                        .description("CHECK 제약조건 표현식"),
                                fieldWithPath("result.defaultExpr")
                                        .optional()
                                        .description("DEFAULT 표현식"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블별 제약조건 목록 조회 API 문서화")
    void getConstraintsByTableId() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Constraint constraint1 = Constraint.builder()
                .tableId(tableId)
                .name("PK_users")
                .kind("PRIMARY_KEY")
                .build();
        Constraint constraint2 = Constraint.builder()
                .tableId(tableId)
                .name("UK_email")
                .kind("UNIQUE")
                .build();
        ReflectionTestUtils.setField(constraint1, "id", "constraint-1");
        ReflectionTestUtils.setField(constraint2, "id", "constraint-2");

        when(constraintService.getConstraintsByTableId(tableId))
                .thenReturn(Flux.just(constraint1, constraint2));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/constraints/table/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(2)
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
                                fieldWithPath("result[].checkExpr")
                                        .optional()
                                        .description("CHECK 제약조건 표현식"),
                                fieldWithPath("result[].defaultExpr")
                                        .optional()
                                        .description("DEFAULT 표현식"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result[].deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("제약조건 이름 변경 API 문서화")
    void updateConstraintName() throws Exception {
        // Given
        String constraintId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.ChangeConstraintNameRequest request = Validation.ChangeConstraintNameRequest
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
                                        .addConstraints(Validation.Constraint.newBuilder()
                                                .setId(constraintId)
                                                .setTableId(tableId)
                                                .setName("PK_users")
                                                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setConstraintId(constraintId)
                .setNewName("PK_users_new")
                .build();

        Constraint updatedConstraint = Constraint.builder()
                .tableId("table-id")
                .name("PK_users_new")
                .kind("PRIMARY_KEY")
                .build();
        ReflectionTestUtils.setField(updatedConstraint, "id", constraintId);

        when(constraintService.updateConstraintName(
                any(Validation.ChangeConstraintNameRequest.class)))
                        .thenReturn(Mono.just(updatedConstraint));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/constraints/{constraintId}/name",
                        constraintId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("PK_users_new")
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
                                fieldWithPath("result.checkExpr")
                                        .optional()
                                        .description("CHECK 제약조건 표현식"),
                                fieldWithPath("result.defaultExpr")
                                        .optional()
                                        .description("DEFAULT 표현식"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("제약조건에 컬럼 추가 API 문서화")
    void addColumnToConstraint() throws Exception {
        // Given
        String constraintId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Validation.AddColumnToConstraintRequest request = Validation.AddColumnToConstraintRequest
                .newBuilder()
                .setConstraintColumn(Validation.ConstraintColumn.newBuilder()
                        .setId("cc-fe-id")
                        .setConstraintId(constraintId)
                        .setColumnId("column-id")
                        .setSeqNo(1)
                        .build())
                .build();

        ConstraintColumn constraintColumn = ConstraintColumn.builder()
                .constraintId(constraintId)
                .columnId("column-id")
                .seqNo(1)
                .build();
        ReflectionTestUtils.setField(constraintColumn, "id", "cc-be-id");

        when(constraintService.addColumnToConstraint(
                any(Validation.AddColumnToConstraintRequest.class)))
                        .thenReturn(Mono.just(constraintColumn));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/constraints/{constraintId}/columns",
                        constraintId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.constraintId").isEqualTo(constraintId)
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
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("생성된 제약조건 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("제약조건 컬럼 ID"),
                                fieldWithPath("result.constraintId")
                                        .description("제약조건 ID"),
                                fieldWithPath("result.columnId")
                                        .description("컬럼 ID"),
                                fieldWithPath("result.seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("제약조건에서 컬럼 제거 API 문서화")
    void removeColumnFromConstraint() throws Exception {
        // Given
        String constraintId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.RemoveColumnFromConstraintRequest request = Validation.RemoveColumnFromConstraintRequest
                .newBuilder()
                .setConstraintColumnId("cc-id-123")
                .build();

        when(constraintService.removeColumnFromConstraint(
                any(Validation.RemoveColumnFromConstraintRequest.class)))
                        .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH
                        + "/constraints/{constraintId}/columns/{columnId}",
                        constraintId, columnId)
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
    @DisplayName("제약조건 삭제 API 문서화")
    void deleteConstraint() throws Exception {
        // Given
        String constraintId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.DeleteConstraintRequest request = Validation.DeleteConstraintRequest
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
                                        .addConstraints(Validation.Constraint.newBuilder()
                                                .setId(constraintId)
                                                .setTableId(tableId)
                                                .setName("PK_users")
                                                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setConstraintId(constraintId)
                .build();

        when(constraintService.deleteConstraint(
                any(Validation.DeleteConstraintRequest.class)))
                        .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/constraints/{constraintId}",
                        constraintId)
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
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("응답 데이터 (null)"))));
    }

}
