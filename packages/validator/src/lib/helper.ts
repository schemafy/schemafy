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
