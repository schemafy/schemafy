package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.project.application.access.GetProjectIdByAccessResourcePort;
import com.schemafy.core.project.application.access.ProjectAccessResourceType;

import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({
  ErdProjectIdByAccessResourceAdapter.class,
  R2dbcTestConfiguration.class
})
@DisplayName("ErdProjectIdByAccessResourceAdapter")
class ErdProjectIdByAccessResourceAdapterTest {

  private static final String WORKSPACE_ID = "01ARZ3NDEKTSV4RRFFQ69G5WSP";
  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5PRJ";
  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String PK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TPK";
  private static final String FK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TFK";
  private static final String PK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5CPK";
  private static final String FK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5CFK";
  private static final String CONSTRAINT_ID = "01ARZ3NDEKTSV4RRFFQ69G5CST";
  private static final String CONSTRAINT_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5CSC";
  private static final String INDEX_ID = "01ARZ3NDEKTSV4RRFFQ69G5IDX";
  private static final String INDEX_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5IDC";
  private static final String RELATIONSHIP_ID = "01ARZ3NDEKTSV4RRFFQ69G5REL";
  private static final String RELATIONSHIP_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5RLC";
  private static final String MEMO_ID = "01ARZ3NDEKTSV4RRFFQ69G5MEM";
  private static final String MEMO_COMMENT_ID = "01ARZ3NDEKTSV4RRFFQ69G5MCM";
  private static final String OPERATION_ID = "01ARZ3NDEKTSV4RRFFQ69G5OPR";
  private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5USR";

  @Autowired
  GetProjectIdByAccessResourcePort sut;

