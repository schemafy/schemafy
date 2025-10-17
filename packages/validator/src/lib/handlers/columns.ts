import {
  SchemaNotExistError,
  TableNotExistError,
  ColumnNotExistError,
  ColumnNameNotUniqueError,
  ColumnNameInvalidError,
  TableEmptyColumnError,
  MultipleAutoIncrementColumnsError,
  ColumnNameIsReservedKeywordError,
  ColumnNameInvalidFormatError,
  ColumnPrecisionRequiredError,
  ColumnLengthRequiredError,
  ColumnDataTypeInvalidError,
} from "../errors";
import { Database, Schema, Table, Column, Constraint, COLUMN } from "../types";
import * as helper from "../helper";

export interface ColumnHandlers {
  createColumn: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    column: Omit<Column, "tableId" | "createdAt" | "updatedAt">,
  ) => Database;
  deleteColumn: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    columnId: Column["id"],
  ) => Database;
  changeColumnName: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    columnId: Column["id"],
    newName: Column["name"],
  ) => Database;
  changeColumnType: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    columnId: Column["id"],
    dataType: Column["dataType"],
    lengthScale?: Column["lengthScale"],
  ) => Database;
  changeColumnPosition: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    columnId: Column["id"],
    newPosition: Column["ordinalPosition"],
  ) => Database;
  changeColumnNullable: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    columnId: Column["id"],
    nullable: boolean,
  ) => Database;
}

