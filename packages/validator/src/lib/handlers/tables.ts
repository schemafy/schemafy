import { SchemaNotExistError, TableNameInvalidError, TableNotExistError } from '../errors';
import { Database, TABLE, Schema, Table } from '../types';
import * as helper from '../helper';

export interface TableHandlers {
  createTable: (
    database: Database,
    schemaId: Schema['id'],
    table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>
  ) => Database;
  deleteTable: (database: Database, schemaId: Schema['id'], tableId: Table['id']) => Database;
  changeTableName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    newName: Table['name']
  ) => Database;
}

export const tableHandlers: TableHandlers = {
  createTable: (database, schemaId, table) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const result = TABLE.safeParse(table);
    if (!result.success) throw new TableNameInvalidError(table.name);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              tables: [...s.tables, { ...table, createdAt: new Date(), updatedAt: new Date(), schemaId }],
            }
          : s
      ),
    };
  },
  deleteTable: (database, schemaId, tableId) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    // Find all relationships that reference this table
    const affectedRelationships = helper.findRelationshipsByTable(schema, tableId);

    // Collect all columns from the deleted table that are referenced in relationships
    const deletedTableColumnIds = new Set<string>();
    affectedRelationships.forEach(({ relationships }) => {
      relationships.forEach((rel) => {
        rel.columns.forEach((rc) => {
          if (rel.srcTableId === tableId) {
            deletedTableColumnIds.add(rc.srcColumnId);
          }
          if (rel.tgtTableId === tableId) {
            deletedTableColumnIds.add(rc.tgtColumnId);
          }
        });
      });
    });

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables
                .filter((t) => t.id !== tableId) // Remove the deleted table
                .map((t) => {
                  // For remaining tables, clean up:
                  // 1. Remove relationships that reference the deleted table
                  // 2. Remove orphaned FK columns and their constraints/indexes
                  let updatedTable = {
                    ...t,
                    updatedAt: new Date(),
                    relationships: t.relationships.filter((r) => r.srcTableId !== tableId && r.tgtTableId !== tableId),
                  };

                  // Find columns that were FKs to the deleted table
                  const orphanedColumns: string[] = [];
                  affectedRelationships.forEach(({ table: affectedTable, relationships }) => {
                    if (affectedTable.id === t.id) {
                      relationships.forEach((rel) => {
                        if (rel.tgtTableId === tableId) {
                          // This table has FK columns pointing to the deleted table
                          rel.columns.forEach((rc) => {
                            orphanedColumns.push(rc.srcColumnId);
                          });
                        }
                      });
                    }
                  });

                  // Remove orphaned FK columns
                  if (orphanedColumns.length > 0) {
                    updatedTable = {
                      ...updatedTable,
                      columns: updatedTable.columns.filter((c) => !orphanedColumns.includes(c.id)),
                      // Remove indexes that reference orphaned columns
                      indexes: updatedTable.indexes
                        .map((idx) => ({
                          ...idx,
                          columns: idx.columns.filter((ic) => !orphanedColumns.includes(ic.columnId)),
                        }))
                        .filter((idx) => idx.columns.length > 0),
                      // Remove constraints that reference orphaned columns
                      constraints: updatedTable.constraints
                        .map((constraint) => ({
                          ...constraint,
                          columns: constraint.columns.filter((cc) => !orphanedColumns.includes(cc.columnId)),
                        }))
                        .filter((constraint) => constraint.columns.length > 0),
                    };
                  }

                  return updatedTable;
                }),
            }
          : {
              ...s,
              // 다른 스키마의 테이블들에서도 삭제되는 테이블을 참조하는 관계들 제거
              tables: s.tables.map((t) => ({
                ...t,
                relationships: t.relationships.filter((r) => r.srcTableId !== tableId && r.tgtTableId !== tableId),
              })),
            }
      ),
    };
  },
  changeTableName: (database, schemaId, tableId, newName) => {
    const schema = database.projects.find((s) => s.id === schemaId);
    if (!schema) throw new SchemaNotExistError(schemaId);

    const table = schema.tables.find((t) => t.id === tableId);
    if (!table) throw new TableNotExistError(tableId);
    if (table.deletedAt) throw new TableNotExistError(tableId);

    return {
      ...database,
      projects: database.projects.map((s) =>
        s.id === schemaId
          ? {
              ...s,
              updatedAt: new Date(),
              tables: s.tables.map((t) => (t.id === tableId ? { ...t, updatedAt: new Date(), name: newName } : t)),
            }
          : s
      ),
    };
  },
};
