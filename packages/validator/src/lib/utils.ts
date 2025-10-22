import {
    schemaHandlers,
    type SchemaHandlers,
    tableHandlers,
    type TableHandlers,
    columnHandlers,
    type ColumnHandlers,
    indexHandlers,
    type IndexHandlers,
    constraintHandlers,
    type ConstraintHandlers,
    relationshipHandlers,
    type RelationshipHandlers,
} from "./handlers";
import { type Database, DATABASE } from "./types";
import {
    SchemaNameInvalidError,
    SchemaNameNotUniqueError,
    TableNameNotInvalidError,
    TableNameNotUniqueError,
    ColumnNameInvalidError,
    ColumnNameNotUniqueError,
    ColumnDataTypeRequiredError,
    ColumnNameIsReservedKeywordError,
    MultipleAutoIncrementColumnsError,
    ConstraintNameNotUniqueError,
    ConstraintColumnNotExistError,
    DuplicateKeyDefinitionError,
    IndexNameNotUniqueError,
    IndexTypeInvalidError,
    IndexColumnNotExistError,
    DuplicateIndexDefinitionError,
    RelationshipNameNotUniqueError,
    RelationshipEmptyError,
    RelationshipTargetTableNotExistError,
    RelationshipCyclicReferenceError,
    ERDValidationError,
} from "./errors";

interface ERDValidator
    extends SchemaHandlers,
        TableHandlers,
        ColumnHandlers,
        IndexHandlers,
        ConstraintHandlers,
        RelationshipHandlers {
    validate: (database: Database) => void;
}

