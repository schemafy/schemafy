package com.schemafy.core.erd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.request.CreateTableRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.Table;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("TableService 테스트")
class TableServiceTest {

    @Autowired
    TableService tableService;

    @Autowired
    TableRepository tableRepository;

    @Autowired
    ColumnRepository columnRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    @MockitoBean
    ValidationClient validationClient;

    @BeforeEach
    void setUp() {
        tableRepository.deleteAll().block();
        relationshipRepository.deleteAll().block();
        columnRepository.deleteAll().block();

        given(validationClient
                .changeTableName(any(Validation.ChangeTableNameRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
        given(validationClient
                .deleteTable(any(Validation.DeleteTableRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("createTable: 테이블 생성 시 매핑 정보가 올바르게 반환된다")
    void createTable_mappingResponse_success() {
        Validation.CreateTableRequest request = Validation.CreateTableRequest
                .newBuilder()
                .setTable(Validation.Table.newBuilder()
                        .setId("fe-table-id")
                        .setSchemaId("schema-1")
                        .setName("test-table")
                        .setComment("테스트 테이블")
                        .setTableOptions("ENGINE=InnoDB")
                        .build())
                .setDatabase(Validation.Database.newBuilder()
                        .setId("proj-1")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("schema-1")
                                .setName("test-schema")
                                .build())
                        .build())
                .build();

        CreateTableRequestWithExtra requestWithExtra = new CreateTableRequestWithExtra(
                request, "extra-data");

        // ValidationClient 모킹
        Validation.Database mockResponse = Validation.Database
                .newBuilder()
                .setId("proj-1")
                .setIsAffected(true)
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("test-schema")
                        .setIsAffected(true)
                        .addTables(Validation.Table.newBuilder()
                                .setId("be-table-id")
                                .setSchemaId("schema-1")
                                .setName("test-table")
                                .setComment("테스트 테이블")
                                .setTableOptions("ENGINE=InnoDB")
                                .setIsAffected(true)
                                .build())
                        .build())
                .build();

        given(validationClient
                .createTable(any(Validation.CreateTableRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when
        Mono<AffectedMappingResponse> result = tableService
                .createTable(requestWithExtra);

        StepVerifier.create(result)
                .assertNext(response -> {
                    // 테이블 매핑 정보 확인 (FE-ID → BE-ID 매핑)
                    assertThat(response.tables()).hasSize(1);
                    assertThat(response.tables()).containsEntry("fe-table-id",
                            "be-table-id");
                    // 다른 매핑들은 비어있어야 함 (테이블만 생성했으므로)
                    assertThat(response.schemas()).isEmpty();
                    assertThat(response.columns()).isEmpty();
                    assertThat(response.indexes()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraints()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationships()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTable: 저장된 테이블을 조회한다")
    void getTable_success() {
        Table saved = tableRepository.save(
                Table.builder()
                        .schemaId("schema-1")
                        .name("test-table")
                        .comment("테스트 테이블")
                        .tableOptions("ENGINE=InnoDB")
                        .extra("extra-data")
                        .build())
                .block();

        Mono<TableDetailResponse> result = tableService.getTable(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("test-table");
                    assertThat(found.getExtra()).isEqualTo("extra-data");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTablesBySchemaId: 스키마 기준으로 테이블 목록을 조회한다")
    void getTablesBySchemaId_success() {
        Table t1 = Table.builder().schemaId("schema-A").name("table-a")
                .comment("테스트 테이블").tableOptions("ENGINE=InnoDB")
                .extra("extra-data").build();
        Table t2 = Table.builder().schemaId("schema-A").name("table-b")
                .comment("테스트 테이블").tableOptions("ENGINE=InnoDB")
                .extra("extra-data").build();
        Table tOther = Table.builder().schemaId("schema-B").name("table-c")
                .comment("테스트 테이블").tableOptions("ENGINE=InnoDB")
                .extra("extra-data").build();
        tableRepository.save(t1).then(tableRepository.save(t2))
                .then(tableRepository.save(tOther)).block();

        StepVerifier
                .create(tableService.getTablesBySchemaId("schema-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(
                            list.stream().map(TableResponse::getName).toList())
                            .containsExactlyInAnyOrder("table-a", "table-b");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateTableName: 존재하면 이름을 변경한다")
    void updateTableName_success() {
        Table saved = tableRepository.save(
                Table.builder()
                        .schemaId("schema-1")
                        .name("old-name")
                        .comment("테스트 테이블")
                        .tableOptions("ENGINE=InnoDB")
                        .extra("extra-data")
                        .build())
                .block();

        StepVerifier
                .create(tableService
                        .updateTableName(
                                Validation.ChangeTableNameRequest.newBuilder()
                                        .setTableId(saved.getId())
                                        .setNewName("new-name")
                                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new-name");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(tableRepository.findById(saved.getId()))
                .assertNext(
                        found -> assertThat(found.getName())
                                .isEqualTo("new-name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateTableName: 존재하지 않으면 에러를 반환한다")
    void updateTableName_notFound() {
        StepVerifier
                .create(tableService
                        .updateTableName(
                                Validation.ChangeTableNameRequest.newBuilder()
                                        .setTableId("non-existent")
                                        .setSchemaId("schema-1")
                                        .setNewName("new-name")
                                        .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_TABLE_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("deleteTable: 소프트 삭제가 수행된다")
    void deleteTable_softDelete() {
        String childTableId = "child-table";

        Table saved = tableRepository.save(
                Table.builder()
                        .schemaId("schema-1")
                        .name("to-delete")
                        .comment("테스트 테이블")
                        .tableOptions("ENGINE=InnoDB")
                        .extra("extra-data")
                        .build())
                .block();

        Column tableOwnedColumn = columnRepository.save(
                Column.builder()
                        .tableId(saved.getId())
                        .name("owned_col")
                        .ordinalPosition(1)
                        .dataType("INT")
                        .build())
                .block();

        Column fkColumnToDelete = columnRepository.save(
                Column.builder()
                        .tableId(childTableId)
                        .name("parent_id")
                        .ordinalPosition(1)
                        .dataType("INT")
                        .build())
                .block();

        Relationship relationship = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId(childTableId)
                        .tgtTableId(saved.getId())
                        .name("fk_child_parent")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
                        .onDelete("NO_ACTION")
                        .onUpdate("NO_ACTION")
                        .extra("")
                        .build())
                .block();

        Validation.Database beforeDatabase = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("schema")
                        .addTables(Validation.Table.newBuilder()
                                .setId(saved.getId())
                                .setSchemaId("schema-1")
                                .setName("to-delete")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(tableOwnedColumn.getId())
                                        .setTableId(saved.getId())
                                        .setName("owned_col")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setSchemaId("schema-1")
                                .setName("child")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(fkColumnToDelete.getId())
                                        .setTableId(childTableId)
                                        .setName("parent_id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .addRelationships(
                                        Validation.Relationship.newBuilder()
                                                .setId(relationship.getId())
                                                .setSrcTableId(childTableId)
                                                .setTgtTableId(saved.getId())
                                                .setName("fk_child_parent")
                                                .setKind(
                                                        Validation.RelationshipKind.NON_IDENTIFYING)
                                                .setCardinality(
                                                        Validation.RelationshipCardinality.ONE_TO_MANY)
                                                .addColumns(
                                                        Validation.RelationshipColumn
                                                                .newBuilder()
                                                                .setId(
                                                                        "relcol-1")
                                                                .setRelationshipId(
                                                                        relationship
                                                                                .getId())
                                                                .setFkColumnId(
                                                                        fkColumnToDelete
                                                                                .getId())
                                                                .setRefColumnId(
                                                                        tableOwnedColumn
                                                                                .getId())
                                                                .setSeqNo(1)
                                                                .build())
                                                .build())
                                .build())
                        .build())
                .build();

        Validation.Database afterDatabase = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("schema")
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setSchemaId("schema-1")
                                .setName("child")
                                .build())
                        .build())
                .build();

        given(validationClient
                .deleteTable(any(Validation.DeleteTableRequest.class)))
                .willReturn(Mono.just(afterDatabase));

        StepVerifier
                .create(tableService
                        .deleteTable(Validation.DeleteTableRequest.newBuilder()
                                .setTableId(saved.getId())
                                .setSchemaId("schema-1")
                                .setDatabase(beforeDatabase)
                                .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(tableRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();

        StepVerifier.create(
                relationshipRepository.findById(relationship.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();

        StepVerifier.create(
                columnRepository.findById(fkColumnToDelete.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();

        StepVerifier.create(
                columnRepository.findById(tableOwnedColumn.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isFalse())
                .verifyComplete();
    }

}
