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
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.erd.service.IndexService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import validation.Validation;

@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("IndexController RestDocs 테스트")
class IndexControllerTest {

    private static final String API_BASE_PATH = ApiPath.AUTH_API
            .replace("{version}", "v1.0");

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
        // Given
        Validation.CreateIndexRequest request = Validation.CreateIndexRequest
                .newBuilder()
                .setIndex(Validation.Index.newBuilder()
                        .setId("01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .setTableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                        .setName("IDX_users_email")
                        .setType(Validation.IndexType.BTREE)
                        .setComment("이메일 인덱스")
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
                Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAW", Map.of("01ARZ3NDEKTSV4RRFFQ69G5FAV", "01ARZ3NDEKTSV4RRFFQ69G5FAX")),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                AffectedMappingResponse.PropagatedEntities.empty());

        when(indexService.createIndex(any(Validation.CreateIndexRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/indexes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.indexes['01ARZ3NDEKTSV4RRFFQ69G5FAW']['01ARZ3NDEKTSV4RRFFQ69G5FAV']")
                .isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAX")
                .consumeWith(document("index-create",
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
                                fieldWithPath("result.indexes").description(
                                        "인덱스 ID 매핑 (Table BE ID -> { FE ID -> BE ID })"),
                                fieldWithPath("result.indexes.01ARZ3NDEKTSV4RRFFQ69G5FAW")
                                        .description("테이블별 인덱스 ID 매핑"),
                                fieldWithPath(
                                        "result.indexes.01ARZ3NDEKTSV4RRFFQ69G5FAW.01ARZ3NDEKTSV4RRFFQ69G5FAV")
                                                .description(
                                                        "백엔드에서 생성된 인덱스 ID"),
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
    @DisplayName("인덱스 단건 조회 API 문서화")
    void getIndex() throws Exception {
        // Given
        String indexId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Index index = Index.builder()
                .tableId("01ARZ3NDEKTSV4RRFFQ69G5FAW")
                .name("IDX_users_email")
                .type("BTREE")
                .comment("이메일 검색을 위한 B-Tree 인덱스")
                .build();
        ReflectionTestUtils.setField(index, "id", indexId);

        when(indexService.getIndex(indexId))
                .thenReturn(Mono.just(index));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/indexes/{indexId}", indexId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(indexId)
                .jsonPath("$.result.name").isEqualTo("IDX_users_email")
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
                                        .optional()
                                        .description("인덱스 설명"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .optional()
                                        .description("삭제 일시 (Soft Delete)"))));
    }

    @Test
    @DisplayName("테이블별 인덱스 목록 조회 API 문서화")
    void getIndexesByTableId() throws Exception {
        // Given
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Index index1 = Index.builder()
                .tableId(tableId)
                .name("IDX_users_email")
                .type("BTREE")
                .comment("User email index")
                .build();
        Index index2 = Index.builder()
                .tableId(tableId)
                .name("IDX_users_name")
                .type("BTREE")
                .comment("User name index")
                .build();
        ReflectionTestUtils.setField(index1, "id", "index-1");
        ReflectionTestUtils.setField(index2, "id", "index-2");

        when(indexService.getIndexesByTableId(tableId))
                .thenReturn(Flux.just(index1, index2));

        // When & Then
        webTestClient.get()
                .uri(API_BASE_PATH + "/indexes/table/{tableId}", tableId)
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result").isArray()
                .jsonPath("$.result.length()").isEqualTo(2)
                .consumeWith(document("index-get-by-table",
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
                                        .optional()
                                        .description("인덱스 설명"),
                                fieldWithPath("result[].createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result[].updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result[].deletedAt")
                                        .optional()
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("인덱스 이름 변경 API 문서화")
    void updateIndexName() throws Exception {
        // Given
        String indexId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.ChangeIndexNameRequest request = Validation.ChangeIndexNameRequest
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
                                        .addIndexes(Validation.Index.newBuilder()
                                                .setId(indexId)
                                                .setTableId(tableId)
                                                .setName("IDX_users_email")
                                                .setType(Validation.IndexType.BTREE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setIndexId(indexId)
                .setNewName("IDX_users_email_new")
                .build();

        Index updatedIndex = Index.builder()
                .tableId("table-id")
                .name("IDX_users_email_new")
                .type("BTREE")
                .comment("Updated email index")
                .build();
        ReflectionTestUtils.setField(updatedIndex, "id", indexId);

        when(indexService
                .updateIndexName(any(Validation.ChangeIndexNameRequest.class)))
                        .thenReturn(Mono.just(updatedIndex));

        // When & Then
        webTestClient.put()
                .uri(API_BASE_PATH + "/indexes/{indexId}/name", indexId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("IDX_users_email_new")
                .consumeWith(document("index-update-name",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID")),
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
                                        .optional()
                                        .description("인덱스 설명"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("인덱스에 컬럼 추가 API 문서화")
    void addColumnToIndex() throws Exception {
        // Given
        String indexId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        Validation.AddColumnToIndexRequest request = Validation.AddColumnToIndexRequest
                .newBuilder()
                .setIndexColumn(Validation.IndexColumn.newBuilder()
                        .setId("ic-fe-id")
                        .setIndexId(indexId)
                        .setColumnId("column-id")
                        .setSeqNo(1)
                        .setSortDir(Validation.IndexSortDir.ASC)
                        .build())
                .build();

        IndexColumn indexColumn = IndexColumn.builder()
                .indexId(indexId)
                .columnId("column-id")
                .seqNo(1)
                .sortDir("ASC")
                .build();
        ReflectionTestUtils.setField(indexColumn, "id", "ic-be-id");

        when(indexService.addColumnToIndex(
                any(Validation.AddColumnToIndexRequest.class)))
                        .thenReturn(Mono.just(indexColumn));

        // When & Then
        webTestClient.post()
                .uri(API_BASE_PATH + "/indexes/{indexId}/columns", indexId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toJson(request))
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.indexId").isEqualTo(indexId)
                .consumeWith(document("index-add-column",
                        pathParameters(
                                parameterWithName("indexId")
                                        .description("인덱스 ID")),
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
                                        .description("정렬 방향 (ASC, DESC)"),
                                fieldWithPath("result.createdAt")
                                        .description("생성 일시"),
                                fieldWithPath("result.updatedAt")
                                        .description("수정 일시"),
                                fieldWithPath("result.deletedAt")
                                        .description("삭제 일시"))));
    }

    @Test
    @DisplayName("인덱스에서 컬럼 제거 API 문서화")
    void removeColumnFromIndex() throws Exception {
        // Given
        String indexId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String columnId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.RemoveColumnFromIndexRequest request = Validation.RemoveColumnFromIndexRequest
                .newBuilder()
                .setIndexColumnId("ic-id-123")
                .build();

        when(indexService.removeColumnFromIndex(
                any(Validation.RemoveColumnFromIndexRequest.class)))
                        .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/indexes/{indexId}/columns/{columnId}",
                        indexId, columnId)
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
    @DisplayName("인덱스 삭제 API 문서화")
    void deleteIndex() throws Exception {
        // Given
        String indexId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String schemaId = "01ARZ3NDEKTSV4RRFFQ69G5FA2";
        String tableId = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
        Validation.DeleteIndexRequest request = Validation.DeleteIndexRequest
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
                                        .addIndexes(Validation.Index.newBuilder()
                                                .setId(indexId)
                                                .setTableId(tableId)
                                                .setName("IDX_users_email")
                                                .setType(Validation.IndexType.BTREE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setSchemaId(schemaId)
                .setTableId(tableId)
                .setIndexId(indexId)
                .build();

        when(indexService.deleteIndex(any(Validation.DeleteIndexRequest.class)))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(API_BASE_PATH + "/indexes/{indexId}", indexId)
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
