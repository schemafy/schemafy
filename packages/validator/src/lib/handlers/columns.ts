import {
  ColumnDataTypeInvalidError,
  ColumnLengthRequiredError,
  ColumnInvalidError,
  ColumnInvalidFormatError,
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
}

export const columnHandlers: ColumnHandlers = {
  createColumn: (database, schemaId, tableId, column) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const result = COLUMN.shape.name.safeParse(column.name);
    if (!result.success) {
      throw new ColumnInvalidError(column.id);
    }

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

    if (!helper.isValidColumnName(column.name)) throw new ColumnInvalidFormatError(column.name);

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

    const columnNotUnique = table.columns.find((c) => c.name === newName && c.id !== columnId);
    if (columnNotUnique) throw new ColumnNameNotUniqueError(newName);

    const result = COLUMN.shape.name.safeParse(newName);
    if (!result.success) throw new ColumnInvalidError({ name: newName });

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId
        ? {
            ...t,
            isAffected: true,
            columns: t.columns.map((c) => (c.id === columnId ? { ...c, name: newName, isAffected: true } : c)),
          }
        : t
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
  changeColumnType: (database, schemaId, tableId, columnId, dataType, lengthScale) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const changeColumns: Column[] = table.columns.map((c) =>
      c.id === columnId ? { ...c, isAffected: true, dataType, lengthScale: lengthScale || c.lengthScale } : c
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId ? { ...t, isAffected: true, columns: changeColumns } : t
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  changeColumnPosition: (database, schemaId, tableId, columnId, newPosition) => {
    // NOTE: 변경에 의해서도 이전과 이후가 같은 상황에 대해선 고려가 필요없는지?

    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const column = table.columns.find((c) => c.id === columnId);
    if (!column) throw new ColumnNotExistError(columnId);

    const changeColumns: Column[] = table.columns.map((c) =>
      c.id === columnId ? { ...c, isAffected: true, ordinalPosition: newPosition } : c
    );

    const changeTables: Table[] = schema.tables.map((t) =>
      t.id === tableId ? { ...t, isAffected: true, columns: changeColumns } : t
    );

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId ? { ...s, isAffected: true, tables: changeTables } : s
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
};
