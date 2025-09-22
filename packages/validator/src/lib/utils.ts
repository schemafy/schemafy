import {
  SchemaNotExistError,
  SchemaNameInvalidError,
  SchemaNameNotUniqueError,
  TableNameInvalidError,
  TableNotExistError,
  ColumnNotExistError,
  ColumnNameInvalidError,
  ColumnPositionInvalidError,
  IndexNotExistError,
  IndexNameInvalidError,
  IndexColumnNotExistError,
  ConstraintNotExistError,
  ConstraintNameInvalidError,
  ConstraintColumnNotExistError,
  RelationshipNotExistError,
  RelationshipNameInvalidError,
  RelationshipColumnNotExistError,
} from './errors';
import {
  Database,
  SCHEMA,
  Schema,
  TABLE,
  Table,
  COLUMN,
  Column,
  INDEX,
  Index,
  IndexColumn,
  Constraint,
  ConstraintColumn,
  RELATIONSHIP,
  Relationship,
  RelationshipColumn,
} from './types';

interface ERDValidator {
  changeSchemaName: (database: Database, schemaId: Schema['id'], newName: Schema['name']) => Database;
  createSchema: (database: Database, schema: Omit<Schema, 'createdAt' | 'updatedAt'>) => Database;
  deleteSchema: (database: Database, schemaId: Schema['id']) => Database;

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

  createConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraint: Omit<Constraint, 'tableId'>
  ) => Database;
  deleteConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id']
  ) => Database;
  changeConstraintName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    newName: Constraint['name']
  ) => Database;
  addColumnToConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumn: Omit<ConstraintColumn, 'constraintId'>
  ) => Database;
  removeColumnFromConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id']
  ) => Database;

  createRelationship: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationship: Relationship
  ) => Database;
  deleteRelationship: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationshipId: Relationship['id']
  ) => Database;
  changeRelationshipName: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationshipId: Relationship['id'],
    newName: Relationship['name']
  ) => Database;
  changeRelationshipCardinality: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationshipId: Relationship['id'],
    cardinality: Relationship['cardinality']
  ) => Database;
  addColumnToRelationship: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationshipId: Relationship['id'],
    relationshipColumn: Omit<RelationshipColumn, 'relationshipId'>
  ) => Database;
  removeColumnFromRelationship: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id']
  ) => Database;
}

