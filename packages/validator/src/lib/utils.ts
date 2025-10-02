import { SchemaNotExistError, SchemaNameInvalidError, SchemaNameNotUniqueError } from './errors';
import {
  Database,
  SCHEMA,
  Schema,
  Table,
  Column,
  Index,
  IndexColumn,
  Constraint,
  ConstraintColumn,
  Relationship,
  RelationshipColumn,
} from './types';

interface ERDValidator {
  validate: (database: Database) => void;

  changeSchemaName: (database: Database, schemaId: Schema['id'], newName: Schema['name']) => Database;
  createSchema: (database: Database, schema: Omit<Schema, 'id' | 'createdAt' | 'updatedAt'>) => Database;
  deleteSchema: (database: Database, schemaId: Schema['id']) => Database;

  createTable: (
    database: Database,
    schemaId: Schema['id'],
    table: Omit<Table, 'id' | 'schemaId' | 'createdAt' | 'updatedAt'>
  ) => Database;
  deleteTable: (database: Database, schemaId: Schema['id'], tableId: Table['id']) => Database;
  changeTableName: (database: Database, tableId: Table['id'], newName: Table['name']) => Database;

  createColumn: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'id' | 'tableId' | 'createdAt' | 'updatedAt'>
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
    index: Omit<Index, 'id' | 'tableId'>
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
    indexColumn: Omit<IndexColumn, 'id' | 'indexId'>
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
    constraint: Omit<Constraint, 'id' | 'tableId'>
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
    constraintColumn: Omit<ConstraintColumn, 'id' | 'constraintId'>
  ) => Database;
  removeColumnFromConstraint: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id']
  ) => Database;

  createRelationship: (database: Database, schemaId: Schema['id'], relationship: Omit<Relationship, 'id'>) => Database;
  deleteRelationship: (database: Database, schemaId: Schema['id'], relationshipId: Relationship['id']) => Database;
  changeRelationshipName: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    newName: Relationship['name']
  ) => Database;
  changeRelationshipCardinality: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    cardinality: Relationship['cardinality']
  ) => Database;
  addColumnToRelationship: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumn: Omit<RelationshipColumn, 'id' | 'relationshipId'>
  ) => Database;
  removeColumnFromRelationship: (
    database: Database,
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id']
  ) => Database;
}