export const ERD_VALIDATOR: ERDValidator = {
    validate: (database: Database) => {
        const parseResult = DATABASE.safeParse(database);
        if (!parseResult.success) {
            const zodError = parseResult.error.issues[0];
            throw new ERDValidationError(
                "SCHEMA_VALIDATION_ERROR" as any,
                `Schema validation failed: ${zodError.message} at ${zodError.path.join(".")}`,
            );
        }

        const schemaNames = new Set<string>();
        for (const schema of database.schemas) {
            if (schema.name.length < 3 || schema.name.length > 20) {
                throw new SchemaNameInvalidError(schema.name);
            }

            if (schemaNames.has(schema.name)) {
                const existingSchemaId =
                    database.schemas.find((s) => s.name === schema.name && s.id !== schema.id)?.id || "";
                throw new SchemaNameNotUniqueError(schema.name, existingSchemaId);
            }
            schemaNames.add(schema.name);

            const tableNames = new Set<string>();
            for (const table of schema.tables) {
                if (table.name.length < 3 || table.name.length > 20) {
                    throw new TableNameNotInvalidError(table.name);
                }

                if (tableNames.has(table.name)) {
                    throw new TableNameNotUniqueError(table.name, schema.id);
                }
                tableNames.add(table.name);

                const columnNames = new Set<string>();
                let autoIncrementCount = 0;

                for (const column of table.columns) {
                    if (column.name.length < 3 || column.name.length > 40) {
                        throw new ColumnNameInvalidError(column.name);
                    }

                    if (columnNames.has(column.name)) {
                        throw new ColumnNameNotUniqueError(column.name, table.id);
                    }
                    columnNames.add(column.name);

                    if (!column.dataType || column.dataType.trim() === "") {
                        throw new ColumnDataTypeRequiredError(column.name, table.id);
                    }

                    if (column.isAutoIncrement) {
                        autoIncrementCount++;
                        if (autoIncrementCount > 1) {
                            throw new MultipleAutoIncrementColumnsError(table.id);
                        }
                    }

                    const reservedKeywords = [
                        "SELECT",
                        "INSERT",
                        "UPDATE",
                        "DELETE",
                        "FROM",
                        "WHERE",
                        "JOIN",
                        "ORDER",
                        "GROUP",
                        "HAVING",
                    ];
                    if (reservedKeywords.includes(column.name.toUpperCase())) {
                        throw new ColumnNameIsReservedKeywordError(column.name, schema.dbVendorId);
                    }
                }

                const constraintNames = new Set<string>();
                const constraintDefinitions = new Set<string>();

                for (const constraint of table.constraints) {
                    const fullConstraintName = `${schema.name}.${constraint.name}`;
                    if (constraintNames.has(fullConstraintName)) {
                        throw new ConstraintNameNotUniqueError(constraint.name, schema.id);
                    }
                    constraintNames.add(fullConstraintName);

                    for (const constraintColumn of constraint.columns) {
                        const columnExists = table.columns.some((col) => col.id === constraintColumn.columnId);
                        if (!columnExists) {
                            throw new ConstraintColumnNotExistError(constraintColumn.columnId, constraint.name);
                        }
                    }

                    const constraintDef = JSON.stringify({
                        kind: constraint.kind,
                        checkExpr: constraint.checkExpr,
                        defaultExpr: constraint.defaultExpr,
                        columnIds: constraint.columns.map((cc) => cc.columnId).sort(),
                    });

                    if (constraintDefinitions.has(constraintDef)) {
                        const existingConstraint = table.constraints.find((c) => {
                            const existingDef = JSON.stringify({
                                kind: c.kind,
                                checkExpr: c.checkExpr,
                                defaultExpr: c.defaultExpr,
                                columnIds: c.columns.map((cc) => cc.columnId).sort(),
                            });
                            return existingDef === constraintDef && c.id !== constraint.id;
                        });
                        throw new DuplicateKeyDefinitionError(constraint.name, existingConstraint?.name || "unknown");
                    }
                    constraintDefinitions.add(constraintDef);
                }

                const indexNames = new Set<string>();
                const indexDefinitions = new Set<string>();

                for (const index of table.indexes) {
                    if (indexNames.has(index.name)) {
                        throw new IndexNameNotUniqueError(index.name, table.id);
                    }
                    indexNames.add(index.name);

                    const validIndexTypes = ["BTREE", "HASH", "FULLTEXT", "SPATIAL", "OTHER"];
                    if (!validIndexTypes.includes(index.type)) {
                        throw new IndexTypeInvalidError(index.type, schema.dbVendorId);
                    }

                    for (const indexColumn of index.columns) {
                        const columnExists = table.columns.some((col) => col.id === indexColumn.columnId);
                        if (!columnExists) {
                            const columnName =
                                table.columns.find((col) => col.id === indexColumn.columnId)?.name ||
                                indexColumn.columnId;
                            throw new IndexColumnNotExistError(columnName, index.name);
                        }
                    }

                    const indexDef = index.columns
                        .map((ic) => `${ic.columnId}:${ic.sortDir}`)
                        .sort()
                        .join(",");
                    if (indexDefinitions.has(indexDef)) {
                        const existingIndex = table.indexes.find(
                            (i) =>
                                i.columns
                                    .map((ic) => `${ic.columnId}:${ic.sortDir}`)
                                    .sort()
                                    .join(",") === indexDef && i.id !== index.id,
                        );
                        throw new DuplicateIndexDefinitionError(index.name, existingIndex?.name || "unknown");
                    }
                    indexDefinitions.add(indexDef);
                }

                const relationshipNames = new Set<string>();

                for (const relationship of table.relationships) {
                    if (relationshipNames.has(relationship.name)) {
                        throw new RelationshipNameNotUniqueError(relationship.name, table.id);
                    }
                    relationshipNames.add(relationship.name);

                    if (!relationship.columns || relationship.columns.length === 0) {
                        throw new RelationshipEmptyError(relationship.name);
                    }

                    const targetTableExists = schema.tables.some((t) => t.id === relationship.tgtTableId);
                    if (!targetTableExists) {
                        throw new RelationshipTargetTableNotExistError(relationship.name, relationship.tgtTableId);
                    }
                }
            }
        }

        for (const schema of database.schemas) {
            const visited = new Set<string>();
            const recursionStack = new Set<string>();

            const detectCycle = (tableId: string): boolean => {
                if (recursionStack.has(tableId)) {
                    return true;
                }
                if (visited.has(tableId)) {
                    return false;
                }

                visited.add(tableId);
                recursionStack.add(tableId);

                const table = schema.tables.find((t) => t.id === tableId);
                if (table) {
                    for (const relationship of table.relationships) {
                        if (detectCycle(relationship.tgtTableId)) {
                            const srcTableName = table.name;
                            const tgtTable = schema.tables.find((t) => t.id === relationship.tgtTableId);
                            const tgtTableName = tgtTable?.name || relationship.tgtTableId;
                            throw new RelationshipCyclicReferenceError(srcTableName, tgtTableName);
                        }
                    }
                }

                recursionStack.delete(tableId);
                return false;
            };

            for (const table of schema.tables) {
                if (!visited.has(table.id)) {
                    detectCycle(table.id);
                }
            }
        }

        return database;
    },
    ...schemaHandlers,
    ...tableHandlers,
    ...columnHandlers,
    ...indexHandlers,
    ...constraintHandlers,
    ...relationshipHandlers,
};
