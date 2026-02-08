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
import com.schemafy.core.erd.controller.dto.request.ChangeColumnNameRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeColumnTypeRequest;
import com.schemafy.core.erd.controller.dto.request.CreateColumnRequest;
import com.schemafy.core.erd.docs.ColumnApiSnippets;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ColumnController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class ColumnControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CreateColumnUseCase createColumnUseCase;

  @MockitoBean
  private GetColumnUseCase getColumnUseCase;

  @MockitoBean
  private GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @MockitoBean
  private ChangeColumnNameUseCase changeColumnNameUseCase;

  @MockitoBean
  private ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @MockitoBean
  private ChangeColumnMetaUseCase changeColumnMetaUseCase;

  @MockitoBean
  private ChangeColumnPositionUseCase changeColumnPositionUseCase;

  @MockitoBean
  private DeleteColumnUseCase deleteColumnUseCase;

  @Test
  @DisplayName("컬럼 생성 API 문서화")
  void createColumn() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";

    CreateColumnRequest request = new CreateColumnRequest(
        tableId,
        "user_id",
        "BIGINT",
        20,
        null,
        null,
        true,
        null,
        null,
        "사용자 ID");

    CreateColumnResult result = new CreateColumnResult(
        columnId,
        "user_id",
        "BIGINT",
        new ColumnLengthScale(20, null, null),
        1,
        true,
        null,
        null,
        "사용자 ID");

    given(createColumnUseCase.createColumn(any(CreateColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/columns")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(columnId)
        .consumeWith(document("column-create",
            ColumnApiSnippets.createColumnRequestHeaders(),
            ColumnApiSnippets.createColumnRequest(),
            ColumnApiSnippets.createColumnResponseHeaders(),
            ColumnApiSnippets.createColumnResponse()));
  }

  @Test
  @DisplayName("컬럼 조회 API 문서화")
  void getColumn() {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    Column column = new Column(
        columnId,
        tableId,
        "user_id",
        "BIGINT",
        new ColumnLengthScale(20, null, null),
        1,
        true,
        null,
        null,
        "사용자 ID");

    given(getColumnUseCase.getColumn(any(GetColumnQuery.class)))
        .willReturn(Mono.just(column));

    webTestClient.get()
        .uri(API_BASE_PATH + "/columns/{columnId}", columnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(columnId)
        .consumeWith(document("column-get",
            ColumnApiSnippets.getColumnPathParameters(),
            ColumnApiSnippets.getColumnRequestHeaders(),
            ColumnApiSnippets.getColumnResponseHeaders(),
            ColumnApiSnippets.getColumnResponse()));
  }

  @Test
  @DisplayName("테이블별 컬럼 목록 조회 API 문서화")
  void getColumnsByTableId() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String columnId1 = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String columnId2 = "06D6W3DAHD51T5NJPK29Q6BCRB";

    Column column1 = new Column(columnId1, tableId, "id", "BIGINT",
        new ColumnLengthScale(20, null, null), 1, true, null, null, "PK");
    Column column2 = new Column(columnId2, tableId, "name", "VARCHAR",
        new ColumnLengthScale(255, null, null), 2, false, "utf8mb4", "utf8mb4_general_ci", "이름");

    given(getColumnsByTableIdUseCase.getColumnsByTableId(any(GetColumnsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(column1, column2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/columns", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(columnId1)
        .consumeWith(document("column-list-by-table",
            ColumnApiSnippets.getColumnsByTableIdPathParameters(),
            ColumnApiSnippets.getColumnsByTableIdRequestHeaders(),
            ColumnApiSnippets.getColumnsByTableIdResponseHeaders(),
            ColumnApiSnippets.getColumnsByTableIdResponse()));
  }

  @Test
  @DisplayName("컬럼 이름 변경 API 문서화")
  void changeColumnName() throws Exception {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeColumnNameRequest request = new ChangeColumnNameRequest("new_user_id");

    given(changeColumnNameUseCase.changeColumnName(any(ChangeColumnNameCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/name", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("column-change-name",
            ColumnApiSnippets.changeColumnNamePathParameters(),
            ColumnApiSnippets.changeColumnNameRequestHeaders(),
            ColumnApiSnippets.changeColumnNameRequest(),
            ColumnApiSnippets.changeColumnNameResponseHeaders(),
            ColumnApiSnippets.changeColumnNameResponse()));
  }

  @Test
  @DisplayName("컬럼 타입 변경 API 문서화")
  void changeColumnType() throws Exception {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeColumnTypeRequest request = new ChangeColumnTypeRequest("VARCHAR", 100, null, null);

    given(changeColumnTypeUseCase.changeColumnType(any(ChangeColumnTypeCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/type", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("column-change-type",
            ColumnApiSnippets.changeColumnTypePathParameters(),
            ColumnApiSnippets.changeColumnTypeRequestHeaders(),
            ColumnApiSnippets.changeColumnTypeRequest(),
            ColumnApiSnippets.changeColumnTypeResponseHeaders(),
            ColumnApiSnippets.changeColumnTypeResponse()));
  }

  @Test
  @DisplayName("컬럼 메타 변경 API 문서화")
  void changeColumnMeta() throws Exception {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeColumnMetaUseCase.changeColumnMeta(any(ChangeColumnMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/meta", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"autoIncrement":false,"charset":"utf8mb4","collation":"utf8mb4_general_ci","comment":"변경된 코멘트"}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("column-change-meta",
            ColumnApiSnippets.changeColumnMetaPathParameters(),
            ColumnApiSnippets.changeColumnMetaRequestHeaders(),
            ColumnApiSnippets.changeColumnMetaRequest(),
            ColumnApiSnippets.changeColumnMetaResponseHeaders(),
            ColumnApiSnippets.changeColumnMetaResponse()));
  }

  @Test
  @DisplayName("컬럼 메타 변경 시 null을 전송하면 클리어된다")
  void changeColumnMetaWithExplicitNull() {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeColumnMetaUseCase.changeColumnMeta(any(ChangeColumnMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/meta", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"charset":null}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true);
  }

  @Test
  @DisplayName("컬럼 메타 변경 시 필드를 생략하면 기존 값이 유지된다")
  void changeColumnMetaWithAbsentFields() {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeColumnMetaUseCase.changeColumnMeta(any(ChangeColumnMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/meta", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"charset":"utf8mb4"}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true);
  }

  @Test
  @DisplayName("컬럼 위치 변경 API 문서화")
  void changeColumnPosition() throws Exception {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeColumnPositionRequest request = new ChangeColumnPositionRequest(3);

    given(changeColumnPositionUseCase.changeColumnPosition(any(ChangeColumnPositionCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/columns/{columnId}/position", columnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("column-change-position",
            ColumnApiSnippets.changeColumnPositionPathParameters(),
            ColumnApiSnippets.changeColumnPositionRequestHeaders(),
            ColumnApiSnippets.changeColumnPositionRequest(),
            ColumnApiSnippets.changeColumnPositionResponseHeaders(),
            ColumnApiSnippets.changeColumnPositionResponse()));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("컬럼 삭제 API 문서화")
  void deleteColumn() {
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/columns/{columnId}", columnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("column-delete",
            ColumnApiSnippets.deleteColumnPathParameters(),
            ColumnApiSnippets.deleteColumnRequestHeaders(),
            ColumnApiSnippets.deleteColumnResponseHeaders(),
            ColumnApiSnippets.deleteColumnResponse()));
  }

}