export const ERD_VALIDATOR: ERDValidator = (() => {
  // FIX: 추후에 삭제 (파라미터 미사용 에러 발생 방지)
  const _use = (..._args: unknown[]) => {};

  // 추가적으로 이걸 백엔드에서 검증하기 위해서, 프론트엔드에서 해당 ERD에 대한 모든 데이터가 필요할 것으로 보이는데, 이를 어떻게 하면 좋을지.
  // 단순히 캐싱으로 처리해야하나?
  return {
    validate: (database) => {
      _use(database);
      // 해당 데이터베이스를 전체적으로 검증하는 것이 필요.
      throw new Error('validate: Not implemented yet');
    },

    changeSchemaName: (database, schemaId, newName) => {
      const schema = database.schemas.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const result = SCHEMA.shape.name.safeParse(newName);
      if (!result.success) throw new SchemaNameInvalidError(newName);

      const existingSchema = database.schemas.find((schema) => schema.name === newName && schema.id !== schemaId);
      if (existingSchema) throw new SchemaNameNotUniqueError(newName, existingSchema.id);

      return {
        ...database,
        schemas: database.schemas.map((schema) => (schema.id === schemaId ? { ...schema, name: newName } : schema)),
      };
    },
    createSchema: (database, schema) => {
      _use(database, schema);
      throw new Error('createSchema: Not implemented yet');
    },
    deleteSchema: (database, schemaId) => {
      _use(database, schemaId);
      throw new Error('deleteSchema: Not implemented yet');
    },

    createTable: (database, schemaId, table) => {
      _use(database, schemaId, table);
      throw new Error('createTable: Not implemented yet');
    },
    deleteTable: (database, schemaId, tableId) => {
      _use(database, schemaId, tableId);
      throw new Error('deleteTable: Not implemented yet');
    },
    changeTableName: (database, tableId, newName) => {
      _use(database, tableId, newName);
      throw new Error('changeTableName: Not implemented yet');
    },

    createColumn: (database, schemaId, tableId, column) => {
      _use(database, schemaId, tableId, column);
      throw new Error('createColumn: Not implemented yet');
    },
    deleteColumn: (database, schemaId, tableId, columnId) => {
      _use(database, schemaId, tableId, columnId);
      throw new Error('deleteColumn: Not implemented yet');
    },
    changeColumnName: (database, schemaId, tableId, columnId, newName) => {
      _use(database, schemaId, tableId, columnId, newName);
      throw new Error('changeColumnName: Not implemented yet');
    },
    changeColumnType: (database, schemaId, tableId, columnId, dataType, lengthScale) => {
      _use(database, schemaId, tableId, columnId, dataType, lengthScale);
      throw new Error('changeColumnType: Not implemented yet');
    },
    changeColumnPosition: (database, schemaId, tableId, columnId, newPosition) => {
      _use(database, schemaId, tableId, columnId, newPosition);
      throw new Error('changeColumnPosition: Not implemented yet');
    },

    createIndex: (database, schemaId, tableId, index) => {
      _use(database, schemaId, tableId, index);
      throw new Error('createIndex: Not implemented yet');
    },
    deleteIndex: (database, schemaId, tableId, indexId) => {
      _use(database, schemaId, tableId, indexId);
      throw new Error('deleteIndex: Not implemented yet');
    },
    changeIndexName: (database, schemaId, tableId, indexId, newName) => {
      _use(database, schemaId, tableId, indexId, newName);
      throw new Error('changeIndexName: Not implemented yet');
    },
    addColumnToIndex: (database, schemaId, tableId, indexId, indexColumn) => {
      _use(database, schemaId, tableId, indexId, indexColumn);
      throw new Error('addColumnToIndex: Not implemented yet');
    },
    removeColumnFromIndex: (database, schemaId, tableId, indexId, indexColumnId) => {
      _use(database, schemaId, tableId, indexId, indexColumnId);
      throw new Error('removeColumnFromIndex: Not implemented yet');
    },

    createConstraint: (database, schemaId, tableId, constraint) => {
      _use(database, schemaId, tableId, constraint);
      throw new Error('createConstraint: Not implemented yet');
    },
    deleteConstraint: (database, schemaId, tableId, constraintId) => {
      _use(database, schemaId, tableId, constraintId);
      throw new Error('deleteConstraint: Not implemented yet');
    },
    changeConstraintName: (database, schemaId, tableId, constraintId, newName) => {
      _use(database, schemaId, tableId, constraintId, newName);
      throw new Error('changeConstraintName: Not implemented yet');
    },
    addColumnToConstraint: (database, schemaId, tableId, constraintId, constraintColumn) => {
      _use(database, schemaId, tableId, constraintId, constraintColumn);
      throw new Error('addColumnToConstraint: Not implemented yet');
    },
    removeColumnFromConstraint: (database, schemaId, tableId, constraintId, constraintColumnId) => {
      _use(database, schemaId, tableId, constraintId, constraintColumnId);
      throw new Error('removeColumnFromConstraint: Not implemented yet');
    },

    createRelationship: (database, schemaId, relationship) => {
      _use(database, schemaId, relationship);
      throw new Error('createRelationship: Not implemented yet');
    },
    deleteRelationship: (database, schemaId, relationshipId) => {
      _use(database, schemaId, relationshipId);
      throw new Error('deleteRelationship: Not implemented yet');
    },
    changeRelationshipName: (database, schemaId, relationshipId, newName) => {
      _use(database, schemaId, relationshipId, newName);
      throw new Error('changeRelationshipName: Not implemented yet');
    },
    changeRelationshipCardinality: (database, schemaId, relationshipId, cardinality) => {
      _use(database, schemaId, relationshipId, cardinality);
      throw new Error('changeRelationshipCardinality: Not implemented yet');
    },
    addColumnToRelationship: (database, schemaId, relationshipId, relationshipColumn) => {
      _use(database, schemaId, relationshipId, relationshipColumn);
      throw new Error('addColumnToRelationship: Not implemented yet');
    },
    removeColumnFromRelationship: (database, schemaId, relationshipId, relationshipColumnId) => {
      _use(database, schemaId, relationshipId, relationshipColumnId);
      throw new Error('removeColumnFromRelationship: Not implemented yet');
    },
  };
})();
