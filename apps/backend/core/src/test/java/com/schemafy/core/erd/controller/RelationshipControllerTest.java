package com.schemafy.core.erd.controller;

import java.util.List;
import java.util.Set;

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
import com.schemafy.core.erd.controller.dto.request.AddRelationshipColumnRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipCardinalityRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipColumnPositionRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipKindRequest;
import com.schemafy.core.erd.controller.dto.request.ChangeRelationshipNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequest;
import com.schemafy.core.erd.docs.RelationshipApiSnippets;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnResult;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("RelationshipController 통합 테스트")
@WithMockCustomUser(roles = "EDITOR")
class RelationshipControllerTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CreateRelationshipUseCase createRelationshipUseCase;

  @MockitoBean
  private GetRelationshipUseCase getRelationshipUseCase;

  @MockitoBean
  private GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  @MockitoBean
  private ChangeRelationshipNameUseCase changeRelationshipNameUseCase;

  @MockitoBean
  private ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @MockitoBean
  private ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;

  @MockitoBean
  private ChangeRelationshipExtraUseCase changeRelationshipExtraUseCase;

  @MockitoBean
  private DeleteRelationshipUseCase deleteRelationshipUseCase;

  @MockitoBean
  private GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @MockitoBean
  private AddRelationshipColumnUseCase addRelationshipColumnUseCase;

  @MockitoBean
  private RemoveRelationshipColumnUseCase removeRelationshipColumnUseCase;

  @MockitoBean
  private GetRelationshipColumnUseCase getRelationshipColumnUseCase;

  @MockitoBean
  private ChangeRelationshipColumnPositionUseCase changeRelationshipColumnPositionUseCase;

  @Test
  @DisplayName("관계 생성 API 문서화")
  void createRelationship() throws Exception {
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";

    CreateRelationshipRequest request = new CreateRelationshipRequest(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY);

    CreateRelationshipResult result = new CreateRelationshipResult(
        relationshipId,
        fkTableId,
        pkTableId,
        "fk_orders_users",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    given(createRelationshipUseCase.createRelationship(any(CreateRelationshipCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, Set.of(fkTableId, pkTableId))));

    webTestClient.post()
        .uri(API_BASE_PATH + "/relationships")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(relationshipId)
        .consumeWith(document("relationship-create",
            RelationshipApiSnippets.createRelationshipRequestHeaders(),
            RelationshipApiSnippets.createRelationshipRequest(),
            RelationshipApiSnippets.createRelationshipResponseHeaders(),
            RelationshipApiSnippets.createRelationshipResponse()));
  }

  @Test
  @DisplayName("관계 조회 API 문서화")
  void getRelationship() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    Relationship relationship = new Relationship(
        relationshipId,
        pkTableId,
        fkTableId,
        "fk_orders_users",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    given(getRelationshipUseCase.getRelationship(any(GetRelationshipQuery.class)))
        .willReturn(Mono.just(relationship));

    webTestClient.get()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}", relationshipId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(relationshipId)
        .consumeWith(document("relationship-get",
            RelationshipApiSnippets.getRelationshipPathParameters(),
            RelationshipApiSnippets.getRelationshipRequestHeaders(),
            RelationshipApiSnippets.getRelationshipResponseHeaders(),
            RelationshipApiSnippets.getRelationshipResponse()));
  }

  @Test
  @DisplayName("테이블별 관계 목록 조회 API 문서화")
  void getRelationshipsByTableId() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";
    String relationshipId1 = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String relationshipId2 = "06D6W8DAHD51T5NJPK29Q6BCRL";

    Relationship relationship1 = new Relationship(
        relationshipId1, pkTableId, tableId, "fk_orders_users",
        RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
    Relationship relationship2 = new Relationship(
        relationshipId2, tableId, pkTableId, "fk_order_items_orders",
        RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);

    given(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(any(GetRelationshipsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(relationship1, relationship2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/relationships", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(relationshipId1)
        .consumeWith(document("relationship-list-by-table",
            RelationshipApiSnippets.getRelationshipsByTableIdPathParameters(),
            RelationshipApiSnippets.getRelationshipsByTableIdRequestHeaders(),
            RelationshipApiSnippets.getRelationshipsByTableIdResponseHeaders(),
            RelationshipApiSnippets.getRelationshipsByTableIdResponse()));
  }

  @Test
  @DisplayName("관계 이름 변경 API 문서화")
  void changeRelationshipName() throws Exception {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    ChangeRelationshipNameRequest request = new ChangeRelationshipNameRequest("fk_orders_users_new");

    given(changeRelationshipNameUseCase.changeRelationshipName(any(ChangeRelationshipNameCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/name", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-change-name",
            RelationshipApiSnippets.changeRelationshipNamePathParameters(),
            RelationshipApiSnippets.changeRelationshipNameRequestHeaders(),
            RelationshipApiSnippets.changeRelationshipNameRequest(),
            RelationshipApiSnippets.changeRelationshipNameResponseHeaders(),
            RelationshipApiSnippets.changeRelationshipNameResponse()));
  }

  @Test
  @DisplayName("관계 종류 변경 API 문서화")
  void changeRelationshipKind() throws Exception {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    ChangeRelationshipKindRequest request = new ChangeRelationshipKindRequest(RelationshipKind.IDENTIFYING);

    given(changeRelationshipKindUseCase.changeRelationshipKind(any(ChangeRelationshipKindCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/kind", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-change-kind",
            RelationshipApiSnippets.changeRelationshipKindPathParameters(),
            RelationshipApiSnippets.changeRelationshipKindRequestHeaders(),
            RelationshipApiSnippets.changeRelationshipKindRequest(),
            RelationshipApiSnippets.changeRelationshipKindResponseHeaders(),
            RelationshipApiSnippets.changeRelationshipKindResponse()));
  }

  @Test
  @DisplayName("관계 카디널리티 변경 API 문서화")
  void changeRelationshipCardinality() throws Exception {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    ChangeRelationshipCardinalityRequest request = new ChangeRelationshipCardinalityRequest(Cardinality.ONE_TO_ONE);

    given(changeRelationshipCardinalityUseCase.changeRelationshipCardinality(
        any(ChangeRelationshipCardinalityCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/cardinality", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-change-cardinality",
            RelationshipApiSnippets.changeRelationshipCardinalityPathParameters(),
            RelationshipApiSnippets.changeRelationshipCardinalityRequestHeaders(),
            RelationshipApiSnippets.changeRelationshipCardinalityRequest(),
            RelationshipApiSnippets.changeRelationshipCardinalityResponseHeaders(),
            RelationshipApiSnippets.changeRelationshipCardinalityResponse()));
  }

  @Test
  @DisplayName("관계 프론트엔드 메타데이터 변경 API 문서화")
  void changeRelationshipExtra() throws Exception {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    given(changeRelationshipExtraUseCase.changeRelationshipExtra(any(ChangeRelationshipExtraCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/extra", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"extra":{"position":{"x":210,"y":140},"color":"#f97316","lineStyle":{"dash":[4,2]}}}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-change-extra",
            RelationshipApiSnippets.changeRelationshipExtraPathParameters(),
            RelationshipApiSnippets.changeRelationshipExtraRequestHeaders(),
            RelationshipApiSnippets.changeRelationshipExtraRequest(),
            RelationshipApiSnippets.changeRelationshipExtraResponseHeaders(),
            RelationshipApiSnippets.changeRelationshipExtraResponse()));
  }

  @Test
  @DisplayName("관계 추가정보 변경 API는 extra 객체를 허용한다")
  void changeRelationshipExtraAcceptsObjectValue() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    given(changeRelationshipExtraUseCase.changeRelationshipExtra(any(ChangeRelationshipExtraCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/extra", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"extra":{"position":{"x":60,"y":90},"color":"#6366f1"}}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray();

    then(changeRelationshipExtraUseCase).should().changeRelationshipExtra(
        new ChangeRelationshipExtraCommand(relationshipId, "{\"position\":{\"x\":60,\"y\":90},\"color\":\"#6366f1\"}"));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("관계 삭제 API 문서화")
  void deleteRelationship() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    given(deleteRelationshipUseCase.deleteRelationship(any(DeleteRelationshipCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}", relationshipId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-delete",
            RelationshipApiSnippets.deleteRelationshipPathParameters(),
            RelationshipApiSnippets.deleteRelationshipRequestHeaders(),
            RelationshipApiSnippets.deleteRelationshipResponseHeaders(),
            RelationshipApiSnippets.deleteRelationshipResponse()));
  }

  @Test
  @DisplayName("관계 컬럼 목록 조회 API 문서화")
  void getRelationshipColumns() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String relationshipColumnId1 = "06D6W9CAHD51T5NJPK29Q6BCRM";
    String relationshipColumnId2 = "06D6W9DAHD51T5NJPK29Q6BCRN";
    String pkColumnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String fkColumnId = "06D6W3DAHD51T5NJPK29Q6BCRB";

    RelationshipColumn relationshipColumn1 = new RelationshipColumn(
        relationshipColumnId1, relationshipId, pkColumnId, fkColumnId, 1);
    RelationshipColumn relationshipColumn2 = new RelationshipColumn(
        relationshipColumnId2, relationshipId, "06D6W3EAHD51T5NJPK29Q6BCRC", "06D6W3FAHD51T5NJPK29Q6BCRD", 2);

    given(getRelationshipColumnsByRelationshipIdUseCase.getRelationshipColumnsByRelationshipId(
        any(GetRelationshipColumnsByRelationshipIdQuery.class)))
        .willReturn(Mono.just(List.of(relationshipColumn1, relationshipColumn2)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/columns", relationshipId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(relationshipColumnId1)
        .consumeWith(document("relationship-columns-list",
            RelationshipApiSnippets.getRelationshipColumnsPathParameters(),
            RelationshipApiSnippets.getRelationshipColumnsRequestHeaders(),
            RelationshipApiSnippets.getRelationshipColumnsResponseHeaders(),
            RelationshipApiSnippets.getRelationshipColumnsResponse()));
  }

  @Test
  @DisplayName("관계 컬럼 추가 API 문서화")
  void addRelationshipColumn() throws Exception {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";
    String pkColumnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String fkColumnId = "06D6W3DAHD51T5NJPK29Q6BCRB";
    String relationshipColumnId = "06D6W9CAHD51T5NJPK29Q6BCRM";

    AddRelationshipColumnRequest request = new AddRelationshipColumnRequest(pkColumnId, fkColumnId, 1);

    AddRelationshipColumnResult result = new AddRelationshipColumnResult(
        relationshipColumnId,
        relationshipId,
        pkColumnId,
        fkColumnId,
        1);

    given(addRelationshipColumnUseCase.addRelationshipColumn(any(AddRelationshipColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, Set.of(fkTableId, pkTableId))));

    webTestClient.post()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/columns", relationshipId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(relationshipColumnId)
        .consumeWith(document("relationship-column-add",
            RelationshipApiSnippets.addRelationshipColumnPathParameters(),
            RelationshipApiSnippets.addRelationshipColumnRequestHeaders(),
            RelationshipApiSnippets.addRelationshipColumnRequest(),
            RelationshipApiSnippets.addRelationshipColumnResponseHeaders(),
            RelationshipApiSnippets.addRelationshipColumnResponse()));
  }

  @Test
  @DisplayName("관계 컬럼 제거 API 문서화")
  void removeRelationshipColumn() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String relationshipColumnId = "06D6W9CAHD51T5NJPK29Q6BCRM";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    given(removeRelationshipColumnUseCase.removeRelationshipColumn(any(RemoveRelationshipColumnCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/relationships/{relationshipId}/columns/{relationshipColumnId}",
            relationshipId, relationshipColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-column-remove",
            RelationshipApiSnippets.removeRelationshipColumnPathParameters(),
            RelationshipApiSnippets.removeRelationshipColumnRequestHeaders(),
            RelationshipApiSnippets.removeRelationshipColumnResponseHeaders(),
            RelationshipApiSnippets.removeRelationshipColumnResponse()));
  }

  @Test
  @DisplayName("관계 컬럼 조회 API 문서화")
  void getRelationshipColumn() {
    String relationshipId = "06D6W8CAHD51T5NJPK29Q6BCRK";
    String relationshipColumnId = "06D6W9CAHD51T5NJPK29Q6BCRM";
    String pkColumnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String fkColumnId = "06D6W3DAHD51T5NJPK29Q6BCRB";

    RelationshipColumn relationshipColumn = new RelationshipColumn(
        relationshipColumnId, relationshipId, pkColumnId, fkColumnId, 1);

    given(getRelationshipColumnUseCase.getRelationshipColumn(any(GetRelationshipColumnQuery.class)))
        .willReturn(Mono.just(relationshipColumn));

    webTestClient.get()
        .uri(API_BASE_PATH + "/relationship-columns/{relationshipColumnId}", relationshipColumnId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(relationshipColumnId)
        .consumeWith(document("relationship-column-get",
            RelationshipApiSnippets.getRelationshipColumnPathParameters(),
            RelationshipApiSnippets.getRelationshipColumnRequestHeaders(),
            RelationshipApiSnippets.getRelationshipColumnResponseHeaders(),
            RelationshipApiSnippets.getRelationshipColumnResponse()));
  }

  @Test
  @DisplayName("관계 컬럼 위치 변경 API 문서화")
  void changeRelationshipColumnPosition() throws Exception {
    String relationshipColumnId = "06D6W9CAHD51T5NJPK29Q6BCRM";
    String fkTableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String pkTableId = "06D6W2CAHD51T5NJPK29Q6BCRA";

    ChangeRelationshipColumnPositionRequest request = new ChangeRelationshipColumnPositionRequest(2);

    given(changeRelationshipColumnPositionUseCase.changeRelationshipColumnPosition(
        any(ChangeRelationshipColumnPositionCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, Set.of(fkTableId, pkTableId))));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/relationship-columns/{relationshipColumnId}/position", relationshipColumnId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("relationship-column-change-position",
            RelationshipApiSnippets.changeRelationshipColumnPositionPathParameters(),
            RelationshipApiSnippets.changeRelationshipColumnPositionRequestHeaders(),
            RelationshipApiSnippets.changeRelationshipColumnPositionRequest(),
            RelationshipApiSnippets.changeRelationshipColumnPositionResponseHeaders(),
            RelationshipApiSnippets.changeRelationshipColumnPositionResponse()));
  }

}
