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
import com.schemafy.core.erd.controller.dto.request.AddConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeConstraintNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintColumnRequest;
import com.schemafy.core.erd.controller.dto.request.CreateConstraintRequest;
import com.schemafy.core.erd.docs.ConstraintApiSnippets;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("ConstraintController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class ConstraintControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CreateConstraintUseCase createConstraintUseCase;

  @MockitoBean
  private GetConstraintUseCase getConstraintUseCase;

  @MockitoBean
  private GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  @MockitoBean
  private ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @MockitoBean
  private DeleteConstraintUseCase deleteConstraintUseCase;

  @MockitoBean
  private GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;

  @MockitoBean
  private AddConstraintColumnUseCase addConstraintColumnUseCase;

  @MockitoBean
  private RemoveConstraintColumnUseCase removeConstraintColumnUseCase;

  @MockitoBean
  private GetConstraintColumnUseCase getConstraintColumnUseCase;

  @MockitoBean
  private ChangeConstraintColumnPositionUseCase changeConstraintColumnPositionUseCase;

  @Test
  @DisplayName("제약조건 생성 API 문서화")
  void createConstraint() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";

    CreateConstraintRequest request = new CreateConstraintRequest(
        tableId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnRequest(columnId, 1)));

    CreateConstraintResult result = new CreateConstraintResult(
        constraintId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null);

    given(createConstraintUseCase.createConstraint(any(CreateConstraintCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/constraints")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(constraintId)
        .consumeWith(document("constraint-create",
            ConstraintApiSnippets.createConstraintRequestHeaders(),
            ConstraintApiSnippets.createConstraintRequest(),
            ConstraintApiSnippets.createConstraintResponseHeaders(),
            ConstraintApiSnippets.createConstraintResponse()));
  }

  @Test
  @DisplayName("제약조건 조회 API 문서화")
  void getConstraint() {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    Constraint constraint = new Constraint(
        constraintId,
        tableId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null);

    given(getConstraintUseCase.getConstraint(any(GetConstraintQuery.class)))
        .willReturn(Mono.just(constraint));

    webTestClient.get()
        .uri(API_BASE_PATH + "/constraints/{constraintId}", constraintId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(constraintId)
        .consumeWith(document("constraint-get",
            ConstraintApiSnippets.getConstraintPathParameters(),
            ConstraintApiSnippets.getConstraintRequestHeaders(),
            ConstraintApiSnippets.getConstraintResponseHeaders(),
            ConstraintApiSnippets.getConstraintResponse()));
  }

  @Test
  @DisplayName("테이블별 제약조건 목록 조회 API 문서화")
  void getConstraintsByTableId() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String constraintId1 = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String constraintId2 = "06D6W4DAHD51T5NJPK29Q6BCRD";

    Constraint constraint1 = new Constraint(constraintId1, tableId, "pk_users",
        ConstraintKind.PRIMARY_KEY, null, null);
    Constraint constraint2 = new Constraint(constraintId2, tableId, "uq_users_email",
        ConstraintKind.UNIQUE, null, null);

    given(getConstraintsByTableIdUseCase.getConstraintsByTableId(any(GetConstraintsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(constraint1, constraint2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/constraints", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(constraintId1)
        .consumeWith(document("constraint-list-by-table",
            ConstraintApiSnippets.getConstraintsByTableIdPathParameters(),
            ConstraintApiSnippets.getConstraintsByTableIdRequestHeaders(),
            ConstraintApiSnippets.getConstraintsByTableIdResponseHeaders(),
            ConstraintApiSnippets.getConstraintsByTableIdResponse()));
  }

  @Test
  @DisplayName("제약조건 이름 변경 API 문서화")
  void changeConstraintName() throws Exception {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeConstraintNameRequest request = new ChangeConstraintNameRequest("pk_users_new");

    given(changeConstraintNameUseCase.changeConstraintName(any(ChangeConstraintNameCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/constraints/{constraintId}/name", constraintId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("constraint-change-name",
            ConstraintApiSnippets.changeConstraintNamePathParameters(),
            ConstraintApiSnippets.changeConstraintNameRequestHeaders(),
            ConstraintApiSnippets.changeConstraintNameRequest(),
            ConstraintApiSnippets.changeConstraintNameResponseHeaders(),
            ConstraintApiSnippets.changeConstraintNameResponse()));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("제약조건 삭제 API 문서화")
  void deleteConstraint() {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(deleteConstraintUseCase.deleteConstraint(any(DeleteConstraintCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/constraints/{constraintId}", constraintId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("constraint-delete",
            ConstraintApiSnippets.deleteConstraintPathParameters(),
            ConstraintApiSnippets.deleteConstraintRequestHeaders(),
            ConstraintApiSnippets.deleteConstraintResponseHeaders(),
            ConstraintApiSnippets.deleteConstraintResponse()));
  }

  @Test
  @DisplayName("제약조건 컬럼 목록 조회 API 문서화")
  void getConstraintColumns() {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String constraintColumnId1 = "06D6W5CAHD51T5NJPK29Q6BCRE";
    String constraintColumnId2 = "06D6W5DAHD51T5NJPK29Q6BCRF";
    String columnId1 = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String columnId2 = "06D6W3DAHD51T5NJPK29Q6BCRB";

    ConstraintColumn constraintColumn1 = new ConstraintColumn(
        constraintColumnId1, constraintId, columnId1, 1);
    ConstraintColumn constraintColumn2 = new ConstraintColumn(
        constraintColumnId2, constraintId, columnId2, 2);

    given(getConstraintColumnsByConstraintIdUseCase.getConstraintColumnsByConstraintId(
        any(GetConstraintColumnsByConstraintIdQuery.class)))
        .willReturn(Mono.just(List.of(constraintColumn1, constraintColumn2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/constraints/{constraintId}/columns", constraintId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(constraintColumnId1)
        .consumeWith(document("constraint-columns-list",
            ConstraintApiSnippets.getConstraintColumnsPathParameters(),
            ConstraintApiSnippets.getConstraintColumnsRequestHeaders(),
            ConstraintApiSnippets.getConstraintColumnsResponseHeaders(),
            ConstraintApiSnippets.getConstraintColumnsResponse()));
  }

  @Test
  @DisplayName("제약조건 컬럼 추가 API 문서화")
  void addConstraintColumn() throws Exception {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String constraintColumnId = "06D6W5CAHD51T5NJPK29Q6BCRE";

    AddConstraintColumnRequest request = new AddConstraintColumnRequest(columnId, 1);

    AddConstraintColumnResult result = new AddConstraintColumnResult(
        constraintColumnId,
        constraintId,
        columnId,
        1,
        List.of());

    given(addConstraintColumnUseCase.addConstraintColumn(any(AddConstraintColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/constraints/{constraintId}/columns", constraintId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(constraintColumnId)
        .consumeWith(document("constraint-column-add",
            ConstraintApiSnippets.addConstraintColumnPathParameters(),
            ConstraintApiSnippets.addConstraintColumnRequestHeaders(),
            ConstraintApiSnippets.addConstraintColumnRequest(),
            ConstraintApiSnippets.addConstraintColumnResponseHeaders(),
            ConstraintApiSnippets.addConstraintColumnResponse()));
  }

  @Test
  @DisplayName("제약조건 컬럼 제거 API 문서화")
  void removeConstraintColumn() {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String constraintColumnId = "06D6W5CAHD51T5NJPK29Q6BCRE";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(removeConstraintColumnUseCase.removeConstraintColumn(any(RemoveConstraintColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/constraints/{constraintId}/columns/{constraintColumnId}",
            constraintId, constraintColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("constraint-column-remove",
            ConstraintApiSnippets.removeConstraintColumnPathParameters(),
            ConstraintApiSnippets.removeConstraintColumnRequestHeaders(),
            ConstraintApiSnippets.removeConstraintColumnResponseHeaders(),
            ConstraintApiSnippets.removeConstraintColumnResponse()));
  }

  @Test
  @DisplayName("제약조건 컬럼 조회 API 문서화")
  void getConstraintColumn() {
    String constraintId = "06D6W4CAHD51T5NJPK29Q6BCRC";
    String constraintColumnId = "06D6W5CAHD51T5NJPK29Q6BCRE";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";

    ConstraintColumn constraintColumn = new ConstraintColumn(
        constraintColumnId, constraintId, columnId, 1);

    given(getConstraintColumnUseCase.getConstraintColumn(any(GetConstraintColumnQuery.class)))
        .willReturn(Mono.just(constraintColumn));

    webTestClient.get()
        .uri(API_BASE_PATH + "/constraint-columns/{constraintColumnId}", constraintColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(constraintColumnId)
        .consumeWith(document("constraint-column-get",
            ConstraintApiSnippets.getConstraintColumnPathParameters(),
            ConstraintApiSnippets.getConstraintColumnRequestHeaders(),
            ConstraintApiSnippets.getConstraintColumnResponseHeaders(),
            ConstraintApiSnippets.getConstraintColumnResponse()));
  }

  @Test
  @DisplayName("제약조건 컬럼 위치 변경 API 문서화")
  void changeConstraintColumnPosition() throws Exception {
    String constraintColumnId = "06D6W5CAHD51T5NJPK29Q6BCRE";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    ChangeConstraintColumnPositionRequest request = new ChangeConstraintColumnPositionRequest(2);

    given(changeConstraintColumnPositionUseCase.changeConstraintColumnPosition(
        any(ChangeConstraintColumnPositionCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/constraint-columns/{constraintColumnId}/position", constraintColumnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("constraint-column-change-position",
            ConstraintApiSnippets.changeConstraintColumnPositionPathParameters(),
            ConstraintApiSnippets.changeConstraintColumnPositionRequestHeaders(),
            ConstraintApiSnippets.changeConstraintColumnPositionRequest(),
            ConstraintApiSnippets.changeConstraintColumnPositionResponseHeaders(),
            ConstraintApiSnippets.changeConstraintColumnPositionResponse()));
  }

}
