package com.schemafy.core.erd.service;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationDatabaseIdRewriter 테스트")
class ValidationDatabaseIdRewriterTest {

    @Test
    @DisplayName("rewrite: 모든 id/참조 필드를 매핑으로 치환한다")
    void rewrite_replacesAllIdsAndReferences() {
        Validation.Database database = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-old")
                        .setName("schema")
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-old")
                                .setSchemaId("schema-old")
                                .setName("table")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("col-old")
                                        .setTableId("table-old")
                                        .setName("col")
                                        .setSeqNo(1)
                                        .setDataType("INT")
                                        .build())
                                .addIndexes(Validation.Index.newBuilder()
                                        .setId("idx-old")
                                        .setTableId("table-old")
                                        .setName("idx")
                                        .setType(Validation.IndexType.BTREE)
                                        .setComment("")
                                        .addColumns(Validation.IndexColumn
                                                .newBuilder()
                                                .setId("idxcol-old")
                                                .setIndexId("idx-old")
                                                .setColumnId("col-old")
                                                .setSeqNo(1)
                                                .setSortDir(
                                                        Validation.IndexSortDir.ASC)
                                                .build())
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId("constraint-old")
                                        .setTableId("table-old")
                                        .setName("pk")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .addColumns(
                                                Validation.ConstraintColumn
                                                        .newBuilder()
                                                        .setId("constraintcol-old")
                                                        .setConstraintId(
                                                                "constraint-old")
                                                        .setColumnId("col-old")
                                                        .setSeqNo(1)
                                                        .build())
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId("relationship-old")
                                        .setFkTableId("table-old")
                                        .setPkTableId("table-old")
                                        .setName("rel")
                                        .setKind(
                                                Validation.RelationshipKind.NON_IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId("relcol-old")
                                                        .setRelationshipId(
                                                                "relationship-old")
                                                        .setFkColumnId(
                                                                "col-old")
                                                        .setPkColumnId(
                                                                "col-old")
                                                        .setSeqNo(1)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        IdMappings idMappings = new IdMappings(
                Map.of("schema-old", "schema-new"),
                Map.of("table-old", "table-new"),
                Map.of("col-old", "col-new"),
                Map.of("idx-old", "idx-new"),
                Map.of("idxcol-old", "idxcol-new"),
                Map.of("constraint-old", "constraint-new"),
                Map.of("constraintcol-old", "constraintcol-new"),
                Map.of("relationship-old", "relationship-new"),
                Map.of("relcol-old", "relcol-new"));

        Validation.Database rewritten = ValidationDatabaseIdRewriter
                .rewrite(database, idMappings);

        Validation.Schema schema = rewritten.getSchemas(0);
        assertThat(schema.getId()).isEqualTo("schema-new");

        Validation.Table table = schema.getTables(0);
        assertThat(table.getId()).isEqualTo("table-new");
        assertThat(table.getSchemaId()).isEqualTo("schema-new");

        Validation.Column column = table.getColumns(0);
        assertThat(column.getId()).isEqualTo("col-new");
        assertThat(column.getTableId()).isEqualTo("table-new");

        Validation.Index index = table.getIndexes(0);
        assertThat(index.getId()).isEqualTo("idx-new");
        assertThat(index.getTableId()).isEqualTo("table-new");

        Validation.IndexColumn indexColumn = index.getColumns(0);
        assertThat(indexColumn.getId()).isEqualTo("idxcol-new");
        assertThat(indexColumn.getIndexId()).isEqualTo("idx-new");
        assertThat(indexColumn.getColumnId()).isEqualTo("col-new");

        Validation.Constraint constraint = table.getConstraints(0);
        assertThat(constraint.getId()).isEqualTo("constraint-new");
        assertThat(constraint.getTableId()).isEqualTo("table-new");

        Validation.ConstraintColumn constraintColumn = constraint.getColumns(0);
        assertThat(constraintColumn.getId()).isEqualTo("constraintcol-new");
        assertThat(constraintColumn.getConstraintId())
                .isEqualTo("constraint-new");
        assertThat(constraintColumn.getColumnId()).isEqualTo("col-new");

        Validation.Relationship relationship = table.getRelationships(0);
        assertThat(relationship.getId()).isEqualTo("relationship-new");
        assertThat(relationship.getFkTableId()).isEqualTo("table-new");
        assertThat(relationship.getPkTableId()).isEqualTo("table-new");

        Validation.RelationshipColumn relationshipColumn = relationship
                .getColumns(0);
        assertThat(relationshipColumn.getId()).isEqualTo("relcol-new");
        assertThat(relationshipColumn.getRelationshipId())
                .isEqualTo("relationship-new");
        assertThat(relationshipColumn.getFkColumnId()).isEqualTo("col-new");
        assertThat(relationshipColumn.getPkColumnId()).isEqualTo("col-new");
    }

}
