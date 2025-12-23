package com.schemafy.core.erd.service;

import java.util.Map;

import validation.Validation;

public final class ValidationDatabaseIdRewriter {

    private ValidationDatabaseIdRewriter() {}

    public static Validation.Database rewrite(
            Validation.Database database,
            IdMappings idMappings) {
        Validation.Database.Builder dbBuilder = database.toBuilder();
        dbBuilder.clearSchemas();

        for (Validation.Schema schema : database.getSchemasList()) {
            Validation.Schema.Builder schemaBuilder = schema.toBuilder();
            schemaBuilder.setId(remapId(schema.getId(), idMappings.schemas()));
            schemaBuilder.clearTables();

            for (Validation.Table table : schema.getTablesList()) {
                Validation.Table.Builder tableBuilder = table.toBuilder();
                tableBuilder
                        .setId(remapId(table.getId(), idMappings.tables()));
                tableBuilder.setSchemaId(
                        remapId(table.getSchemaId(), idMappings.schemas()));

                tableBuilder.clearColumns();
                for (Validation.Column column : table.getColumnsList()) {
                    tableBuilder.addColumns(rewriteColumn(column, idMappings));
                }

                tableBuilder.clearIndexes();
                for (Validation.Index index : table.getIndexesList()) {
                    tableBuilder.addIndexes(rewriteIndex(index, idMappings));
                }

                tableBuilder.clearConstraints();
                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    tableBuilder
                            .addConstraints(rewriteConstraint(constraint,
                                    idMappings));
                }

                tableBuilder.clearRelationships();
                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    tableBuilder.addRelationships(rewriteRelationship(
                            relationship,
                            idMappings));
                }

                schemaBuilder.addTables(tableBuilder.build());
            }

            dbBuilder.addSchemas(schemaBuilder.build());
        }

        return dbBuilder.build();
    }

    private static Validation.Column rewriteColumn(
            Validation.Column column,
            IdMappings idMappings) {
        return column.toBuilder()
                .setId(remapId(column.getId(), idMappings.columns()))
                .setTableId(remapId(column.getTableId(), idMappings.tables()))
                .build();
    }

    private static Validation.Index rewriteIndex(
            Validation.Index index,
            IdMappings idMappings) {
        Validation.Index.Builder builder = index.toBuilder()
                .setId(remapId(index.getId(), idMappings.indexes()))
                .setTableId(remapId(index.getTableId(), idMappings.tables()));

        builder.clearColumns();
        for (Validation.IndexColumn indexColumn : index.getColumnsList()) {
            builder.addColumns(rewriteIndexColumn(indexColumn, idMappings));
        }

        return builder.build();
    }

    private static Validation.IndexColumn rewriteIndexColumn(
            Validation.IndexColumn indexColumn,
            IdMappings idMappings) {
        return indexColumn.toBuilder()
                .setId(remapId(indexColumn.getId(), idMappings.indexColumns()))
                .setIndexId(
                        remapId(indexColumn.getIndexId(), idMappings.indexes()))
                .setColumnId(remapId(indexColumn.getColumnId(),
                        idMappings.columns()))
                .build();
    }

    private static Validation.Constraint rewriteConstraint(
            Validation.Constraint constraint,
            IdMappings idMappings) {
        Validation.Constraint.Builder builder = constraint.toBuilder()
                .setId(remapId(constraint.getId(), idMappings.constraints()))
                .setTableId(
                        remapId(constraint.getTableId(), idMappings.tables()));

        builder.clearColumns();
        for (Validation.ConstraintColumn constraintColumn : constraint
                .getColumnsList()) {
            builder.addColumns(
                    rewriteConstraintColumn(constraintColumn, idMappings));
        }

        return builder.build();
    }

    private static Validation.ConstraintColumn rewriteConstraintColumn(
            Validation.ConstraintColumn constraintColumn,
            IdMappings idMappings) {
        return constraintColumn.toBuilder()
                .setId(remapId(constraintColumn.getId(),
                        idMappings.constraintColumns()))
                .setConstraintId(remapId(constraintColumn.getConstraintId(),
                        idMappings.constraints()))
                .setColumnId(remapId(constraintColumn.getColumnId(),
                        idMappings.columns()))
                .build();
    }

    private static Validation.Relationship rewriteRelationship(
            Validation.Relationship relationship,
            IdMappings idMappings) {
        Validation.Relationship.Builder builder = relationship.toBuilder()
                .setId(remapId(relationship.getId(),
                        idMappings.relationships()))
                .setSrcTableId(remapId(relationship.getSrcTableId(),
                        idMappings.tables()))
                .setTgtTableId(remapId(relationship.getTgtTableId(),
                        idMappings.tables()));

        builder.clearColumns();
        for (Validation.RelationshipColumn relationshipColumn : relationship
                .getColumnsList()) {
            builder.addColumns(rewriteRelationshipColumn(relationshipColumn,
                    idMappings));
        }

        return builder.build();
    }

    private static Validation.RelationshipColumn rewriteRelationshipColumn(
            Validation.RelationshipColumn relationshipColumn,
            IdMappings idMappings) {
        return relationshipColumn.toBuilder()
                .setId(remapId(relationshipColumn.getId(),
                        idMappings.relationshipColumns()))
                .setRelationshipId(
                        remapId(relationshipColumn.getRelationshipId(),
                                idMappings.relationships()))
                .setFkColumnId(remapId(relationshipColumn.getFkColumnId(),
                        idMappings.columns()))
                .setRefColumnId(remapId(relationshipColumn.getRefColumnId(),
                        idMappings.columns()))
                .build();
    }

    private static String remapId(String originalId,
            Map<String, String> idMap) {
        String mappedId = idMap.get(originalId);
        return mappedId != null ? mappedId : originalId;
    }

}
