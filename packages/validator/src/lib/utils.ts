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
  moveTableToSchema: (database: Database, tableId: Table['id'], newSchemaId: Schema['id']) => Database;

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

  createRelationship: (
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    relationship: Omit<Relationship, 'id'>
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
    relationshipColumn: Omit<RelationshipColumn, 'id' | 'relationshipId'>
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
    changeSchemaName: (database, schemaId, newName) => {
      const schema = database.projects.find((s) => s.id === schemaId);
      if (!schema) throw new SchemaNotExistError(schemaId);

      const result = SCHEMA.shape.name.safeParse(newName);
      if (!result.success) throw new SchemaNameInvalidError(newName);

      const existingSchema = database.projects.find((schema) => schema.name === newName && schema.id !== schemaId);
      if (existingSchema) throw new SchemaNameNotUniqueError(newName, existingSchema.id);

      return {
        ...database,
        projects: database.projects.map((schema) => (schema.id === schemaId ? { ...schema, name: newName } : schema)),
      };
    },
    createSchema: (database, schema) => {
      throw new Error('createSchema: Not implemented yet');
    },
    deleteSchema: (database, schemaId) => {
      throw new Error('deleteSchema: Not implemented yet');
    },

    createTable: (database, schemaId, table) => {
      throw new Error('createTable: Not implemented yet');
    },
    deleteTable: (database, schemaId, tableId) => {
      throw new Error('deleteTable: Not implemented yet');
    },
    changeTableName: (database, tableId, newName) => {
      throw new Error('changeTableName: Not implemented yet');
    },
    moveTableToSchema: (database, tableId, newSchemaId) => {
      throw new Error('moveTableToSchema: Not implemented yet');
    },

    createColumn: (database, schemaId, tableId, column) => {
      throw new Error('createColumn: Not implemented yet');
    },
    deleteColumn: (database, schemaId, tableId, columnId) => {
      throw new Error('deleteColumn: Not implemented yet');
    },
    changeColumnName: (database, schemaId, tableId, columnId, newName) => {
      throw new Error('changeColumnName: Not implemented yet');
    },
    changeColumnType: (database, schemaId, tableId, columnId, dataType, lengthScale) => {
      throw new Error('changeColumnType: Not implemented yet');
    },
    changeColumnPosition: (database, schemaId, tableId, columnId, newPosition) => {
      throw new Error('changeColumnPosition: Not implemented yet');
    },

    createIndex: (database, schemaId, tableId, index) => {
      throw new Error('createIndex: Not implemented yet');
    },
    deleteIndex: (database, schemaId, tableId, indexId) => {
      throw new Error('deleteIndex: Not implemented yet');
    },
    changeIndexName: (database, schemaId, tableId, indexId, newName) => {
      throw new Error('changeIndexName: Not implemented yet');
    },
    addColumnToIndex: (database, schemaId, tableId, indexId, indexColumn) => {
      throw new Error('addColumnToIndex: Not implemented yet');
    },
    removeColumnFromIndex: (database, schemaId, tableId, indexId, indexColumnId) => {
      throw new Error('removeColumnFromIndex: Not implemented yet');
    },

    createConstraint: (database, schemaId, tableId, constraint) => {
      throw new Error('createConstraint: Not implemented yet');
    },
    deleteConstraint: (database, schemaId, tableId, constraintId) => {
      throw new Error('deleteConstraint: Not implemented yet');
    },
    changeConstraintName: (database, schemaId, tableId, constraintId, newName) => {
      throw new Error('changeConstraintName: Not implemented yet');
    },
    addColumnToConstraint: (database, schemaId, tableId, constraintId, constraintColumn) => {
      throw new Error('addColumnToConstraint: Not implemented yet');
    },
    removeColumnFromConstraint: (database, schemaId, tableId, constraintId, constraintColumnId) => {
      throw new Error('removeColumnFromConstraint: Not implemented yet');
    },

    createRelationship: (database, schemaId, tableId, relationship) => {
      throw new Error('createRelationship: Not implemented yet');
    },
    deleteRelationship: (database, schemaId, tableId, relationshipId) => {
      throw new Error('deleteRelationship: Not implemented yet');
    },
    changeRelationshipName: (database, schemaId, tableId, relationshipId, newName) => {
      throw new Error('changeRelationshipName: Not implemented yet');
    },
    changeRelationshipCardinality: (database, schemaId, tableId, relationshipId, cardinality) => {
      throw new Error('changeRelationshipCardinality: Not implemented yet');
    },
    addColumnToRelationship: (database, schemaId, tableId, relationshipId, relationshipColumn) => {
      throw new Error('addColumnToRelationship: Not implemented yet');
    },
    removeColumnFromRelationship: (database, schemaId, tableId, relationshipId, relationshipColumnId) => {
      throw new Error('removeColumnFromRelationship: Not implemented yet');
    },
  };
})();