export const columnHandlers: ColumnHandlers = {
  createColumn: (database, schemaId, tableId, column) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const result = COLUMN.shape.name.safeParse(column.name);
    if (!result.success) throw new ColumnNameInvalidError(column.name);

    if (column.isAutoIncrement) {
      const autoIncrementColumn = table.columns.find((c) => c.isAutoIncrement);
      if (autoIncrementColumn)
        throw new MultipleAutoIncrementColumnsError(tableId);
    }

    const reservedKeywords = ["TABLE", "SELECT", "CREATE"];
    if (reservedKeywords.some((keyword) => column.name.includes(keyword)))
      throw new ColumnNameIsReservedKeywordError(column.name);

    if (column.dataType) {
      const presisionRequired = ["DECIMAL", "NUMERIC"];
      if (presisionRequired.includes(column.dataType) && !column.lengthScale)
        throw new ColumnPrecisionRequiredError(column.dataType);

      const lengthScaleRequired = ["VARCHAR", "CHAR"];
      if (lengthScaleRequired.includes(column.dataType) && !column.lengthScale)
        throw new ColumnLengthRequiredError(column.dataType);

      const vendorValid = helper.categorizedMysqlDataTypes.includes(
        column.dataType,
      );
      if (!vendorValid) throw new ColumnDataTypeInvalidError(column.dataType);
    }

    if (!helper.isValidColumnName(column.name))
      throw new ColumnNameInvalidFormatError(column.name);

    const newColumn = {
      ...column,
      tableId,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    const columnNotUnique = table.columns.find((c) => c.name === column.name);
    if (columnNotUnique) throw new ColumnNameNotUniqueError(column.name);

    let updatedDatabase = {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      columns: [...t.columns, newColumn],
                    }
                  : t,
              ),
            }
          : s,
      ),
    };

    const isPrimaryKey = table.constraints.some(
      (constraint) =>
        constraint.kind === "PRIMARY_KEY" &&
        constraint.columns.some((cc) => cc.columnId === column.id),
    );

    if (isPrimaryKey) {
      const propagateNewPrimaryKey = (
        currentSchema: Schema,
        parentTableId: string,
        newPkColumn: typeof newColumn,
        visited: Set<string> = new Set(),
      ): Schema => {
        if (visited.has(parentTableId)) return currentSchema;
        visited.add(parentTableId);

        let updatedSchema = currentSchema;

        for (const table of updatedSchema.tables) {
          for (const rel of table.relationships) {
            if (rel.tgtTableId === parentTableId) {
              const childTable = updatedSchema.tables.find(
                (t) => t.id === rel.srcTableId,
              );
              if (!childTable) continue;

              const parentTable = updatedSchema.tables.find(
                (t) => t.id === parentTableId,
              )!;
              const existingColumn = childTable.columns.find((c) =>
                c.name.startsWith(`${parentTable.name}_${newPkColumn.name}`),
              );

              if (!existingColumn) {
                const newFkColumnId = `col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
                const columnName = `${parentTable.name}_${newPkColumn.name}`;

                const newFkColumn = {
                  ...newPkColumn,
                  id: newFkColumnId,
                  tableId: childTable.id,
                  name: columnName,
                  ordinalPosition: childTable.columns.length + 1,
                  createdAt: new Date(),
                  updatedAt: new Date(),
                };

                updatedSchema = {
                  ...updatedSchema,
                  tables: updatedSchema.tables.map((t) =>
                    t.id === childTable.id
                      ? {
                          ...t,
                          columns: [...t.columns, newFkColumn],
                        }
                      : t,
                  ),
                };

                const newRelColumn = {
                  id: `rel_col_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, //id 어떻게 하지....
                  relationshipId: rel.id,
                  fkColumnId: newFkColumnId,
                  refColumnId: newPkColumn.id,
                  seqNo: rel.columns.length + 1,
                };

                updatedSchema = {
                  ...updatedSchema,
                  tables: updatedSchema.tables.map((t) =>
                    t.id === parentTableId
                      ? {
                          ...t,
                          relationships: t.relationships.map((r) =>
                            r.id === rel.id
                              ? {
                                  ...r,
                                  columns: [...r.columns, newRelColumn],
                                }
                              : r,
                          ),
                        }
                      : t,
                  ),
                };

                if (rel.kind === "IDENTIFYING") {
                  updatedSchema = propagateNewPrimaryKey(
                    structuredClone(updatedSchema),
                    rel.tgtTableId,
                    newFkColumn,
                    new Set(visited),
                  );
                }
              }
            }
          }
        }

        return updatedSchema;
      };

      const updatedSchema = updatedDatabase.schemas.find(
        (s) => s.id === schemaId,
      )!;
      const propagatedSchema = propagateNewPrimaryKey(
        structuredClone(updatedSchema),
        tableId,
        newColumn,
      );

      updatedDatabase = {
        ...updatedDatabase,
        schemas: updatedDatabase.schemas.map((s) =>
          s.id === schemaId ? propagatedSchema : s,
        ),
      };
    }

    return updatedDatabase;
  },
  deleteColumn: (database, schemaId, tableId, columnId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    if (table.columns.length === 1) throw new TableEmptyColumnError(tableId);

    const isPrimaryKey = table.constraints.some(
      (constraint) =>
        constraint.kind === "PRIMARY_KEY" &&
        constraint.columns.some((cc) => cc.columnId === columnId),
    );

    let updatedDatabase = {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) => {
                if (t.id === tableId) {
                  return {
                    ...t,
                    columns: t.columns.filter((c) => c.id !== columnId),
                    indexes: t.indexes
                      .map((idx) => ({
                        ...idx,
                        columns: idx.columns.filter(
                          (ic) => ic.columnId !== columnId,
                        ),
                      }))
                      .filter((idx) => idx.columns.length > 0),
                    constraints: t.constraints
                      .map((constraint) => ({
                        ...constraint,
                        columns: constraint.columns.filter(
                          (cc) => cc.columnId !== columnId,
                        ),
                      }))
                      .filter((constraint) => constraint.columns.length > 0),
                    relationships: t.relationships
                      .map((rel) => ({
                        ...rel,
                        columns: rel.columns.filter(
                          (rc) =>
                            rc.fkColumnId !== columnId &&
                            rc.refColumnId !== columnId,
                        ),
                      }))
                      .filter((rel) => rel.columns.length > 0),
                  };
                }
                return t;
              }),
            }
          : s,
      ),
    };

    if (isPrimaryKey) {
      const deleteCascadingForeignKeys = (
        currentSchema: Schema,
        parentTableId: string,
        deletedPkColumnId: string,
        visited: Set<string> = new Set(),
      ): Schema => {
        if (visited.has(parentTableId)) return currentSchema;
        visited.add(parentTableId);

        let updatedSchema = currentSchema;

        for (const table of updatedSchema.tables) {
          for (const rel of table.relationships) {
            if (rel.tgtTableId === parentTableId) {
              const childTable = updatedSchema.tables.find(
                (t) => t.id === rel.srcTableId,
              );
              if (!childTable) continue;

              const fkColumnsToDelete = rel.columns
                .filter((relCol) => relCol.refColumnId === deletedPkColumnId)
                .map((relCol) => relCol.fkColumnId);

              if (fkColumnsToDelete.length > 0) {
                updatedSchema = {
                  ...updatedSchema,
                  tables: updatedSchema.tables.map((t) =>
                    t.id === childTable.id
                      ? {
                          ...t,
                          columns: t.columns.filter(
                            (col) => !fkColumnsToDelete.includes(col.id),
                          ),
                          indexes: t.indexes
                            .map((idx) => ({
                              ...idx,
                              columns: idx.columns.filter(
                                (ic) =>
                                  !fkColumnsToDelete.includes(ic.columnId),
                              ),
                            }))
                            .filter((idx) => idx.columns.length > 0),
                          constraints: t.constraints
                            .map((constraint) => ({
                              ...constraint,
                              columns: constraint.columns.filter(
                                (cc) =>
                                  !fkColumnsToDelete.includes(cc.columnId),
                              ),
                            }))
                            .filter(
                              (constraint) => constraint.columns.length > 0,
                            ),
                          relationships: t.relationships
                            .map((relationship) => ({
                              ...relationship,
                              columns: relationship.columns.filter(
                                (rc) =>
                                  !fkColumnsToDelete.includes(rc.fkColumnId) &&
                                  !fkColumnsToDelete.includes(rc.refColumnId),
                              ),
                            }))
                            .filter(
                              (relationship) => relationship.columns.length > 0,
                            ),
                        }
                      : t,
                  ),
                };

                if (rel.kind === "IDENTIFYING") {
                  for (const fkColumnId of fkColumnsToDelete) {
                    updatedSchema = deleteCascadingForeignKeys(
                      structuredClone(updatedSchema),
                      rel.tgtTableId,
                      fkColumnId,
                      new Set(visited),
                    );
                  }
                }
              }
            }
          }
        }

        return updatedSchema;
      };

      const updatedSchema = updatedDatabase.schemas.find(
        (s) => s.id === schemaId,
      )!;
      const cascadedSchema = deleteCascadingForeignKeys(
        structuredClone(updatedSchema),
        tableId,
        columnId,
      );

      updatedDatabase = {
        ...updatedDatabase,
        schemas: updatedDatabase.schemas.map((s) =>
          s.id === schemaId ? cascadedSchema : s,
        ),
      };
    }

    return updatedDatabase;
  },
  changeColumnName: (database, schemaId, tableId, columnId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const columnNotUnique = table.columns.find((c) => c.name === newName);
    if (columnNotUnique) throw new ColumnNameNotUniqueError(newName);

    const result = COLUMN.shape.name.safeParse(newName);
    if (!result.success) throw new ColumnNameInvalidError(newName);

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      columns: t.columns.map((c) =>
                        c.id === columnId ? { ...c, name: newName } : c,
                      ),
                    }
                  : t,
              ),
            }
          : s,
      ),
    };
  },
  changeColumnType: (
    database,
    schemaId,
    tableId,
    columnId,
    dataType,
    lengthScale,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      columns: t.columns.map((c) =>
                        c.id === columnId
                          ? {
                              ...c,
                              dataType,
                              lengthScale: lengthScale || c.lengthScale,
                            }
                          : c,
                      ),
                    }
                  : t,
              ),
            }
          : s,
      ),
    };
  },
  changeColumnPosition: (
    database,
    schemaId,
    tableId,
    columnId,
    newPosition,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      columns: t.columns.map((c) =>
                        c.id === columnId
                          ? {
                              ...c,
                              updatedAt: new Date(),
                              ordinalPosition: newPosition,
                            }
                          : c,
                      ),
                    }
                  : t,
              ),
            }
          : s,
      ),
    };
  },
  changeColumnNullable: (database, schemaId, tableId, columnId, nullable) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const currentNullable = helper.isColumnNullable(table, columnId);
    if (currentNullable === nullable) return database;

    const updatedColumn = {
      ...column,
    };

    let currentDatabase = columnHandlers.deleteColumn(
      structuredClone(database),
      schemaId,
      tableId,
      columnId,
    );

    currentDatabase = columnHandlers.createColumn(
      structuredClone(currentDatabase),
      schemaId,
      tableId,
      updatedColumn,
    );

    if (!nullable) {
      const updatedSchema = currentDatabase.schemas.find(
        (s) => s.id === schemaId,
      )!;
      const updatedTable = updatedSchema.tables.find((t) => t.id === tableId)!;

      const hasNotNull = updatedTable.constraints.some(
        (constraint) =>
          constraint.kind === "NOT_NULL" &&
          constraint.columns.some((cc) => cc.columnId === columnId),
      );

      if (!hasNotNull) {
        const newConstraintId = `constraint_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        const newConstraint: Constraint = {
          id: newConstraintId,
          tableId,
          name: `nn_${column.name}`,
          kind: "NOT_NULL" as const,
          checkExpr: null,
          defaultExpr: null,
          columns: [
            {
              id: `cc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
              constraintId: newConstraintId,
              columnId,
              seqNo: 1,
            },
          ],
        };

        currentDatabase = {
          ...currentDatabase,
          schemas: currentDatabase.schemas.map((s) =>
            s.id === schemaId
              ? {
                  ...s,
                  tables: s.tables.map((t) =>
                    t.id === tableId
                      ? {
                          ...t,
                          constraints: [...t.constraints, newConstraint],
                        }
                      : t,
                  ),
                }
              : s,
          ),
        };
      }
    }

    return currentDatabase;
  },
};
