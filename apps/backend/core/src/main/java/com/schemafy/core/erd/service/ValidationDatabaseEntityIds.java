package com.schemafy.core.erd.service;

import java.util.HashSet;
import java.util.Set;

import validation.Validation;

public record ValidationDatabaseEntityIds(
        Set<String> schemas,
        Set<String> tables,
        Set<String> columns,
        Set<String> indexes,
        Set<String> indexColumns,
        Set<String> constraints,
        Set<String> constraintColumns,
        Set<String> relationships,
        Set<String> relationshipColumns) {

    public static ValidationDatabaseEntityIds from(
            Validation.Database database) {
        Set<String> schemas = new HashSet<>();
        Set<String> tables = new HashSet<>();
        Set<String> columns = new HashSet<>();
        Set<String> indexes = new HashSet<>();
        Set<String> indexColumns = new HashSet<>();
        Set<String> constraints = new HashSet<>();
        Set<String> constraintColumns = new HashSet<>();
        Set<String> relationships = new HashSet<>();
        Set<String> relationshipColumns = new HashSet<>();

        for (Validation.Schema schema : database.getSchemasList()) {
            schemas.add(schema.getId());

            for (Validation.Table table : schema.getTablesList()) {
                tables.add(table.getId());

                for (Validation.Column column : table.getColumnsList()) {
                    columns.add(column.getId());
                }

                for (Validation.Index index : table.getIndexesList()) {
                    indexes.add(index.getId());
                    for (Validation.IndexColumn indexColumn : index
                            .getColumnsList()) {
                        indexColumns.add(indexColumn.getId());
                    }
                }

                for (Validation.Constraint constraint : table
                        .getConstraintsList()) {
                    constraints.add(constraint.getId());
                    for (Validation.ConstraintColumn constraintColumn : constraint
                            .getColumnsList()) {
                        constraintColumns.add(constraintColumn.getId());
                    }
                }

                for (Validation.Relationship relationship : table
                        .getRelationshipsList()) {
                    relationships.add(relationship.getId());
                    for (Validation.RelationshipColumn relationshipColumn : relationship
                            .getColumnsList()) {
                        relationshipColumns.add(relationshipColumn.getId());
                    }
                }
            }
        }

        return new ValidationDatabaseEntityIds(
                schemas,
                tables,
                columns,
                indexes,
                indexColumns,
                constraints,
                constraintColumns,
                relationships,
                relationshipColumns);
    }

}
