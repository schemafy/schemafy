package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;

import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Mono;

interface ErdProjectIdByAccessResourceRepository extends Repository<Project, String> {

  @Query("""
      SELECT p.id AS project_id
      FROM projects p
      WHERE p.id = :id
        AND p.deleted_at IS NULL
      """)
  Mono<String> findProjectIdByProjectId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_schemas s
      WHERE s.id = :id
      """)
  Mono<String> findProjectIdBySchemaId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_tables t
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE t.id = :id
      """)
  Mono<String> findProjectIdByTableId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_columns c
      JOIN db_tables t ON t.id = c.table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE c.id = :id
      """)
  Mono<String> findProjectIdByColumnId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_constraints c
      JOIN db_tables t ON t.id = c.table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE c.id = :id
      """)
  Mono<String> findProjectIdByConstraintId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_constraint_columns cc
      JOIN db_constraints c ON c.id = cc.constraint_id
      JOIN db_tables t ON t.id = c.table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE cc.id = :id
      """)
  Mono<String> findProjectIdByConstraintColumnId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_indexes i
      JOIN db_tables t ON t.id = i.table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE i.id = :id
      """)
  Mono<String> findProjectIdByIndexId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_index_columns ic
      JOIN db_indexes i ON i.id = ic.index_id
      JOIN db_tables t ON t.id = i.table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE ic.id = :id
      """)
  Mono<String> findProjectIdByIndexColumnId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_relationships r
      JOIN db_tables t ON t.id = r.fk_table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE r.id = :id
      """)
  Mono<String> findProjectIdByRelationshipId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM db_relationship_columns rc
      JOIN db_relationships r ON r.id = rc.relationship_id
      JOIN db_tables t ON t.id = r.fk_table_id
      JOIN db_schemas s ON s.id = t.schema_id
      WHERE rc.id = :id
      """)
  Mono<String> findProjectIdByRelationshipColumnId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM memos m
      JOIN db_schemas s ON s.id = m.schema_id
      WHERE m.id = :id
        AND m.deleted_at IS NULL
      """)
  Mono<String> findProjectIdByMemoId(String id);

  @Query("""
      SELECT s.project_id AS project_id
      FROM memo_comments mc
      JOIN memos m ON m.id = mc.memo_id
      JOIN db_schemas s ON s.id = m.schema_id
      WHERE mc.id = :id
        AND mc.deleted_at IS NULL
        AND m.deleted_at IS NULL
      """)
  Mono<String> findProjectIdByMemoCommentId(String id);

  @Query("""
      SELECT o.project_id AS project_id
      FROM erd_operation_log o
      WHERE o.op_id = :id
      """)
  Mono<String> findProjectIdByOperationId(String id);

}
