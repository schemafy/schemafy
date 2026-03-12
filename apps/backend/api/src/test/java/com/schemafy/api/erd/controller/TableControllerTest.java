package com.schemafy.api.erd.controller;

import java.util.Map;

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
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.WithMockCustomUser;
import com.schemafy.api.erd.controller.dto.request.ChangeTableNameRequest;
import com.schemafy.api.erd.controller.dto.request.CreateTableRequest;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.docs.TableApiSnippets;
import com.schemafy.api.erd.service.TableSnapshotOrchestrator;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
  private TableSnapshotOrchestrator tableSnapshotOrchestrator;

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
        .jsonPath("$.affectedTableIds").isArray()
        .jsonPath("$.data.id").isEqualTo(tableId)
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
        .jsonPath("$.id").isEqualTo(tableId)
        .jsonPath("$.extra").isEqualTo("{\"position\":{\"x\":10,\"y\":20}}")
        .consumeWith(document("table-get",
            TableApiSnippets.getTablePathParameters(),
            TableApiSnippets.getTableRequestHeaders(),
            TableApiSnippets.getTableResponseHeaders(),
            TableApiSnippets.getTableResponse()));
  }

  @Test
  @DisplayName("테이블 스냅샷 조회 API 문서화")
  void getTableSnapshot() throws Exception {
    String tableId = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String columnId = "06D6W3CAHD51T5NJPK29Q6BCRA";
    String constraintId = "06D6W4DAHD51T5NJPK29Q6BCRB";
    String relationshipId = "06D6W6FAHD51T5NJPK29Q6BCRD";
    String indexId = "06D6W9JAHD51T5NJPK29Q6BCRH";

    TableSnapshotResponse response = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
              "table": {
                "id": "06D6W2BAHD51T5NJPK29Q6BCR9",
                "schemaId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "name": "users",
                "charset": "utf8mb4",
                "collation": "utf8mb4_general_ci",
                "extra": "{\\"position\\":{\\"x\\":30,\\"y\\":40}}"
              },
              "columns": [
                {
                  "id": "06D6W3CAHD51T5NJPK29Q6BCRA",
                  "tableId": "06D6W2BAHD51T5NJPK29Q6BCR9",
                  "name": "id",
                  "dataType": "BIGINT",
                  "typeArguments": {"length": 20, "precision": null, "scale": null, "values": null},
                  "seqNo": 1,
                  "autoIncrement": true,
                  "charset": null,
                  "collation": null,
                  "comment": "Primary key"
                }
              ],
              "constraints": [
                {
                  "constraint": {
                    "id": "06D6W4DAHD51T5NJPK29Q6BCRB",
                    "tableId": "06D6W2BAHD51T5NJPK29Q6BCR9",
                    "name": "pk_users",
                    "kind": "PRIMARY_KEY",
                    "checkExpr": null,
                    "defaultExpr": null
                  },
                  "columns": [
                    {
                      "id": "06D6W5EAHD51T5NJPK29Q6BCRC",
                      "constraintId": "06D6W4DAHD51T5NJPK29Q6BCRB",
                      "columnId": "06D6W3CAHD51T5NJPK29Q6BCRA",
                      "seqNo": 1
                    }
                  ]
                }
              ],
              "relationships": [
                {
                  "relationship": {
                    "id": "06D6W6FAHD51T5NJPK29Q6BCRD",
                    "fkTableId": "06D6W2BAHD51T5NJPK29Q6BCR9",
                    "pkTableId": "06D6W8HAHD51T5NJPK29Q6BCRF",
                    "name": "fk_users_orders",
                    "kind": "NON_IDENTIFYING",
                    "cardinality": "ONE_TO_MANY",
                    "extra": null
                  },
                  "columns": [
                    {
                      "id": "06D6W7GAHD51T5NJPK29Q6BCRE",
                      "relationshipId": "06D6W6FAHD51T5NJPK29Q6BCRD",
                      "pkColumnId": "06D6W9IAHD51T5NJPK29Q6BCRG",
                      "fkColumnId": "06D6W3CAHD51T5NJPK29Q6BCRA",
                      "seqNo": 1
                    }
                  ]
                }
              ],
              "indexes": [
                {
                  "index": {
                    "id": "06D6W9JAHD51T5NJPK29Q6BCRH",
                    "tableId": "06D6W2BAHD51T5NJPK29Q6BCR9",
                    "name": "idx_users_id",
                    "type": "BTREE"
                  },
                  "columns": [
                    {
                      "id": "06D6W9KAHD51T5NJPK29Q6BCRI",
                      "indexId": "06D6W9JAHD51T5NJPK29Q6BCRH",
                      "columnId": "06D6W3CAHD51T5NJPK29Q6BCRA",
                      "seqNo": 0,
                      "sortDirection": "ASC"
                    }
                  ]
                }
              ]
            }
            """), TableSnapshotResponse.class);
    given(tableSnapshotOrchestrator.getTableSnapshot(tableId))
        .willReturn(Mono.just(response));

    webTestClient.get()
        .uri(API_BASE_PATH + "/tables/{tableId}/snapshot", tableId)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.table.id").isEqualTo(tableId)
        .jsonPath("$.table.extra").isEqualTo("{\"position\":{\"x\":30,\"y\":40}}")
        .jsonPath("$.columns[0].id").isEqualTo(columnId)
        .jsonPath("$.constraints[0].constraint.id").isEqualTo(constraintId)
        .jsonPath("$.relationships[0].relationship.id").isEqualTo(relationshipId)
        .jsonPath("$.indexes[0].index.id").isEqualTo(indexId)
        .jsonPath("$.indexes[0].columns[0].seqNo").isEqualTo(0)
        .consumeWith(document("table-snapshot",
            TableApiSnippets.getTableSnapshotPathParameters(),
            TableApiSnippets.getTableSnapshotRequestHeaders(),
            TableApiSnippets.getTableSnapshotResponseHeaders(),
            TableApiSnippets.getTableSnapshotResponse()));
  }

  @Test
  @DisplayName("배치 테이블 스냅샷 조회 API 문서화")
  void getTableSnapshots() throws Exception {
    String tableId1 = "06D6W2BAHD51T5NJPK29Q6BCR9";
    String tableId2 = "06D6W2CAHD51T5NJPK29Q6BCRX";
    TableSnapshotResponse snapshot1 = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
              "table": {
                "id": "06D6W2BAHD51T5NJPK29Q6BCR9",
                "schemaId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "name": "users",
                "charset": "utf8mb4",
                "collation": "utf8mb4_general_ci",
                "extra": null
              },
              "columns": [],
              "constraints": [],
              "relationships": [],
              "indexes": []
            }
            """), TableSnapshotResponse.class);
    TableSnapshotResponse snapshot2 = objectMapper.treeToValue(
        objectMapper.readTree("""
            {
              "table": {
                "id": "06D6W2CAHD51T5NJPK29Q6BCRX",
                "schemaId": "06D6W1GAHD51T5NJPK29Q6BCR8",
                "name": "orders",
                "charset": "utf8mb4",
                "collation": "utf8mb4_general_ci",
                "extra": null
              },
              "columns": [],
              "constraints": [],
              "relationships": [],
              "indexes": []
            }
            """), TableSnapshotResponse.class);
    given(tableSnapshotOrchestrator.getTableSnapshots(anyList()))
        .willReturn(Mono.just(Map.of(tableId1, snapshot1, tableId2, snapshot2)));

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(API_BASE_PATH + "/tables/snapshots")
            .queryParam("tableIds", tableId1, tableId2)
            .build())
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
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
        .jsonPath("$").isArray()
        .jsonPath("$[0].id").isEqualTo(tableId1)
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
        .jsonPath("$.affectedTableIds").isArray()
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
        .jsonPath("$.affectedTableIds").isArray()
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
        .jsonPath("$.affectedTableIds").isArray();
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
        .jsonPath("$.affectedTableIds").isArray();
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
        .jsonPath("$.affectedTableIds").isArray()
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
        .jsonPath("$.affectedTableIds").isArray();

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
        .jsonPath("$.affectedTableIds").isArray()
        .consumeWith(document("table-delete",
            TableApiSnippets.deleteTablePathParameters(),
            TableApiSnippets.deleteTableRequestHeaders(),
            TableApiSnippets.deleteTableResponseHeaders(),
            TableApiSnippets.deleteTableResponse()));
  }

}
