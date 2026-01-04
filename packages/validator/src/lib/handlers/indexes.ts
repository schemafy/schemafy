import {
  DuplicateIndexDefinitionError,
  IndexColumnNotExistError,
  IndexColumnNotUniqueError,
  IndexColumnSortDirInvalidError,
  IndexNameNotUniqueError,
  IndexNotExistError,
  IndexParseInvalidError,
  IndexTypeInvalidError,
  SchemaNotExistError,
  TableNotExistError,
} from "../errors";
import { INDEX } from "../types";
import type { Database, Index, IndexColumn, Schema, Table } from "../types";

export interface IndexHandlers {
  createIndex: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    index: Omit<Index, "tableId">,
  ) => Database;
  deleteIndex: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    indexId: Index["id"],
  ) => Database;
  changeIndexName: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    indexId: Index["id"],
    newName: Index["name"],
  ) => Database;
  addColumnToIndex: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    indexId: Index["id"],
    indexColumn: Omit<IndexColumn, "indexId">,
  ) => Database;
  removeColumnFromIndex: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    indexId: Index["id"],
    indexColumnId: IndexColumn["id"],
  ) => Database;
}

export const indexHandlers: IndexHandlers = {
  createIndex: (database, schemaId, tableId, index) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const indexNotUnique = table.indexes.find((i) => i.name === index.name);
    if (indexNotUnique) throw new IndexNameNotUniqueError(index.name, tableId);

    const indexParse = INDEX.shape.name.safeParse(index.name);
    if (!indexParse.success) {
      throw new IndexParseInvalidError(indexParse.error.message);
    }

    const existingIndex = table.indexes.find((i) => {
      const existingIndexDef = i.columns
        .map((ic) => `${ic.columnId}:${ic.sortDir}`)
        .sort()
        .join(",");
      const newIndexDef = index.columns
        .map((ic) => `${ic.columnId}:${ic.sortDir}`)
        .sort()
        .join(",");
      return existingIndexDef === newIndexDef && i.type === index.type;
    });

    if (existingIndex)
      throw new DuplicateIndexDefinitionError(index.name, existingIndex.name);

    const indexColumnNotUnique = index.columns.map((ic) => ic.columnId);
    if (indexColumnNotUnique.length !== new Set(indexColumnNotUnique).size)
      throw new IndexColumnNotUniqueError(index.name);

    const validIndexTypes = ["BTREE", "HASH", "FULLTEXT", "SPATIAL", "OTHER"];
    if (!validIndexTypes.includes(index.type))
      throw new IndexTypeInvalidError(index.type, schema.dbVendorId);

    for (const indexColumn of index.columns) {
      const validSortDirs = ["ASC", "DESC"];
      if (!validSortDirs.includes(indexColumn.sortDir)) {
        throw new IndexColumnSortDirInvalidError(
          indexColumn.sortDir,
          index.name,
        );
      }
    }

    const changeTable: Table = {
      ...table,
      isAffected: true,
      indexes: [...table.indexes, { ...index, isAffected: true, tableId }],
    };

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: s.tables.map((t) => (t.id === tableId ? changeTable : t)),
          }
        : s,
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  deleteIndex: (database, schemaId, tableId, indexId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId, tableId);

    const changeTable: Table = {
      ...table,
      isAffected: true,
      indexes: table.indexes.filter((i) => i.id !== indexId),
    };

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: s.tables.map((t) => (t.id === tableId ? changeTable : t)),
          }
        : s,
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  changeIndexName: (database, schemaId, tableId, indexId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId, tableId);

    const indexNotUnique = table.indexes.find(
      (i) => i.name === newName && i.id !== indexId,
    );
    if (indexNotUnique) throw new IndexNameNotUniqueError(newName, tableId);

    const changeTable: Table = {
      ...table,
      isAffected: true,
      indexes: table.indexes.map((i) =>
        i.id === indexId ? { ...i, name: newName, isAffected: true } : i,
      ),
    };

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: s.tables.map((t) => (t.id === tableId ? changeTable : t)),
          }
        : s,
    );

    return {
      ...database,
      isAffected: true,
      schemas: changeSchemas,
    };
  },
  addColumnToIndex: (database, schemaId, tableId, indexId, indexColumn) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId, tableId);

    const indexColumnNotUnique = index.columns.find(
      (ic) => ic.columnId === indexColumn.columnId,
    );
    if (indexColumnNotUnique) throw new IndexColumnNotUniqueError(index.name);

    const changeTable: Table = {
      ...table,
      isAffected: true,
      indexes: table.indexes.map((i) =>
        i.id === indexId
          ? {
              ...i,
              isAffected: true,
              columns: [
                ...i.columns,
                { ...indexColumn, indexId, isAffected: true },
              ],
            }
          : i,
      ),
    };

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: s.tables.map((t) => (t.id === tableId ? changeTable : t)),
          }
        : s,
    );
    return { ...database, isAffected: true, schemas: changeSchemas };
  },
  removeColumnFromIndex: (
    database,
    schemaId,
    tableId,
    indexId,
    indexColumnId,
  ) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    const index = table.indexes.find((i) => i.id === indexId);
    if (!index) throw new IndexNotExistError(indexId, tableId);

    const indexColumn = index.columns.find((ic) => ic.id === indexColumnId);
    if (!indexColumn)
      throw new IndexColumnNotExistError(indexColumnId, index.name);

    const changeTable: Table = {
      ...table,
      isAffected: true,
      indexes: table.indexes
        .map((i) =>
          i.id === indexId
            ? {
                ...i,
                isAffected: i.columns.some((ic) => ic.id === indexColumnId),
                columns: i.columns.filter((ic) => ic.id !== indexColumnId),
              }
            : i,
        )
        .filter((i) => i.columns.length > 0),
    };

    const changeSchemas: Schema[] = database.schemas.map((s) =>
      s.id === schemaId
        ? {
            ...s,
            isAffected: true,
            tables: s.tables.map((t) => (t.id === tableId ? changeTable : t)),
          }
        : s,
    );

    return { ...database, isAffected: true, schemas: changeSchemas };
  },
};
