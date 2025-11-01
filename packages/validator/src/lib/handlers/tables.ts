import {
  SchemaNotExistError,
  TableNameNotUniqueError,
  TableNotExistError,
  TableNameNotInvalidError,
  TableNameChangeSameError,
} from "../errors";
import { Database, Schema, TABLE, Table } from "../types";
import { relationshipHandlers } from "./relationships";

export interface TableHandlers {
  createTable: (
    database: Database,
    schemaId: Schema["id"],
    table: Omit<Table, "schemaId" | "createdAt" | "updatedAt">,
  ) => Database;
  deleteTable: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
  ) => Database;
  changeTableName: (
    database: Database,
    schemaId: Schema["id"],
    tableId: Table["id"],
    newName: Table["name"],
  ) => Database;
}

export const tableHandlers: TableHandlers = {
  createTable: (database, schemaId, table) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const tableNotUnique = schema.tables.find((t) => t.name === table.name);
    if (tableNotUnique) throw new TableNameNotUniqueError(table.name);

    const isValidTable = TABLE.shape.name.safeParse(table.name);
    if (!isValidTable.success)
      throw new TableNameNotInvalidError(
        table.name,
        isValidTable.error.message,
      );

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: [
                ...s.tables,
                {
                  ...table,
                  createdAt: new Date(),
                  updatedAt: new Date(),
                  schemaId,
                },
              ],
            }
          : s,
      ),
    };
  },
  deleteTable: (database, schemaId, tableId) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const tableToDelete = schema.tables.find((t) => t.id === tableId);
    if (!tableToDelete) throw new TableNotExistError(tableId);

    let currentDatabase = structuredClone(database);
    const relationshipsToDelete: Set<string> = new Set();

    for (const table of schema.tables) {
      for (const relationship of table.relationships) {
        if (
          relationship.srcTableId === tableId ||
          relationship.tgtTableId === tableId
        ) {
          relationshipsToDelete.add(relationship.id);
        }
      }
    }

    for (const relationshipId of relationshipsToDelete) {
      currentDatabase = relationshipHandlers.deleteRelationship(
        currentDatabase,
        schemaId,
        relationshipId,
      );
    }

    const updatedSchema = currentDatabase.schemas.find(
      (s) => s.id === schemaId,
    )!;
    const finalSchema = {
      ...updatedSchema,
      tables: updatedSchema.tables.filter((t) => t.id !== tableId),
    };

    return {
      ...currentDatabase,
      schemas: currentDatabase.schemas.map((s) =>
        s.id === schemaId ? finalSchema : s,
      ),
    };
  },
  changeTableName: (database, schemaId, tableId, newName) => {
    const schema = database.schemas.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const isValidTableName = TABLE.shape.name.safeParse(newName);
    if (!isValidTableName.success)
      throw new TableNameNotInvalidError(
        newName,
        isValidTableName.error.message,
      );

    const tableNotUnique = schema.tables.find(
      (t) => t.name === newName && t.id !== tableId,
    );
    if (tableNotUnique) throw new TableNameNotUniqueError(newName);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);

    if (newName === table.name) throw new TableNameChangeSameError(tableId);

    return {
      ...database,
      schemas: database.schemas.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: s.tables.map((t) =>
                t.id === tableId
                  ? { ...t, updatedAt: new Date(), name: newName }
                  : t,
              ),
            }
          : s,
      ),
    };
  },
};
