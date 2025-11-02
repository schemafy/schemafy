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
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;
import com.schemafy.core.erd.service.RelationshipService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("RelationshipController RestDocs 테스트")
class RelationshipControllerTest {

    private static final String API_BASE_PATH = ApiPath.AUTH_API
            .replace("{version}", "v1.0");

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
        // Given
        Validation.CreateRelationshipRequest request = Validation.CreateRelationshipRequest
                .newBuilder()
                .setRelationship(Validation.Relationship.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .setSrcTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                        .setTgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                        .setName("FK_users_roles")
                        .setKind(Validation.RelationshipKind.IDENTIFYING)
                        .setCardinality(
                                Validation.RelationshipCardinality.ONE_TO_MANY)
                        .setOnDelete(Validation.RelationshipOnDelete.CASCADE)
                        .setOnUpdate(
                                Validation.RelationshipOnUpdate.CASCADE_UPDATE)
                        .build())
                .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FA2")
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
                                        .setComment("사용자 테이블")
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                        .setSchemaId("01ARZ3NDEKTSV4RRFFQ69G5FA2")
                                        .setName("roles")
                                        .setComment("역할 테이블")
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
                Collections.emptyMap(),
                Collections.emptyMap(),
                Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAW", Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAY")),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        when(relationshipService.createRelationship(any()))
                        .thenReturn(Mono.just(mockResponse));

        String requestJson = toJson(request);

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/relationships")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.relationships['01ARZ3NDEKTSV4RRFFQ69G5FAW']['01ARZ3NDEKTSV4RRFFQ69G5FAV']")
                .isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAY")
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
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.schemas")
                                        .description("스키마 ID 매핑"),
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
                                        .description(
                                                "관계 ID 매핑 (Source Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath(
                                        "result.relationships.01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                                .description(
                                                        "소스 테이블별 관계 ID 매핑"),
                                fieldWithPath(
                                        "result.relationships.01ARZ3NDEKTSV4RRFFQ69G5FAW.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                                .description("백엔드에서 생성된 관계 ID"),
                                fieldWithPath("result.relationshipColumns")
                                        .description("관계 컬럼 ID 매핑"),
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
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Relationship relationship = Relationship.builder()
                .srcTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .tgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                .name("FK_users_roles")
                .kind("IDENTIFYING")
                .cardinality("ONE_TO_MANY")
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .extra("사용자와 역할 간의 식별 관계")
                .build();
        ReflectionTestUtils.setField(relationship, "id", relationshipId);

        when(relationshipService.getRelationship(relationshipId))
                .thenReturn(Mono.just(relationship));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}",
                        relationshipId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(relationshipId)
                .jsonPath("$.result.name").isEqualTo("FK_users_roles")
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
                                        .optional()
                                        .description("추가 정보 (JSON 형식)"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블별 관계 목록 조회 API 문서화")
    void getRelationshipsByTableId() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Relationship relationship1 = Relationship.builder()
                .srcTableId(tableId)
                .tgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("FK_users_roles")
                .kind("IDENTIFYING")
                .cardinality("ONE_TO_MANY")
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .extra("사용자-역할 다대다 관계")
                .build();
        Relationship relationship2 = Relationship.builder()
                .srcTableId(tableId)
                .tgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                .name("FK_users_departments")
                .kind("NON_IDENTIFYING")
                .cardinality("MANY_TO_ONE")
                .onDelete("SET_NULL")
                .onUpdate("CASCADE")
                .extra("사용자-부서 다대일 관계")
                .build();
        ReflectionTestUtils.setField(relationship1, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAY");
        ReflectionTestUtils.setField(relationship2, "id", "01ARZ3NDEKTSV4RRFFQ69G5FAZ");

        when(relationshipService.getRelationshipsByTableId(tableId))
                .thenReturn(Flux.just(relationship1, relationship2));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/relationships/table/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(2)
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
                                        .optional()
                                        .description("추가 정보"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result[].deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("관계 이름 변경 API 문서화")
    void updateRelationshipName() throws Exception {
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        Validation.ChangeRelationshipNameRequest request = Validation.ChangeRelationshipNameRequest
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
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addRelationships(Validation.Relationship.newBuilder()
                                                .setId(relationshipId)
                                                .setSrcTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                                .setTgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                                .setName("FK_users_roles")
                                                .setKind(Validation.RelationshipKind.IDENTIFYING)
                                                .setCardinality(Validation.RelationshipCardinality.ONE_TO_MANY)
                                                .build())
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                        .setSchemaId(schemaId)
                                        .setName("roles")
                                        .setComment("역할 테이블")
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setRelationshipId(relationshipId)
                .setNewName("FK_users_roles_new")
                .build();

        Relationship updatedRelationship = Relationship.builder()
                .srcTableId("src-table-id")
                .tgtTableId("tgt-table-id")
                .name("FK_users_roles_new")
                .kind("IDENTIFYING")
                .cardinality("ONE_TO_MANY")
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .extra(null)
                .build();
        ReflectionTestUtils.setField(updatedRelationship, "id", relationshipId);

        when(relationshipService.updateRelationshipName(
                any(Validation.ChangeRelationshipNameRequest.class)))
                        .thenReturn(Mono.just(updatedRelationship));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}/name",
                        relationshipId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("FK_users_roles_new")
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
                                        .optional()
                                        .description("추가 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("관계 카디널리티 변경 API 문서화")
    void updateRelationshipCardinality() throws Exception {
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        Validation.ChangeRelationshipCardinalityRequest request = Validation.ChangeRelationshipCardinalityRequest
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
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addRelationships(Validation.Relationship.newBuilder()
                                                .setId(relationshipId)
                                                .setSrcTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                                .setTgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                                .setName("FK_users_roles")
                                                .setKind(Validation.RelationshipKind.IDENTIFYING)
                                                .setCardinality(Validation.RelationshipCardinality.ONE_TO_MANY)
                                                .build())
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                        .setSchemaId(schemaId)
                                        .setName("roles")
                                        .setComment("역할 테이블")
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setRelationshipId(relationshipId)
                .setCardinality(Validation.RelationshipCardinality.ONE_TO_ONE)
                .build();

        Relationship updatedRelationship = Relationship.builder()
                .srcTableId("src-table-id")
                .tgtTableId("tgt-table-id")
                .name("FK_users_roles")
                .kind("IDENTIFYING")
                .cardinality("ONE_TO_ONE")
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .extra(null)
                .build();
        ReflectionTestUtils.setField(updatedRelationship, "id", relationshipId);

        when(relationshipService.updateRelationshipCardinality(
                any(Validation.ChangeRelationshipCardinalityRequest.class)))
                        .thenReturn(Mono.just(updatedRelationship));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH
                        + "/relationships/{relationshipId}/cardinality",
                        relationshipId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.cardinality").isEqualTo("ONE_TO_ONE")
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
                                        .optional()
                                        .description("추가 정보"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("관계에 컬럼 추가 API 문서화")
    void addColumnToRelationship() throws Exception {
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Validation.AddColumnToRelationshipRequest request = Validation.AddColumnToRelationshipRequest
                .newBuilder()
                .setRelationshipColumn(
                        Validation.RelationshipColumn.newBuilder()
                                .setId("rc-fe-id")
                                .setRelationshipId(relationshipId)
                                .setFkColumnId("fk-column-id")
                                .setRefColumnId("ref-column-id")
                                .setSeqNo(1)
                                .build())
                .build();

        RelationshipColumn relationshipColumn = RelationshipColumn.builder()
                .relationshipId(relationshipId)
                .srcColumnId("fk-column-id")
                .tgtColumnId("ref-column-id")
                .seqNo(1)
                .build();
        ReflectionTestUtils.setField(relationshipColumn, "id", "rc-be-id");

        when(relationshipService.addColumnToRelationship(
                any(Validation.AddColumnToRelationshipRequest.class)))
                        .thenReturn(Mono.just(relationshipColumn));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/relationships/{relationshipId}/columns",
                        relationshipId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.relationshipId").isEqualTo(relationshipId)
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
                        responseFields(
                                fieldWithPath("success")
                                        .description("요청 성공 여부"),
                                fieldWithPath("result")
                                        .description("생성된 관계 컬럼 정보"),
                                fieldWithPath("result.id")
                                        .description("관계 컬럼 ID"),
                                fieldWithPath("result.relationshipId")
                                        .description("관계 ID"),
                                fieldWithPath("result.srcColumnId")
                                        .description("소스(FK) 컬럼 ID"),
                                fieldWithPath("result.tgtColumnId")
                                        .description("타겟(참조) 컬럼 ID"),
                                fieldWithPath("result.seqNo")
                                        .description("순서 번호"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("관계에서 컬럼 제거 API 문서화")
    void removeColumnFromRelationship() throws Exception {
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.RemoveColumnFromRelationshipRequest request = Validation.RemoveColumnFromRelationshipRequest
                .newBuilder()
                .setRelationshipColumnId("rc-id-123")
                .build();

        when(relationshipService.removeColumnFromRelationship(
                any(Validation.RemoveColumnFromRelationshipRequest.class)))
                        .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH
                        + "/relationships/{relationshipId}/columns/{columnId}",
                        relationshipId, columnId)
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
        // Given
        String relationshipId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        Validation.DeleteRelationshipRequest request = Validation.DeleteRelationshipRequest
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
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .setSchemaId(schemaId)
                                        .setName("users")
                                        .setComment("사용자 테이블")
                                        .addRelationships(Validation.Relationship.newBuilder()
                                                .setId(relationshipId)
                                                .setSrcTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                                .setTgtTableId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                                .setName("FK_users_roles")
                                                .setKind(Validation.RelationshipKind.IDENTIFYING)
                                                .setCardinality(Validation.RelationshipCardinality.ONE_TO_MANY)
                                                .build())
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                                        .setSchemaId(schemaId)
                                        .setName("roles")
                                        .setComment("역할 테이블")
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setRelationshipId(relationshipId)
                .build();

        when(relationshipService.deleteRelationship(
                any(Validation.DeleteRelationshipRequest.class)))
                        .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/relationships/{relationshipId}",
                        relationshipId)
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
