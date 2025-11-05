import type { Column, Schema, Table } from "..";

export const isColumnNullable = (table: Table, columnId: Column["id"]): boolean => {
    const notNullConstraint = table.constraints.find(
        (constraint) => constraint.kind === "NOT_NULL" && constraint.columns.some((cc) => cc.columnId === columnId),
    );
    return !notNullConstraint;
};

export const detectCircularReference = (
    schema: Schema,
    fromTableId: Table["id"],
    toTableId: Table["id"],
    visited: Set<Table["id"]> = new Set(),
): boolean => {
    if (visited.has(fromTableId)) return true;
    if (fromTableId === toTableId) return true;

    visited.add(fromTableId);

    const referencedTables = new Set<Table["id"]>();
    schema.tables.forEach((table) => {
        table.relationships.forEach((rel) => {
            if (rel.srcTableId === fromTableId) {
                referencedTables.add(rel.tgtTableId);
            }
        });
    });

    for (const referencedTableId of referencedTables) {
        if (detectCircularReference(schema, referencedTableId, toTableId, new Set(visited))) {
            return true;
        }
    }

    return false;
};

export const isValidColumnName = (str: string): boolean => {
    if (/-/.test(str)) {
        return false;
    }

    if (/\s/.test(str)) {
        return false;
    }

    if (/^[0-9]/.test(str)) {
        return false;
    }

    if (/[^a-zA-Z0-9]/.test(str)) {
        return false;
    }

    return true;
};

export const categorizedMysqlDataTypes = [
    "TINYINT",
    "SMALLINT",
    "MEDIUMINT",
    "INT",
    "INTEGER",
    "BIGINT",
    "FLOAT",
    "DOUBLE",
    "REAL",
    "DECIMAL",
    "NUMERIC",
    "BIT",
    "BOOL",
    "BOOLEAN",
    "CHAR",
    "VARCHAR",
    "TINYTEXT",
    "TEXT",
    "MEDIUMTEXT",
    "LONGTEXT",
    "BINARY",
    "VARBINARY",
    "BLOB",
    "TINYBLOB",
    "MEDIUMBLOB",
    "LONGBLOB",
    "ENUM",
    "SET",
    "DATE",
    "TIME",
    "DATETIME",
    "TIMESTAMP",
    "YEAR",
    "GEOMETRY",
    "POINT",
    "LINESTRING",
    "POLYGON",
    "MULTIPOINT",
    "MULTILINESTRING",
    "MULTIPOLYGON",
    "GEOMETRYCOLLECTION",
    "JSON",
];

export const propagatePrimaryKeysToChildren = (
    currentSchema: Schema,
    parentTableId: Table["id"],
    pkColumns: Column[],
    visited: Set<Table["id"]> = new Set(),
): Schema => {
    if (visited.has(parentTableId)) return currentSchema;
    visited.add(parentTableId);

    const parentTable = currentSchema.tables.find((t) => t.id === parentTableId);
    if (!parentTable) return currentSchema;

    let updatedSchema = currentSchema;

    for (const table of updatedSchema.tables) {
        for (const rel of table.relationships) {
            if (rel.tgtTableId === parentTableId) {
                const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);
                if (!childTable) continue;

                let updatedChildTable = childTable;
                let currentRel = rel;
                const newlyCreatedFkColumns: Column[] = [];

                for (const pkColumn of pkColumns) {
                    const relColumn = currentRel.columns.find((rc) => rc.refColumnId === pkColumn.id);

                    if (relColumn && relColumn.fkColumnId) {
                        const existingColumn = childTable.columns.find((c) => c.id === relColumn.fkColumnId);
                        if (existingColumn) continue;
                    }

                    const newColumnId = `fkcol_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
                    const columnName = `${parentTable.name}_${pkColumn.name}`;

                    const newFkColumn: Column = {
                        ...pkColumn,
                        id: newColumnId,
                        tableId: childTable.id,
                        name: columnName,
                        ordinalPosition: updatedChildTable.columns.length + 1,
                        createdAt: new Date(),
                        updatedAt: new Date(),
                    };

                    updatedChildTable = {
                        ...updatedChildTable,
                        columns: [...updatedChildTable.columns, newFkColumn],
                    };

                    newlyCreatedFkColumns.push(newFkColumn);

                    if (relColumn) {
                        const updatedRel = {
                            ...currentRel,
                            columns: currentRel.columns.map((rc) =>
                                rc.id === relColumn.id ? { ...rc, fkColumnId: newColumnId } : rc,
                            ),
                        };

                        currentRel = updatedRel;

                        updatedChildTable = {
                            ...updatedChildTable,
                            relationships: updatedChildTable.relationships.map((r) =>
                                r.id === rel.id ? updatedRel : r,
                            ),
                        };
                    } else {
                        const newRelColumn = {
                            id: `rel_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                            relationshipId: rel.id,
                            fkColumnId: newColumnId,
                            refColumnId: pkColumn.id,
                            seqNo: currentRel.columns.length + 1,
                        };

                        const updatedRel = {
                            ...currentRel,
                            columns: [...currentRel.columns, newRelColumn],
                        };

                        currentRel = updatedRel;

                        updatedChildTable = {
                            ...updatedChildTable,
                            relationships: updatedChildTable.relationships.map((r) =>
                                r.id === rel.id ? updatedRel : r,
                            ),
                        };
                    }
                }

                updatedSchema = {
                    ...updatedSchema,
                    tables: updatedSchema.tables.map((t) => (t.id === childTable.id ? updatedChildTable : t)),
                };

                if (rel.kind === "IDENTIFYING" && newlyCreatedFkColumns.length > 0) {
                    const childPkConstraint = updatedChildTable.constraints.find((c) => c.kind === "PRIMARY_KEY");
                    if (childPkConstraint) {
                        const newPkColumns = [];

                        for (const fkColumn of newlyCreatedFkColumns) {
                            newPkColumns.push({
                                id: `constraint_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                                constraintId: childPkConstraint.id,
                                columnId: fkColumn.id,
                                seqNo: childPkConstraint.columns.length + newPkColumns.length + 1,
                            });
                        }

                        if (newPkColumns.length > 0) {
                            const updatedPkConstraint = {
                                ...childPkConstraint,
                                columns: [...childPkConstraint.columns, ...newPkColumns],
                            };

                            updatedChildTable = {
                                ...updatedChildTable,
                                constraints: updatedChildTable.constraints.map((c) =>
                                    c.id === childPkConstraint.id ? updatedPkConstraint : c,
                                ),
                            };

                            updatedSchema = {
                                ...updatedSchema,
                                tables: updatedSchema.tables.map((t) =>
                                    t.id === childTable.id ? updatedChildTable : t,
                                ),
                            };
                        }
                    }

                    updatedSchema = propagatePrimaryKeysToChildren(
                        structuredClone(updatedSchema),
                        rel.srcTableId,
                        newlyCreatedFkColumns,
                        new Set(visited),
                    );
                }
            }
        }
    }

    return updatedSchema;
};