export const ERD_VALIDATOR: ERDValidator = (() => {
  return {
    // schema related functions
    changeSchemaName: (database, schemaId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const result = SCHEMA.shape.name.safeParse(newName);
      if (!result.success) throw new SchemaNameInvalidError(newName);

      const existingSchema = database.projects.find((schema) => schema.name === newName && schema.id !== schemaId);
      if (existingSchema) throw new SchemaNameNotUniqueError(newName, existingSchema.id);

      return {
        ...database,
        projects: database.projects.map((schema) =>
          schema.id === schemaId ? { ...schema, name: newName, updatedAt: new Date() } : schema
        ),
      };
    },
    createSchema: (database, schema) => {
      const result = SCHEMA.safeParse(schema);
      if (!result.success) throw new SchemaNameInvalidError(schema.name);

      const existingSchema = database.projects.find((s) => s.name === schema.name);
      if (existingSchema) throw new SchemaNameNotUniqueError(schema.name, existingSchema.id);

      return {
        ...database,
        projects: [...database.projects, { ...schema, createdAt: new Date(), updatedAt: new Date() }],
      };
    },
    deleteSchema: (database, schemaId) => {
      const schema = database.projects.find((s) => s.id === schemaId);

      if (!schema) throw new SchemaNotExistError(schemaId);
      if (schema.deletedAt) throw new SchemaNotExistError(schemaId);

      return {
        ...database,
        projects: database.projects.filter((s) => s.id !== schemaId),
      };
    },

    // table related functions
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

      return {
        ...database,
        projects: database.projects.map((s) =>
          s.id === schemaId
            ? {
                ...s,
                updatedAt: new Date(),
                tables: s.tables.filter((t) => t.id !== tableId),
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

    // column related functions
    createColumn: (database, schemaId, tableId, column) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const result = COLUMN.omit({ id: true, tableId: true, createdAt: true, updatedAt: true }).safeParse(column);
      if (!result.success) throw new ColumnNameInvalidError(column.name);

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
                        columns: [...t.columns, { ...column, tableId, createdAt: new Date(), updatedAt: new Date() }],
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    deleteColumn: (database, schemaId, tableId, columnId) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const column = table.columns.find((c) => c.id === columnId);
      if (!column) throw new ColumnNotExistError(columnId);
      if (column.deletedAt) throw new ColumnNotExistError(columnId);

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
                        // 컬럼 삭제
                        columns: t.columns.filter((c) => c.id !== columnId),
                        // 인덱스에서 해당 컬럼 제거, 빈 인덱스는 삭제
                        indexes: t.indexes
                          .map((idx) => ({
                            ...idx,
                            columns: idx.columns.filter((ic) => ic.columnId !== columnId),
                          }))
                          .filter((idx) => idx.columns.length > 0),
                        // 제약조건에서 해당 컬럼 제거, 빈 제약조건은 삭제
                        constraints: t.constraints
                          .map((constraint) => ({
                            ...constraint,
                            columns: constraint.columns.filter((cc) => cc.columnId !== columnId),
                          }))
                          .filter((constraint) => constraint.columns.length > 0),
                        // 관계에서 해당 컬럼 제거, 빈 관계는 삭제
                        relationships: t.relationships
                          .map((rel) => ({
                            ...rel,
                            columns: rel.columns.filter(
                              (rc) => rc.srcColumnId !== columnId && rc.tgtColumnId !== columnId
                            ),
                          }))
                          .filter((rel) => rel.columns.length > 0),
                      }
                    : {
                        ...t,
                        updatedAt: new Date(),
                        // 다른 테이블의 관계에서도 해당 컬럼 참조 제거
                        relationships: t.relationships
                          .map((rel) => ({
                            ...rel,
                            columns: rel.columns.filter(
                              (rc) => rc.srcColumnId !== columnId && rc.tgtColumnId !== columnId
                            ),
                          }))
                          .filter((rel) => rel.columns.length > 0),
                      }
                ),
              }
            : s
        ),
      };
    },
    changeColumnName: (database, schemaId, tableId, columnId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const column = table.columns.find((c) => c.id === columnId);
      if (!column) throw new ColumnNotExistError(columnId);
      if (column.deletedAt) throw new ColumnNotExistError(columnId);

      const result = COLUMN.shape.name.safeParse(newName);
      if (!result.success) throw new ColumnNameInvalidError(newName);

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
                        columns: t.columns.map((c) =>
                          c.id === columnId ? { ...c, updatedAt: new Date(), name: newName } : c
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
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const column = table.columns.find((c) => c.id === columnId);
      if (!column) throw new ColumnNotExistError(columnId);
      if (column.deletedAt) throw new ColumnNotExistError(columnId);

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
                        columns: t.columns.map((c) =>
                          c.id === columnId
                            ? { ...c, updatedAt: new Date(), dataType, lengthScale: lengthScale || c.lengthScale }
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
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const column = table.columns.find((c) => c.id === columnId);
      if (!column) throw new ColumnNotExistError(columnId);
      if (column.deletedAt) throw new ColumnNotExistError(columnId);

      const activeColumns = table.columns.filter((c) => !c.deletedAt);
      if (newPosition < 1 || newPosition > activeColumns.length) {
        throw new ColumnPositionInvalidError(newPosition, activeColumns.length);
      }

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
                        columns: t.columns.map((c) =>
                          c.id === columnId ? { ...c, updatedAt: new Date(), ordinalPosition: newPosition } : c
                        ),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },

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

    createConstraint: (database, schemaId, tableId, constraint) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      // Validate constraint name directly since CONSTRAINT has refinement
      if (!constraint.name || typeof constraint.name !== 'string') {
        throw new ConstraintNameInvalidError(constraint.name);
      }

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
                        constraints: [...t.constraints, { ...constraint, tableId }],
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    deleteConstraint: (database, schemaId, tableId, constraintId) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const constraint = table.constraints.find((c) => c.id === constraintId);
      if (!constraint) throw new ConstraintNotExistError(constraintId);

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
                        constraints: t.constraints.filter((c) => c.id !== constraintId),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    changeConstraintName: (database, schemaId, tableId, constraintId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const constraint = table.constraints.find((c) => c.id === constraintId);
      if (!constraint) throw new ConstraintNotExistError(constraintId);

      // Validate constraint name directly since CONSTRAINT has refinement
      if (!newName || typeof newName !== 'string') {
        throw new ConstraintNameInvalidError(newName);
      }

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
                        constraints: t.constraints.map((c) => (c.id === constraintId ? { ...c, name: newName } : c)),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    addColumnToConstraint: (database, schemaId, tableId, constraintId, constraintColumn) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const constraint = table.constraints.find((c) => c.id === constraintId);
      if (!constraint) throw new ConstraintNotExistError(constraintId);

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
                        constraints: t.constraints.map((c) =>
                          c.id === constraintId
                            ? {
                                ...c,
                                columns: [...c.columns, { ...constraintColumn, constraintId }],
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
    removeColumnFromConstraint: (database, schemaId, tableId, constraintId, constraintColumnId) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const constraint = table.constraints.find((c) => c.id === constraintId);
      if (!constraint) throw new ConstraintNotExistError(constraintId);

      const constraintColumn = constraint.columns.find((cc) => cc.id === constraintColumnId);
      if (!constraintColumn) throw new ConstraintColumnNotExistError(constraintColumnId);

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
                        // 제약조건에서 컬럼 제거 후, 빈 제약조건은 삭제
                        constraints: t.constraints
                          .map((c) =>
                            c.id === constraintId
                              ? {
                                  ...c,
                                  columns: c.columns.filter((cc) => cc.id !== constraintColumnId),
                                }
                              : c
                          )
                          .filter((c) => c.columns.length > 0),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },

    createRelationship: (database, schemaId, tableId, relationship) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const result = RELATIONSHIP.omit({ id: true }).safeParse(relationship);
      if (!result.success) throw new RelationshipNameInvalidError(relationship.name);

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
                        relationships: [...t.relationships, relationship],
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    deleteRelationship: (database, schemaId, tableId, relationshipId) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const relationship = table.relationships.find((r) => r.id === relationshipId);
      if (!relationship) throw new RelationshipNotExistError(relationshipId);

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
                        relationships: t.relationships.filter((r) => r.id !== relationshipId),
                      }
                    : {
                        ...t,
                        updatedAt: new Date(),
                        // 다른 테이블에서도 같은 관계 ID 참조 제거
                        relationships: t.relationships.filter((r) => r.id !== relationshipId),
                      }
                ),
              }
            : s
        ),
      };
    },
    changeRelationshipName: (database, schemaId, tableId, relationshipId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const relationship = table.relationships.find((r) => r.id === relationshipId);
      if (!relationship) throw new RelationshipNotExistError(relationshipId);

      const result = RELATIONSHIP.shape.name.safeParse(newName);
      if (!result.success) throw new RelationshipNameInvalidError(newName);

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
                        relationships: t.relationships.map((r) =>
                          r.id === relationshipId ? { ...r, name: newName } : r
                        ),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    changeRelationshipCardinality: (database, schemaId, tableId, relationshipId, cardinality) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const relationship = table.relationships.find((r) => r.id === relationshipId);
      if (!relationship) throw new RelationshipNotExistError(relationshipId);

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
                        relationships: t.relationships.map((r) =>
                          r.id === relationshipId ? { ...r, cardinality } : r
                        ),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    addColumnToRelationship: (database, schemaId, tableId, relationshipId, relationshipColumn) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const relationship = table.relationships.find((r) => r.id === relationshipId);
      if (!relationship) throw new RelationshipNotExistError(relationshipId);

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
                        relationships: t.relationships.map((r) =>
                          r.id === relationshipId
                            ? {
                                ...r,
                                columns: [...r.columns, { ...relationshipColumn, relationshipId }],
                              }
                            : r
                        ),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
    removeColumnFromRelationship: (database, schemaId, tableId, relationshipId, relationshipColumnId) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const table = schema.tables.find((t) => t.id === tableId);
      if (!table) throw new TableNotExistError(tableId);
      if (table.deletedAt) throw new TableNotExistError(tableId);

      const relationship = table.relationships.find((r) => r.id === relationshipId);
      if (!relationship) throw new RelationshipNotExistError(relationshipId);

      const relationshipColumn = relationship.columns.find((rc) => rc.id === relationshipColumnId);
      if (!relationshipColumn) throw new RelationshipColumnNotExistError(relationshipColumnId);

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
                        // 관계에서 컬럼 제거 후, 빈 관계는 삭제
                        relationships: t.relationships
                          .map((r) =>
                            r.id === relationshipId
                              ? {
                                  ...r,
                                  columns: r.columns.filter((rc) => rc.id !== relationshipColumnId),
                                }
                              : r
                          )
                          .filter((r) => r.columns.length > 0),
                      }
                    : t
                ),
              }
            : s
        ),
      };
    },
  };
})();
