import { makeAutoObservable, observable } from 'mobx';
import {
  ERD_VALIDATOR,
  type Database,
  type Schema,
  type Table,
  type Column,
  type Index,
  type IndexColumn,
  type Constraint,
  type ConstraintColumn,
  type Relationship,
  type RelationshipColumn,
} from '@schemafy/validator';

interface IDLE {
  state: 'idle';
}

interface LOADING {
  state: 'loading';
}

interface LOADED {
  state: 'loaded';
}

export class ErdStore {
  database: Database | null = null;
  loadingState: IDLE | LOADING | LOADED = { state: 'idle' };

  constructor() {
    // database는 깊은 observable이 되면 내부 배열이 MobX ObservableArray(Proxy)로 래핑되어
    // validator 내부의 structuredClone에서 복제가 실패할 수 있다. 참조형으로만 추적한다.
    makeAutoObservable(this, { database: observable.ref }, { autoBind: true });
  }

  // state
  load(database: Database) {
    this.loadingState = { state: 'loading' };
    this.database = database;
    this.loadingState = { state: 'loaded' };
  }

  reset() {
    this.database = null;
    this.loadingState = { state: 'idle' };
  }

  validate() {
    if (!this.database) {
      throw new Error('Database is not loaded');
    }

    try {
      ERD_VALIDATOR.validate(this.database);
    } catch (e) {
      console.error(e);
      throw e;
    }
  }

  private update(updater: (db: Database) => Database) {
    if (!this.database) {
      throw new Error('Database is not loaded');
    }
    try {
      const next = updater(this.database);
      this.database = next;
      this.loadingState = { state: 'loaded' };
    } catch (e) {
      console.error(e);
      throw e;
    }
  }

  // schemas
  createSchema(schema: Omit<Schema, 'createdAt' | 'updatedAt'>) {
    this.update((db) => ERD_VALIDATOR.createSchema(db, schema));
  }

  changeSchemaName(schemaId: Schema['id'], newName: Schema['name']) {
    this.update((db) => ERD_VALIDATOR.changeSchemaName(db, schemaId, newName));
  }

  deleteSchema(schemaId: Schema['id']) {
    this.update((db) => ERD_VALIDATOR.deleteSchema(db, schemaId));
  }

  // tables
  createTable(
    schemaId: Schema['id'],
    table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>,
  ) {
    this.update((db) => ERD_VALIDATOR.createTable(db, schemaId, table));
  }

  changeTableName(
    schemaId: Schema['id'],
    tableId: Table['id'],
    newName: Table['name'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeTableName(db, schemaId, tableId, newName),
    );
  }

  deleteTable(schemaId: Schema['id'], tableId: Table['id']) {
    this.update((db) => ERD_VALIDATOR.deleteTable(db, schemaId, tableId));
  }

  // columns
  createColumn(
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.createColumn(db, schemaId, tableId, column),
    );
  }

  deleteColumn(
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.deleteColumn(db, schemaId, tableId, columnId),
    );
  }

  changeColumnName(
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newName: Column['name'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeColumnName(db, schemaId, tableId, columnId, newName),
    );
  }

  changeColumnType(
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    dataType: Column['dataType'],
    lengthScale?: Column['lengthScale'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeColumnType(
        db,
        schemaId,
        tableId,
        columnId,
        dataType,
        lengthScale,
      ),
    );
  }

  changeColumnPosition(
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newPosition: Column['ordinalPosition'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeColumnPosition(
        db,
        schemaId,
        tableId,
        columnId,
        newPosition,
      ),
    );
  }

  changeColumnNullable(
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    nullable: boolean,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeColumnNullable(
        db,
        schemaId,
        tableId,
        columnId,
        nullable,
      ),
    );
  }

  // indexes
  createIndex(
    schemaId: Schema['id'],
    tableId: Table['id'],
    index: Omit<Index, 'tableId'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.createIndex(db, schemaId, tableId, index),
    );
  }

  deleteIndex(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.deleteIndex(db, schemaId, tableId, indexId),
    );
  }

  changeIndexName(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    newName: Index['name'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeIndexName(db, schemaId, tableId, indexId, newName),
    );
  }

  addColumnToIndex(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumn: Omit<IndexColumn, 'indexId'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.addColumnToIndex(
        db,
        schemaId,
        tableId,
        indexId,
        indexColumn,
      ),
    );
  }

  removeColumnFromIndex(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumnId: IndexColumn['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.removeColumnFromIndex(
        db,
        schemaId,
        tableId,
        indexId,
        indexColumnId,
      ),
    );
  }

  // constraints
  createConstraint(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraint: Omit<Constraint, 'tableId'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.createConstraint(db, schemaId, tableId, constraint),
    );
  }

  deleteConstraint(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.deleteConstraint(db, schemaId, tableId, constraintId),
    );
  }

  changeConstraintName(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    newName: Constraint['name'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeConstraintName(
        db,
        schemaId,
        tableId,
        constraintId,
        newName,
      ),
    );
  }

  addColumnToConstraint(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumn: Omit<ConstraintColumn, 'constraintId'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.addColumnToConstraint(
        db,
        schemaId,
        tableId,
        constraintId,
        constraintColumn,
      ),
    );
  }

  removeColumnFromConstraint(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.removeColumnFromConstraint(
        db,
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      ),
    );
  }

  // relationships
  createRelationship(schemaId: Schema['id'], relationship: Relationship) {
    this.update((db) =>
      ERD_VALIDATOR.createRelationship(db, schemaId, relationship),
    );
  }

  deleteRelationship(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.deleteRelationship(db, schemaId, relationshipId),
    );
  }

  changeRelationshipName(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    newName: Relationship['name'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeRelationshipName(
        db,
        schemaId,
        relationshipId,
        newName,
      ),
    );
  }

  changeRelationshipCardinality(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    cardinality: Relationship['cardinality'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeRelationshipCardinality(
        db,
        schemaId,
        relationshipId,
        cardinality,
      ),
    );
  }

  addColumnToRelationship(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumn: Omit<RelationshipColumn, 'relationshipId'>,
  ) {
    this.update((db) =>
      ERD_VALIDATOR.addColumnToRelationship(
        db,
        schemaId,
        relationshipId,
        relationshipColumn,
      ),
    );
  }

  removeColumnFromRelationship(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.removeColumnFromRelationship(
        db,
        schemaId,
        relationshipId,
        relationshipColumnId,
      ),
    );
  }
}

export const erdStore = new ErdStore();
