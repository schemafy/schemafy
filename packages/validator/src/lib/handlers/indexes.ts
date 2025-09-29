import { SchemaNotExistError, TableNotExistError, IndexNotExistError, IndexColumnNotExistError } from '../errors';
import { Database, Schema, Table, Index, IndexColumn } from '../types';

export interface IndexHandlers {
  createIndex: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    index: Omit<Index, 'tableId'>
  ) => Database;
  deleteIndex: (database: Database, schemaId: Schema['id'], tableId: Table['id'], indexId: Index['id']) => Database;
  changeIndexName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    newName: Index['name']
  ) => Database;
  addColumnToIndex: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumn: Omit<IndexColumn, 'indexId'>
  ) => Database;
  removeColumnFromIndex: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumnId: IndexColumn['id']
  ) => Database;
}

export const indexHandlers: IndexHandlers = {
  createIndex: (database, schemaId, tableId, index) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

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
                      indexes: [...t.indexes, { ...index, tableId }],
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  deleteIndex: (database, schemaId, tableId, indexId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

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
                      indexes: t.indexes.filter((i) => i.id !== indexId),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  changeIndexName: (database, schemaId, tableId, indexId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

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
                      indexes: t.indexes.map((i) => (i.id === indexId ? { ...i, name: newName } : i)),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  addColumnToIndex: (database, schemaId, tableId, indexId, indexColumn) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

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
                      indexes: t.indexes.map((i) =>
                        i.id === indexId
                          ? {
                              ...i,
                              columns: [...i.columns, { ...indexColumn, indexId }],
                            }
                          : i
                      ),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
  removeColumnFromIndex: (database, schemaId, tableId, indexId, indexColumnId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

    const indexColumn = index.columns.find((ic) => ic.id === indexColumnId);
    if (!indexColumn) throw new IndexColumnNotExistError(indexColumnId);

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
                      indexes: t.indexes
                        .map((i) =>
                          i.id === indexId
                            ? {
                                ...i,
                                columns: i.columns.filter((ic) => ic.id !== indexColumnId),
                              }
                            : i
                        )
                        .filter((i) => i.columns.length > 0),
                    }
                  : t
              ),
            }
          : s
      ),
    };
  },
};