  @Autowired
  DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    deleteAll();
    insertWorkspaceAndProject();
    insertErdResources();
  }

  @Test
  @DisplayName("리소스 pk로 projectId를 직접 조회한다")
  void findProjectId_returnsProjectIdByResourcePk() {
    assertProjectId(ProjectAccessResourceType.PROJECT, PROJECT_ID);
    assertProjectId(ProjectAccessResourceType.SCHEMA, SCHEMA_ID);
    assertProjectId(ProjectAccessResourceType.TABLE, FK_TABLE_ID);
    assertProjectId(ProjectAccessResourceType.COLUMN, FK_COLUMN_ID);
    assertProjectId(ProjectAccessResourceType.CONSTRAINT, CONSTRAINT_ID);
    assertProjectId(ProjectAccessResourceType.CONSTRAINT_COLUMN, CONSTRAINT_COLUMN_ID);
    assertProjectId(ProjectAccessResourceType.INDEX, INDEX_ID);
    assertProjectId(ProjectAccessResourceType.INDEX_COLUMN, INDEX_COLUMN_ID);
    assertProjectId(ProjectAccessResourceType.RELATIONSHIP, RELATIONSHIP_ID);
    assertProjectId(ProjectAccessResourceType.RELATIONSHIP_COLUMN, RELATIONSHIP_COLUMN_ID);
    assertProjectId(ProjectAccessResourceType.MEMO, MEMO_ID);
    assertProjectId(ProjectAccessResourceType.MEMO_COMMENT, MEMO_COMMENT_ID);
    assertProjectId(ProjectAccessResourceType.OPERATION, OPERATION_ID);
  }

  @Test
  @DisplayName("삭제된 메모와 댓글은 projectId를 반환하지 않는다")
  void findProjectId_returnsEmptyForDeletedMemoResources() {
    update("UPDATE memo_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", MEMO_COMMENT_ID);

    StepVerifier.create(sut.findProjectId(ProjectAccessResourceType.MEMO_COMMENT, MEMO_COMMENT_ID))
        .verifyComplete();

    update("UPDATE memos SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id", MEMO_ID);

    StepVerifier.create(sut.findProjectId(ProjectAccessResourceType.MEMO, MEMO_ID))
        .verifyComplete();
  }

  private void assertProjectId(ProjectAccessResourceType type, String id) {
    StepVerifier.create(sut.findProjectId(type, id))
        .expectNext(PROJECT_ID)
        .verifyComplete();
  }

  private void deleteAll() {
    deleteFrom("memo_comments");
    deleteFrom("memos");
    deleteFrom("erd_operation_log");
    deleteFrom("db_relationship_columns");
    deleteFrom("db_relationships");
    deleteFrom("db_index_columns");
    deleteFrom("db_indexes");
    deleteFrom("db_constraint_columns");
    deleteFrom("db_constraints");
    deleteFrom("db_columns");
    deleteFrom("db_tables");
    deleteFrom("db_schemas");
    deleteFrom("project_members");
    deleteFrom("projects");
    deleteFrom("workspace_members");
    deleteFrom("workspaces");
  }

  private void deleteFrom(String tableName) {
    databaseClient.sql("DELETE FROM " + tableName)
        .then()
        .block();
  }

  private void insertWorkspaceAndProject() {
    databaseClient.sql("""
        INSERT INTO workspaces (id, name, description)
        VALUES (:id, 'workspace', 'description')
        """)
        .bind("id", WORKSPACE_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO projects (id, workspace_id, name, description)
        VALUES (:id, :workspaceId, 'project', 'description')
        """)
        .bind("id", PROJECT_ID)
        .bind("workspaceId", WORKSPACE_ID)
        .then()
        .block();
  }

  private void insertErdResources() {
    databaseClient.sql("""
        INSERT INTO db_schemas (id, project_id, db_vendor_name, name, charset, collation)
        VALUES (:id, :projectId, 'MySQL', 'schema', 'utf8mb4', 'utf8mb4_general_ci')
        """)
        .bind("id", SCHEMA_ID)
        .bind("projectId", PROJECT_ID)
        .then()
        .block();
    insertTable(PK_TABLE_ID, "pk_table");
    insertTable(FK_TABLE_ID, "fk_table");
    insertColumn(PK_COLUMN_ID, PK_TABLE_ID, "pk_column", 0);
    insertColumn(FK_COLUMN_ID, FK_TABLE_ID, "fk_column", 0);
    databaseClient.sql("""
        INSERT INTO db_constraints (id, table_id, name, kind)
        VALUES (:id, :tableId, 'constraint_1', 'PRIMARY_KEY')
        """)
        .bind("id", CONSTRAINT_ID)
        .bind("tableId", PK_TABLE_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO db_constraint_columns (id, constraint_id, column_id, seq_no)
        VALUES (:id, :constraintId, :columnId, 0)
        """)
        .bind("id", CONSTRAINT_COLUMN_ID)
        .bind("constraintId", CONSTRAINT_ID)
        .bind("columnId", PK_COLUMN_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO db_indexes (id, table_id, name, type)
        VALUES (:id, :tableId, 'index_1', 'BTREE')
        """)
        .bind("id", INDEX_ID)
        .bind("tableId", FK_TABLE_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO db_index_columns (id, index_id, column_id, seq_no, sort_dir)
        VALUES (:id, :indexId, :columnId, 0, 'ASC')
        """)
        .bind("id", INDEX_COLUMN_ID)
        .bind("indexId", INDEX_ID)
        .bind("columnId", FK_COLUMN_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO db_relationships (id, pk_table_id, fk_table_id, name, kind, cardinality)
        VALUES (:id, :pkTableId, :fkTableId, 'relationship_1', 'NON_IDENTIFYING', 'ONE_TO_MANY')
        """)
        .bind("id", RELATIONSHIP_ID)
        .bind("pkTableId", PK_TABLE_ID)
        .bind("fkTableId", FK_TABLE_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO db_relationship_columns (id, relationship_id, fk_column_id, pk_column_id, seq_no)
        VALUES (:id, :relationshipId, :fkColumnId, :pkColumnId, 0)
        """)
        .bind("id", RELATIONSHIP_COLUMN_ID)
        .bind("relationshipId", RELATIONSHIP_ID)
        .bind("fkColumnId", FK_COLUMN_ID)
        .bind("pkColumnId", PK_COLUMN_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO memos (id, schema_id, author_id, positions)
        VALUES (:id, :schemaId, :authorId, '{}')
        """)
        .bind("id", MEMO_ID)
        .bind("schemaId", SCHEMA_ID)
        .bind("authorId", USER_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO memo_comments (id, memo_id, author_id, body)
        VALUES (:id, :memoId, :authorId, 'comment')
        """)
        .bind("id", MEMO_COMMENT_ID)
        .bind("memoId", MEMO_ID)
        .bind("authorId", USER_ID)
        .then()
        .block();
    databaseClient.sql("""
        INSERT INTO erd_operation_log (
            op_id,
            project_id,
            schema_id,
            op_type,
            committed_revision,
            actor_user_id,
            derivation_kind,
            lifecycle_state,
            payload_json,
            touched_entities_json,
            affected_table_ids_json
        ) VALUES (
            :opId,
            :projectId,
            :schemaId,
            'CHANGE_TABLE_NAME',
            1,
            :actorUserId,
            'ORIGINAL',
            'COMMITTED',
            '{}',
            '[]',
            '[]'
        )
        """)
        .bind("opId", OPERATION_ID)
        .bind("projectId", PROJECT_ID)
        .bind("schemaId", SCHEMA_ID)
        .bind("actorUserId", USER_ID)
        .then()
        .block();
  }

  private void insertTable(String tableId, String name) {
    databaseClient.sql("""
        INSERT INTO db_tables (id, schema_id, name, charset, collation)
        VALUES (:id, :schemaId, :name, 'utf8mb4', 'utf8mb4_general_ci')
        """)
        .bind("id", tableId)
        .bind("schemaId", SCHEMA_ID)
        .bind("name", name)
        .then()
        .block();
  }

  private void insertColumn(String columnId, String tableId, String name, int seqNo) {
    databaseClient.sql("""
        INSERT INTO db_columns (id, table_id, name, data_type, seq_no)
        VALUES (:id, :tableId, :name, 'BIGINT', :seqNo)
        """)
        .bind("id", columnId)
        .bind("tableId", tableId)
        .bind("name", name)
        .bind("seqNo", seqNo)
        .then()
        .block();
  }

  private void update(String sql, String id) {
    databaseClient.sql(sql)
        .bind("id", id)
        .then()
        .block();
  }

}
