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
import { Column, COLUMN, Constraint, Database, Index, Relationship, Schema, Table } from '../types';

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
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    if (table.columns.length === 1) throw new TableEmptyColumnError(tableId);

    const updateIndexes: Index[] = table.indexes.map((idx) => ({
      ...idx,
      isAffected: idx.columns.some((ic) => ic.columnId === columnId),
      columns: idx.columns.filter((ic) => ic.columnId !== columnId),
    }));

    const updateConstraints: Constraint[] = table.constraints.map((constraint) => ({
      ...constraint,
      isAffected: constraint.columns.some((cc) => cc.columnId === columnId),
      columns: constraint.columns.filter((cc) => cc.columnId !== columnId),
    }));

    const updateRelationships: Relationship[] = table.relationships.map((rel) => ({
      ...rel,
      isAffected: rel.columns.some((rc) => rc.fkColumnId === columnId || rc.refColumnId === columnId),
      columns: rel.columns.filter((rc) => rc.fkColumnId !== columnId && rc.refColumnId !== columnId),
    }));

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: true,
            columns: t.columns.filter((c) => c.id !== columnId),
            indexes: updateIndexes,
            constraints: updateConstraints,
            relationships: updateRelationships,
          }
        : { ...t, isAffected: true }
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
