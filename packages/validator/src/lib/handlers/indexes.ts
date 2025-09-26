import {
  SchemaNotExistError,
  TableNotExistError,
  IndexNotExistError,
  IndexNameInvalidError,
  IndexColumnNotExistError,
} from '../errors';
import { Database, INDEX, Schema, Table, Index, IndexColumn } from '../types';

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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const result = INDEX.omit({ id: true, tableId: true }).safeParse(index);
    if (!result.success) throw new IndexNameInvalidError(index.name);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

    const result = INDEX.shape.name.safeParse(newName);
    if (!result.success) throw new IndexNameInvalidError(newName);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
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
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId);

    const indexColumn = index.columns.find((ic) => ic.id === indexColumnId);
    if (!indexColumn) throw new IndexColumnNotExistError(indexColumnId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? {
                      ...t,
                      updatedAt: new Date(),
                      // 인덱스에서 컬럼 제거 후, 빈 인덱스는 삭제
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
