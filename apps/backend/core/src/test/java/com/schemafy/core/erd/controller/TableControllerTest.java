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
import com.schemafy.core.erd.controller.dto.request.ChangeTableNameRequest;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequest;
import com.schemafy.core.erd.docs.TableApiSnippets;
import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTableQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
  private CreateTableUseCase createTableUseCase;

  @MockitoBean
  private GetTableUseCase getTableUseCase;

  @MockitoBean
  private GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;

  @MockitoBean
  private GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @MockitoBean
  private GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  @MockitoBean
  private GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;

  @MockitoBean
  private GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  @MockitoBean
  private GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @MockitoBean
  private GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @MockitoBean
  private GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  @MockitoBean
  private ChangeTableNameUseCase changeTableNameUseCase;

  @MockitoBean
  private ChangeTableMetaUseCase changeTableMetaUseCase;

  @MockitoBean
  private ChangeTableExtraUseCase changeTableExtraUseCase;

  @MockitoBean
  private DeleteTableUseCase deleteTableUseCase;

  @Test
  @DisplayName("테이블 생성 API 문서화")
  void createTable() throws Exception {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    CreateTableRequest request = new CreateTableRequest(
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        null);

    CreateTableResult result = new CreateTableResult(
        tableId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        null);

    given(createTableUseCase.createTable(any(CreateTableCommand.class)))
        .willReturn(Mono.just(MutationResult.of(result, tableId)));

    webTestClient.post()
        .uri(API_BASE_PATH + "/tables")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.data.id").isEqualTo(tableId)
        .consumeWith(document("table-create",
            TableApiSnippets.createTableRequestHeaders(),
            TableApiSnippets.createTableRequest(),
            TableApiSnippets.createTableResponseHeaders(),
            TableApiSnippets.createTableResponse()));
  }

  @Test
  @DisplayName("테이블 조회 API 문서화")
  void getTable() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    Table table = new Table(
        tableId,
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        "{\"position\":{\"x\":10,\"y\":20}}");

    given(getTableUseCase.getTable(any(GetTableQuery.class)))
        .willReturn(Mono.just(table));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.id").isEqualTo(tableId)
        .jsonPath("$.result.extra").isEqualTo("{\"position\":{\"x\":10,\"y\":20}}")
        .consumeWith(document("table-get",
            TableApiSnippets.getTablePathParameters(),
            TableApiSnippets.getTableRequestHeaders(),
            TableApiSnippets.getTableResponseHeaders(),
            TableApiSnippets.getTableResponse()));
  }

  @Test
  @DisplayName("테이블 스냅샷 조회 API 문서화")
  void getTableSnapshot() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String constraintId = "06D6W4DAHD51T5NJPK29Q6BCRB";
    String constraintColumnId = "06D6W5EAHD51T5NJPK29Q6BCRC";
    String relationshipId = "06D6W6FAHD51T5NJPK29Q6BCRD";
    String relationshipColumnId = "06D6W7GAHD51T5NJPK29Q6BCRE";
    String pkTableId = "06D6W8HAHD51T5NJPK29Q6BCRF";
    String pkColumnId = "06D6W9IAHD51T5NJPK29Q6BCRG";
    String indexId = "06D6W9JAHD51T5NJPK29Q6BCRH";
    String indexColumnId = "06D6W9KAHD51T5NJPK29Q6BCRI";

    Table table = new Table(
        tableId,
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        "{\"position\":{\"x\":30,\"y\":40}}");
    Column column = new Column(columnId, tableId, "id", "BIGINT",
        new ColumnLengthScale(20, null, null), 1, true, null, null, "Primary key");
    Constraint constraint = new Constraint(constraintId, tableId, "pk_users",
        ConstraintKind.PRIMARY_KEY, null, null);
    ConstraintColumn constraintColumn = new ConstraintColumn(
        constraintColumnId, constraintId, columnId, 1);
    Relationship relationship = new Relationship(relationshipId, tableId, pkTableId,
        "fk_users_orders", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
    RelationshipColumn relationshipColumn = new RelationshipColumn(
        relationshipColumnId, relationshipId, pkColumnId, columnId, 1);
    Index index = new Index(indexId, tableId, "idx_users_id", IndexType.BTREE);
    IndexColumn indexColumn = new IndexColumn(indexColumnId, indexId, columnId, 0, SortDirection.ASC);

    given(getTableUseCase.getTable(any(GetTableQuery.class)))
        .willReturn(Mono.just(table));
    given(getColumnsByTableIdUseCase.getColumnsByTableId(any(GetColumnsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(column)));
    given(getConstraintsByTableIdUseCase.getConstraintsByTableId(
        any(GetConstraintsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(constraint)));
    given(getConstraintColumnsByConstraintIdUseCase.getConstraintColumnsByConstraintId(
        any(GetConstraintColumnsByConstraintIdQuery.class)))
        .willReturn(Mono.just(List.of(constraintColumn)));
    given(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
        any(GetRelationshipsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(relationship)));
    given(getRelationshipColumnsByRelationshipIdUseCase.getRelationshipColumnsByRelationshipId(
        any(GetRelationshipColumnsByRelationshipIdQuery.class)))
        .willReturn(Mono.just(List.of(relationshipColumn)));
    given(getIndexesByTableIdUseCase.getIndexesByTableId(any(GetIndexesByTableIdQuery.class)))
        .willReturn(Mono.just(List.of(index)));
    given(getIndexColumnsByIndexIdUseCase.getIndexColumnsByIndexId(
        any(GetIndexColumnsByIndexIdQuery.class)))
        .willReturn(Mono.just(List.of(indexColumn)));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/snapshot", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.table.id").isEqualTo(tableId)
        .jsonPath("$.result.table.extra").isEqualTo("{\"position\":{\"x\":30,\"y\":40}}")
        .jsonPath("$.result.columns[0].id").isEqualTo(columnId)
        .jsonPath("$.result.constraints[0].constraint.id").isEqualTo(constraintId)
        .jsonPath("$.result.relationships[0].relationship.id").isEqualTo(relationshipId)
        .jsonPath("$.result.indexes[0].index.id").isEqualTo(indexId)
        .jsonPath("$.result.indexes[0].columns[0].seqNo").isEqualTo(0)
        .consumeWith(document("table-snapshot",
            TableApiSnippets.getTableSnapshotPathParameters(),
            TableApiSnippets.getTableSnapshotRequestHeaders(),
            TableApiSnippets.getTableSnapshotResponseHeaders(),
            TableApiSnippets.getTableSnapshotResponse()));
  }

  @Test
  @DisplayName("배치 테이블 스냅샷 조회 API 문서화")
  void getTableSnapshots() {
    String tableId1 = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String tableId2 = "06D6W2CAHD51T5NJPK29Q6BCRX";
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";

    Table table1 = new Table(tableId1, schemaId, "users", "utf8mb4", "utf8mb4_general_ci");
    Table table2 = new Table(tableId2, schemaId, "orders", "utf8mb4", "utf8mb4_general_ci");

    given(getTableUseCase.getTable(any(GetTableQuery.class)))
        .willAnswer(invocation -> {
          GetTableQuery query = invocation.getArgument(0);
          if (query.tableId().equals(tableId1)) {
            return Mono.just(table1);
          } else {
            return Mono.just(table2);
          }
        });
    given(getColumnsByTableIdUseCase.getColumnsByTableId(any(GetColumnsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of()));
    given(getConstraintsByTableIdUseCase.getConstraintsByTableId(
        any(GetConstraintsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of()));
    given(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
        any(GetRelationshipsByTableIdQuery.class)))
        .willReturn(Mono.just(List.of()));
    given(getIndexesByTableIdUseCase.getIndexesByTableId(any(GetIndexesByTableIdQuery.class)))
        .willReturn(Mono.just(List.of()));

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(API_BASE_PATH + "/tables/snapshots")
            .queryParam("tableIds", tableId1, tableId2)
            .build())
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .consumeWith(document("table-snapshots-batch",
            TableApiSnippets.getTableSnapshotsQueryParameters(),
            TableApiSnippets.getTableSnapshotsRequestHeaders(),
            TableApiSnippets.getTableSnapshotsResponseHeaders(),
            TableApiSnippets.getTableSnapshotsResponse()));
  }

  @Test
  @DisplayName("스키마별 테이블 목록 조회 API 문서화")
  void getTablesBySchemaId() {
    String schemaId = "06D6W1GAHD51T5NJPK29Q6BCR8";
    String tableId1 = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String tableId2 = "06D6W2CAHD51T5NJPK29Q6BCRX";

    Table table1 = new Table(tableId1, schemaId, "users", "utf8mb4", "utf8mb4_general_ci");
    Table table2 = new Table(tableId2, schemaId, "orders", "utf8mb4", "utf8mb4_general_ci");

    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table1, table2));

    webTestClient.get()
        .uri(API_BASE_PATH + "/schemas/{schemaId}/tables", schemaId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result").isArray()
        .jsonPath("$.result[0].id").isEqualTo(tableId1)
        .consumeWith(document("table-list-by-schema",
            TableApiSnippets.getTablesBySchemaIdPathParameters(),
            TableApiSnippets.getTablesBySchemaIdRequestHeaders(),
            TableApiSnippets.getTablesBySchemaIdResponseHeaders(),
            TableApiSnippets.getTablesBySchemaIdResponse()));
  }

  @Test
  @DisplayName("테이블 이름 변경 API 문서화")
  void changeTableName() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    ChangeTableNameRequest request = new ChangeTableNameRequest("new_users");

    given(changeTableNameUseCase.changeTableName(any(ChangeTableNameCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/name", tableId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(objectMapper.writeValueAsString(request))
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("table-change-name",
            TableApiSnippets.changeTableNamePathParameters(),
            TableApiSnippets.changeTableNameRequestHeaders(),
            TableApiSnippets.changeTableNameRequest(),
            TableApiSnippets.changeTableNameResponseHeaders(),
            TableApiSnippets.changeTableNameResponse()));
  }

  @Test
  @DisplayName("테이블 메타 변경 API 문서화")
  void changeTableMeta() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeTableMetaUseCase.changeTableMeta(any(ChangeTableMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/meta", tableId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"charset":"utf8","collation":"utf8_unicode_ci"}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("table-change-meta",
            TableApiSnippets.changeTableMetaPathParameters(),
            TableApiSnippets.changeTableMetaRequestHeaders(),
            TableApiSnippets.changeTableMetaRequest(),
            TableApiSnippets.changeTableMetaResponseHeaders(),
            TableApiSnippets.changeTableMetaResponse()));
  }

  @Test
  @DisplayName("테이블 메타 변경 시 null을 전송하면 클리어된다")
  void changeTableMetaWithExplicitNull() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeTableMetaUseCase.changeTableMeta(any(ChangeTableMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/meta", tableId)
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
  @DisplayName("테이블 메타 변경 시 필드를 생략하면 기존 값이 유지된다")
  void changeTableMetaWithAbsentFields() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeTableMetaUseCase.changeTableMeta(any(ChangeTableMetaCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/meta", tableId)
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
  @DisplayName("테이블 프론트엔드 메타데이터 변경 API 문서화")
  void changeTableExtra() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeTableExtraUseCase.changeTableExtra(any(ChangeTableExtraCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/extra", tableId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"extra":{"position":{"x":120,"y":80},"color":"#0ea5e9","ui":{"collapsed":false}}}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("table-change-extra",
            TableApiSnippets.changeTableExtraPathParameters(),
            TableApiSnippets.changeTableExtraRequestHeaders(),
            TableApiSnippets.changeTableExtraRequest(),
            TableApiSnippets.changeTableExtraResponseHeaders(),
            TableApiSnippets.changeTableExtraResponse()));
  }

  @Test
  @DisplayName("테이블 추가정보 변경 API는 extra 객체를 허용한다")
  void changeTableExtraAcceptsObjectValue() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(changeTableExtraUseCase.changeTableExtra(any(ChangeTableExtraCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.patch()
        .uri(API_BASE_PATH + "/tables/{tableId}/extra", tableId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"extra":{"position":{"x":24,"y":48},"color":"#22c55e"}}
            """)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray();

    then(changeTableExtraUseCase).should()
        .changeTableExtra(new ChangeTableExtraCommand(tableId,
            "{\"position\":{\"x\":24,\"y\":48},\"color\":\"#22c55e\"}"));
  }

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  @DisplayName("테이블 삭제 API 문서화")
  void deleteTable() {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";

    given(deleteTableUseCase.deleteTable(any(DeleteTableCommand.class)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, tableId)));

    webTestClient.delete()
        .uri(API_BASE_PATH + "/tables/{tableId}", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.result.affectedTableIds").isArray()
        .consumeWith(document("table-delete",
            TableApiSnippets.deleteTablePathParameters(),
            TableApiSnippets.deleteTableRequestHeaders(),
            TableApiSnippets.deleteTableResponseHeaders(),
            TableApiSnippets.deleteTableResponse()));
  }

}
