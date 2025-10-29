import {
  ColumnDataTypeInvalidError,
  ColumnLengthRequiredError,
  ColumnNameInvalidError,
  ColumnNameInvalidFormatError,
  ColumnNameIsReservedKeywordError,
  ColumnNameNotUniqueError,
  ColumnNotExistError,
  ColumnPrecisionRequiredError,
  MultipleAutoIncrementColumnsError,
  SchemaNotExistError,
  TableEmptyColumnError,
  TableNotExistError,
} from '../errors';
import * as helper from '../helper';
import { Column, COLUMN, Constraint, Database, Schema, Table } from '../types';

export interface ColumnHandlers {
  createColumn: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>
  ) => Database;
  deleteColumn: (database: Database, schemaId: Schema['id'], tableId: Table['id'], columnId: Column['id']) => Database;
  changeColumnName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newName: Column['name']
  ) => Database;
  changeColumnType: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    dataType: Column['dataType'],
    lengthScale?: Column['lengthScale']
  ) => Database;
  changeColumnPosition: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newPosition: Column['ordinalPosition']
  ) => Database;
  changeColumnNullable: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    nullable: boolean
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
      if (autoIncrementColumn) throw new MultipleAutoIncrementColumnsError(tableId);
    }

    const reservedKeywords = ['TABLE', 'SELECT', 'CREATE'];
    if (reservedKeywords.some((keyword) => column.name.includes(keyword)))
      throw new ColumnNameIsReservedKeywordError(column.name);

    if (column.dataType) {
      const precisionRequired = ['DECIMAL', 'NUMERIC']; // NOTE: 이 내용 외부에서 요청받아서 처리하도록 수정 필요할듯.
      if (precisionRequired.includes(column.dataType) && !column.lengthScale)
        throw new ColumnPrecisionRequiredError(column.dataType);

      const lengthScaleRequired = ['VARCHAR', 'CHAR']; // NOTE: 이 내용 외부에서 요청받아서 처리하도록 수정 필요할듯.
      if (lengthScaleRequired.includes(column.dataType) && !column.lengthScale)
        throw new ColumnLengthRequiredError(column.dataType);

      const vendorValid = helper.categorizedMysqlDataTypes.includes(column.dataType);
      if (!vendorValid) throw new ColumnDataTypeInvalidError(column.dataType);
    }

    if (!helper.isValidColumnName(column.name)) throw new ColumnNameInvalidFormatError(column.name);

    const newColumn: Column = {
      ...column,
      tableId,
      createdAt: new Date(),
      updatedAt: new Date(),
      isAffected: true,
    };

    const columnNotUnique = table.columns.find((c) => c.name === column.name);
    if (columnNotUnique) throw new ColumnNameNotUniqueError(column.name);

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId ? { ...t, isAffected: true, columns: [...t.columns, newColumn] } : { ...t, isAffected: true }
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    return {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };
  },
  deleteColumn: (database, schemaId, tableId, columnId) => {
    // NOTE: 코드 전반으로 index, constraint, relationship에서 컬럼이 없을 때, 해당 인덱스, 컨스트레인트, 릴레이션쉽을 삭제하도록 구현되어 있음.
    //       이 부분에 참고하여서, 외부에서 아이디 매핑시에, 특정 아이디를 찾을 떄 못 찾을 수도 있음. (해당 부분 고려 필요)

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    if (table.columns.length === 1) throw new TableEmptyColumnError(tableId);

    const isPrimaryKey = table.constraints.some(
      (constraint) => constraint.kind === 'PRIMARY_KEY' && constraint.columns.some((cc) => cc.columnId === columnId)
    );

    let updatedDatabase: Database = {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) => {
                if (t.id === tableId) {
                  return {
                    ...t,
                    isAffected: true,
                    columns: t.columns.filter((c) => c.id !== columnId),
                    indexes: t.indexes
                      .map((idx) => ({
                        ...idx,
                        isAffected: idx.columns.some((ic) => ic.columnId === columnId),
                        columns: idx.columns.filter((ic) => ic.columnId !== columnId),
                      }))
                      .filter((idx) => idx.columns.length > 0),
                    constraints: t.constraints
                      .map((constraint) => ({
                        ...constraint,
                        isAffected: constraint.columns.some((cc) => cc.columnId === columnId),
                        columns: constraint.columns.filter((cc) => cc.columnId !== columnId),
                      }))
                      .filter((constraint) => constraint.columns.length > 0),
                    relationships: t.relationships
                      .map((rel) => ({
                        ...rel,
                        isAffected: rel.columns.some((rc) => rc.fkColumnId === columnId || rc.refColumnId === columnId),
                        columns: rel.columns.filter((rc) => rc.fkColumnId !== columnId && rc.refColumnId !== columnId),
                      }))
                      .filter((rel) => rel.columns.length > 0),
                  };
                }
                return t;
              }),
            }
          : s
      ),
    };

    if (isPrimaryKey) {
      const deleteCascadingForeignKeys = (
        currentSchema: Schema,
        parentTableId: string,
        deletedPkColumnId: string,
        visited: Set<string> = new Set()
      ): Schema => {
        if (visited.has(parentTableId)) return currentSchema;
        visited.add(parentTableId);

        let updatedSchema = currentSchema;

        for (const table of updatedSchema.tables) {
          for (const rel of table.relationships) {
            if (rel.tgtTableId === parentTableId) {
              const childTable = updatedSchema.tables.find((t) => t.id === rel.srcTableId);
              if (!childTable) continue;

              const fkColumnsToDelete = rel.columns
                .filter((relCol) => relCol.refColumnId === deletedPkColumnId)
                .map((relCol) => relCol.fkColumnId);

              if (fkColumnsToDelete.length > 0) {
                updatedSchema = {
                  ...updatedSchema,
                  isAffected: true,
                  tables: updatedSchema.tables.map((t) =>
                    t.id === childTable.id
                      ? {
                          ...t,
                          isAffected: t.columns.some((c) => fkColumnsToDelete.includes(c.id)),
                          columns: t.columns.filter((col) => !fkColumnsToDelete.includes(col.id)),
                          indexes: t.indexes
                            .map((idx) => ({
                              ...idx,
                              isAffected: idx.columns.some((ic) => fkColumnsToDelete.includes(ic.columnId)),
                              columns: idx.columns.filter((ic) => !fkColumnsToDelete.includes(ic.columnId)),
                            }))
                            .filter((idx) => idx.columns.length > 0),
                          constraints: t.constraints
                            .map((constraint) => ({
                              ...constraint,
                              isAffected: constraint.columns.some((cc) => fkColumnsToDelete.includes(cc.columnId)),
                              columns: constraint.columns.filter((cc) => !fkColumnsToDelete.includes(cc.columnId)),
                            }))
                            .filter((constraint) => constraint.columns.length > 0),
                          relationships: t.relationships
                            .map((relationship) => ({
                              ...relationship,
                              isAffected: relationship.columns.some(
                                (rc) =>
                                  fkColumnsToDelete.includes(rc.fkColumnId) ||
                                  fkColumnsToDelete.includes(rc.refColumnId)
                              ),
                              columns: relationship.columns.filter(
                                (rc) =>
                                  !fkColumnsToDelete.includes(rc.fkColumnId) &&
                                  !fkColumnsToDelete.includes(rc.refColumnId)
                              ),
                            }))
                            .filter((relationship) => relationship.columns.length > 0),
                        }
                      : t
                  ),
                };

                if (rel.kind === 'IDENTIFYING') {
                  for (const fkColumnId of fkColumnsToDelete) {
                    updatedSchema = deleteCascadingForeignKeys(
                      structuredClone(updatedSchema),
                      rel.tgtTableId,
                      fkColumnId,
                      new Set(visited)
                    );
                  }
                }
              }
            }
          }
        }

        return updatedSchema;
      };

      const updatedSchema = updatedDatabase.schemas.find((s) => s.id === schemaId)!;
      const cascadedSchema = deleteCascadingForeignKeys(structuredClone(updatedSchema), tableId, columnId);

      updatedDatabase = {
        ...updatedDatabase,
        isAffected: true,
        schemas: updatedDatabase.schemas.map((s) => (s.id === schemaId ? { ...cascadedSchema, isAffected: true } : s)),
      };
    }

    return updatedDatabase;
  },
  changeColumnName: (database, schemaId, tableId, columnId, newName) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

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
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      columns: t.columns.map((c) =>
                        c.id === columnId ? { ...c, name: newName, isAffected: true } : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeColumnType: (database, schemaId, tableId, columnId, dataType, lengthScale) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      columns: t.columns.map((c) =>
                        c.id === columnId
                          ? {
                              ...c,
                              isAffected: true,
                              dataType,
                              lengthScale: lengthScale || c.lengthScale,
                            }
                          : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeColumnPosition: (database, schemaId, tableId, columnId, newPosition) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    return {
      ...database,
      isAffected: true,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              isAffected: true,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      isAffected: true,
                      columns: t.columns.map((c) =>
                        c.id === columnId
                          ? {
                              ...c,
                              isAffected: true,
                              updatedAt: new Date(),
                              ordinalPosition: newPosition,
                            }
                          : c
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeColumnNullable: (database, schemaId, tableId, columnId, nullable) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?
    // NOTE: 컬럼에서 nullable 변경이 있는게 맞는지? constraint에서 해당 부분을 처리하는게 옳아보이는데

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const currentNullable = helper.isColumnNullable(table, columnId);
    if (currentNullable === nullable) return database;

    const updatedColumn: Column = {
      ...column,
      isAffected: true,
    };

    let currentDatabase = columnHandlers.deleteColumn(structuredClone(database), schemaId, tableId, columnId);

    currentDatabase = columnHandlers.createColumn(structuredClone(currentDatabase), schemaId, tableId, updatedColumn);

    if (!nullable) {
      const updatedSchema = currentDatabase.schemas.find((s) => s.id === schemaId)!;
      const updatedTable = updatedSchema.tables.find((t) => t.id === tableId)!;

      const hasNotNull = updatedTable.constraints.some(
        (constraint) => constraint.kind === 'NOT_NULL' && constraint.columns.some((cc) => cc.columnId === columnId)
      );

      if (!hasNotNull) {
        const newConstraintId = `constraint_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        const newConstraint: Constraint = {
          id: newConstraintId,
          tableId,
          name: `nn_${column.name}`,
          kind: 'NOT_NULL' as const,
          checkExpr: null,
          defaultExpr: null,
          columns: [
            {
              id: `cc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
              constraintId: newConstraintId,
              columnId,
              seqNo: 1,
              isAffected: true,
            },
          ],
          isAffected: true,
        };

        currentDatabase = {
          ...currentDatabase,
          isAffected: true,
          schemas: currentDatabase.schemas.map((s) =>
            s.id === schemaId
              ? {
                  ...s,
                  isAffected: true,
                  tables: s.tables.map((t) =>
                    t.id === tableId
                      ? {
                          ...t,
                          isAffected: true,
                          constraints: [...t.constraints, newConstraint],
                        }
                      : t
                  ),
                }
              : s
          ),
        };
      }
    }

    return currentDatabase;
  },
};
