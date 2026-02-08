package com.schemafy.core.erd.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.core.erd.controller.dto.request.AddIndexColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexColumnSortDirectionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexNameRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeIndexTypeRequest;
import com.schemafy.core.erd.controller.dto.request.CreateIndexColumnRequest;
import com.schemafy.core.erd.controller.dto.request.CreateIndexRequest;
import com.schemafy.core.erd.docs.IndexApiSnippets;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnResult;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("IndexController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class IndexControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CreateIndexUseCase createIndexUseCase;

  @MockitoBean
  private GetIndexUseCase getIndexUseCase;

  @MockitoBean
  private GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @MockitoBean
  private ChangeIndexNameUseCase changeIndexNameUseCase;

  @MockitoBean
  private ChangeIndexTypeUseCase changeIndexTypeUseCase;

  @MockitoBean
  private DeleteIndexUseCase deleteIndexUseCase;

  @MockitoBean
  private GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  @MockitoBean
  private AddIndexColumnUseCase addIndexColumnUseCase;

  @MockitoBean
  private RemoveIndexColumnUseCase removeIndexColumnUseCase;

  @MockitoBean
  private GetIndexColumnUseCase getIndexColumnUseCase;

  @MockitoBean
  private ChangeIndexColumnPositionUseCase changeIndexColumnPositionUseCase;

  @MockitoBean
  private ChangeIndexColumnSortDirectionUseCase changeIndexColumnSortDirectionUseCase;

  @Test
  @DisplayName("인덱스 생성 API 문서화")
  void createIndex() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";

    CreateIndexRequest request = new CreateIndexRequest(
        tableId,
        "idx_users_email",
        IndexType.BTREE,
        List.of(new CreateIndexColumnRequest(columnId, 1, SortDirection.ASC)));

    CreateIndexResult result = new CreateIndexResult(
        indexId,
        "idx_users_email",
        IndexType.BTREE);

    given(createIndexUseCase.createIndex(any(CreateIndexCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/indexes")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(indexId)
        .consumeWith(document("index-create",
            IndexApiSnippets.createIndexRequestHeaders(),
            IndexApiSnippets.createIndexRequest(),
            IndexApiSnippets.createIndexResponseHeaders(),
            IndexApiSnippets.createIndexResponse()));
  }

  @Test
  @DisplayName("인덱스 조회 API 문서화")
  void getIndex() {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    Index index = new Index(indexId, tableId, "idx_users_email", IndexType.BTREE);

    given(getIndexUseCase.getIndex(any(GetIndexQuery.class)))
        .willReturn(Mono.just(index));

    webTestClient.get()
        .uri(API_BASE_PATH + "/indexes/{indexId}", indexId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(indexId)
        .consumeWith(document("index-get",
            IndexApiSnippets.getIndexPathParameters(),
            IndexApiSnippets.getIndexRequestHeaders(),
            IndexApiSnippets.getIndexResponseHeaders(),
            IndexApiSnippets.getIndexResponse()));
  }

  @Test
  @DisplayName("테이블별 인덱스 목록 조회 API 문서화")
  void getIndexesByTableId() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String indexId1 = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String indexId2 = "06D6W6DAHD51T5NJPK29Q6BCRH";

    Index index1 = new Index(indexId1, tableId, "idx_users_email", IndexType.BTREE);
    Index index2 = new Index(indexId2, tableId, "idx_users_name", IndexType.BTREE);

    given(getIndexesByTableIdUseCase.getIndexesByTableId(any(GetIndexesByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(index1, index2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/indexes", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(indexId1)
        .consumeWith(document("index-list-by-table",
            IndexApiSnippets.getIndexesByTableIdPathParameters(),
            IndexApiSnippets.getIndexesByTableIdRequestHeaders(),
            IndexApiSnippets.getIndexesByTableIdResponseHeaders(),
            IndexApiSnippets.getIndexesByTableIdResponse()));
  }

  @Test
  @DisplayName("인덱스 이름 변경 API 문서화")
  void changeIndexName() throws Exception {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeIndexNameRequest request = new ChangeIndexNameRequest("idx_users_email_new");

    given(changeIndexNameUseCase.changeIndexName(any(ChangeIndexNameCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/indexes/{indexId}/name", indexId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-change-name",
            IndexApiSnippets.changeIndexNamePathParameters(),
            IndexApiSnippets.changeIndexNameRequestHeaders(),
            IndexApiSnippets.changeIndexNameRequest(),
            IndexApiSnippets.changeIndexNameResponseHeaders(),
            IndexApiSnippets.changeIndexNameResponse()));
  }

  @Test
  @DisplayName("인덱스 타입 변경 API 문서화")
  void changeIndexType() throws Exception {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeIndexTypeRequest request = new ChangeIndexTypeRequest(IndexType.HASH);

    given(changeIndexTypeUseCase.changeIndexType(any(ChangeIndexTypeCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/indexes/{indexId}/type", indexId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-change-type",
            IndexApiSnippets.changeIndexTypePathParameters(),
            IndexApiSnippets.changeIndexTypeRequestHeaders(),
            IndexApiSnippets.changeIndexTypeRequest(),
            IndexApiSnippets.changeIndexTypeResponseHeaders(),
            IndexApiSnippets.changeIndexTypeResponse()));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("인덱스 삭제 API 문서화")
  void deleteIndex() {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(deleteIndexUseCase.deleteIndex(any(DeleteIndexCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/indexes/{indexId}", indexId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-delete",
            IndexApiSnippets.deleteIndexPathParameters(),
            IndexApiSnippets.deleteIndexRequestHeaders(),
            IndexApiSnippets.deleteIndexResponseHeaders(),
            IndexApiSnippets.deleteIndexResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 목록 조회 API 문서화")
  void getIndexColumns() {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String indexColumnId1 = "06D6W7CAHD51T5NJPK29Q6BCRI";
    String indexColumnId2 = "06D6W7DAHD51T5NJPK29Q6BCRJ";
    String columnId1 = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String columnId2 = "06D6W3DAHD51T5NJPK29Q6BCRB";

    IndexColumn indexColumn1 = new IndexColumn(
        indexColumnId1, indexId, columnId1, 1, SortDirection.ASC);
    IndexColumn indexColumn2 = new IndexColumn(
        indexColumnId2, indexId, columnId2, 2, SortDirection.DESC);

    given(getIndexColumnsByIndexIdUseCase.getIndexColumnsByIndexId(
        any(GetIndexColumnsByIndexIdQuery.class)))
        .willReturn(Mono.just(List.of(indexColumn1, indexColumn2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/indexes/{indexId}/columns", indexId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(indexColumnId1)
        .consumeWith(document("index-columns-list",
            IndexApiSnippets.getIndexColumnsPathParameters(),
            IndexApiSnippets.getIndexColumnsRequestHeaders(),
            IndexApiSnippets.getIndexColumnsResponseHeaders(),
            IndexApiSnippets.getIndexColumnsResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 추가 API 문서화")
  void addIndexColumn() throws Exception {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String indexColumnId = "06D6W7CAHD51T5NJPK29Q6BCRI";

    AddIndexColumnRequest request = new AddIndexColumnRequest(columnId, 1, SortDirection.ASC);

    AddIndexColumnResult result = new AddIndexColumnResult(
        indexColumnId,
        indexId,
        columnId,
        1,
        SortDirection.ASC);

    given(addIndexColumnUseCase.addIndexColumn(any(AddIndexColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/indexes/{indexId}/columns", indexId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(indexColumnId)
        .consumeWith(document("index-column-add",
            IndexApiSnippets.addIndexColumnPathParameters(),
            IndexApiSnippets.addIndexColumnRequestHeaders(),
            IndexApiSnippets.addIndexColumnRequest(),
            IndexApiSnippets.addIndexColumnResponseHeaders(),
            IndexApiSnippets.addIndexColumnResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 제거 API 문서화")
  void removeIndexColumn() {
    String indexColumnId = "06D6W7CAHD51T5NJPK29Q6BCRI";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(removeIndexColumnUseCase.removeIndexColumn(any(RemoveIndexColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/index-columns/{indexColumnId}", indexColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-column-remove",
            IndexApiSnippets.removeIndexColumnPathParameters(),
            IndexApiSnippets.removeIndexColumnRequestHeaders(),
            IndexApiSnippets.removeIndexColumnResponseHeaders(),
            IndexApiSnippets.removeIndexColumnResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 조회 API 문서화")
  void getIndexColumn() {
    String indexId = "06D6W6CAHD51T5NJPK29Q6BCRG";
    String indexColumnId = "06D6W7CAHD51T5NJPK29Q6BCRI";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";

    IndexColumn indexColumn = new IndexColumn(
        indexColumnId, indexId, columnId, 1, SortDirection.ASC);

    given(getIndexColumnUseCase.getIndexColumn(any(GetIndexColumnQuery.class)))
        .willReturn(Mono.just(indexColumn));

    webTestClient.get()
        .uri(API_BASE_PATH + "/index-columns/{indexColumnId}", indexColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(indexColumnId)
        .consumeWith(document("index-column-get",
            IndexApiSnippets.getIndexColumnPathParameters(),
            IndexApiSnippets.getIndexColumnRequestHeaders(),
            IndexApiSnippets.getIndexColumnResponseHeaders(),
            IndexApiSnippets.getIndexColumnResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 위치 변경 API 문서화")
  void changeIndexColumnPosition() throws Exception {
    String indexColumnId = "06D6W7CAHD51T5NJPK29Q6BCRI";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeIndexColumnPositionRequest request = new ChangeIndexColumnPositionRequest(2);

    given(changeIndexColumnPositionUseCase.changeIndexColumnPosition(
        any(ChangeIndexColumnPositionCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/index-columns/{indexColumnId}/position", indexColumnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-column-change-position",
            IndexApiSnippets.changeIndexColumnPositionPathParameters(),
            IndexApiSnippets.changeIndexColumnPositionRequestHeaders(),
            IndexApiSnippets.changeIndexColumnPositionRequest(),
            IndexApiSnippets.changeIndexColumnPositionResponseHeaders(),
            IndexApiSnippets.changeIndexColumnPositionResponse()));
  }

  @Test
  @DisplayName("인덱스 컬럼 정렬 방향 변경 API 문서화")
  void changeIndexColumnSortDirection() throws Exception {
    String indexColumnId = "06D6W7CAHD51T5NJPK29Q6BCRI";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeIndexColumnSortDirectionRequest request = new ChangeIndexColumnSortDirectionRequest(SortDirection.DESC);

    given(changeIndexColumnSortDirectionUseCase.changeIndexColumnSortDirection(
        any(ChangeIndexColumnSortDirectionCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/index-columns/{indexColumnId}/sort-direction", indexColumnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("index-column-change-sort-direction",
            IndexApiSnippets.changeIndexColumnSortDirectionPathParameters(),
            IndexApiSnippets.changeIndexColumnSortDirectionRequestHeaders(),
            IndexApiSnippets.changeIndexColumnSortDirectionRequest(),
            IndexApiSnippets.changeIndexColumnSortDirectionResponseHeaders(),
            IndexApiSnippets.changeIndexColumnSortDirectionResponse()));
  }

}
