import {
  DuplicateIndexDefinitionError,
  IndexColumnNotExistError,
  IndexColumnNotUniqueError,
  IndexColumnSortDirInvalidError,
  IndexNameNotUniqueError,
  IndexNotExistError,
  IndexTypeInvalidError,
  SchemaNotExistError,
  TableNotExistError,
} from "../errors";
import { Database, Index, IndexColumn, Schema, Table } from "../types";

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
    if (indexNotUnique) throw new IndexNameNotUniqueError(index.name);

    if (
      table.indexes.find((i) => {
        const existingIndexDef = i.columns
          .map((ic) => `${ic.columnId}:${ic.sortDir}`)
          .sort()
          .join(",");
        const newIndexDef = index.columns
          .map((ic) => `${ic.columnId}:${ic.sortDir}`)
          .sort()
          .join(",");
        return existingIndexDef === newIndexDef && i.type === index.type;
      })
    )
      throw new DuplicateIndexDefinitionError(index.name, "existing");

    const indexColumnNotUnique = index.columns.map((ic) => ic.columnId);
    if (indexColumnNotUnique.length !== new Set(indexColumnNotUnique).size)
      throw new IndexColumnNotUniqueError(index.id);

    const validIndexTypes = ["BTREE", "HASH", "FULLTEXT", "SPATIAL", "OTHER"];
    if (!validIndexTypes.includes(index.type))
      throw new IndexTypeInvalidError(index.type);

    for (const indexColumn of index.columns) {
      const validSortDirs = ["ASC", "DESC"];
      if (!validSortDirs.includes(indexColumn.sortDir)) {
        throw new IndexColumnSortDirInvalidError(indexColumn.sortDir);
      }
    }

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
                      indexes: [
                        ...t.indexes,
                        { ...index, isAffected: true, tableId },
                      ],
                    }
                  : t,
              ),
            }
          : s,
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
                      isAffected: t.indexes.some((i) => i.id === indexId),
                      indexes: t.indexes.filter((i) => i.id !== indexId),
                    }
                  : t,
              ),
            }
          : s,
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

    const indexNotUnique = table.indexes.find((i) => i.name === newName);
    if (indexNotUnique) throw new IndexNameNotUniqueError(newName);

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
                      indexes: t.indexes.map((i) =>
                        i.id === indexId
                          ? { ...i, name: newName, isAffected: true }
                          : i,
                      ),
                    }
                  : t,
              ),
            }
          : s,
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

    const indexColumnNotUnique = index.columns.find(
      (ic) => ic.columnId === indexColumn.columnId,
    );
    if (indexColumnNotUnique)
      throw new IndexColumnNotUniqueError(indexColumn.columnId);

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
                      indexes: t.indexes.map((i) =>
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
                    }
                  : t,
              ),
            }
          : s,
      ),
    };
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
    if (!index) throw new IndexNotExistError(indexId);

    const indexColumn = index.columns.find((ic) => ic.id === indexColumnId);
    if (!indexColumn) throw new IndexColumnNotExistError(indexColumnId);

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
                      indexes: t.indexes
                        .map((i) =>
                          i.id === indexId
                            ? {
                                ...i,
                                isAffected: i.columns.some(
                                  (ic) => ic.id === indexColumnId,
                                ),
                                columns: i.columns.filter(
                                  (ic) => ic.id !== indexColumnId,
                                ),
                              }
                            : i,
                        )
                        .filter((i) => i.columns.length > 0),
                    }
                  : t,
              ),
            }
          : s,
      ),
    };
  },
};
