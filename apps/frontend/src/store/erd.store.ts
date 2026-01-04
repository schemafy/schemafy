import { makeAutoObservable, observable, runInAction } from 'mobx';
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
  database: Database;
}

type LoadingState = IDLE | LOADING | LOADED;

type DatabaseExtra = {
  selectedSchemaId?: string;
};

const isDatabaseExtra = (value: unknown): value is DatabaseExtra => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const obj = value as Record<string, unknown>;

  if ('selectedSchemaId' in obj) {
    return (
      obj.selectedSchemaId === undefined ||
      typeof obj.selectedSchemaId === 'string'
    );
  }

  return true;
};

const getDatabaseExtra = (extra: unknown): DatabaseExtra => {
  if (isDatabaseExtra(extra)) {
    return extra;
  }
  return {};
};

export class ErdStore {
  private static instance: ErdStore;
  private static syncedInstance: ErdStore;
  erdState: LoadingState = { state: 'idle' };

  private constructor() {
    // database는 깊은 observable이 되면 내부 배열이 MobX ObservableArray(Proxy)로 래핑되어
    // validator 내부의 structuredClone에서 복제가 실패할 수 있다. 참조형으로만 추적한다.
    makeAutoObservable(
      this,
      { erdState: observable.struct },
      { autoBind: true },
    );
  }

  static getInstance(): ErdStore {
    if (!ErdStore.instance) {
      ErdStore.instance = new ErdStore();
    }
    return ErdStore.instance;
  }

  static getSyncedInstance(): ErdStore {
    if (!ErdStore.syncedInstance) {
      ErdStore.syncedInstance = new ErdStore();
    }
    return ErdStore.syncedInstance;
  }

  // state
  load(database: Database) {
    this.erdState = { state: 'loading' };

    const extra = getDatabaseExtra(database.extra);
    if (!extra.selectedSchemaId && database.schemas.length > 0) {
      database = {
        ...database,
        extra: {
          ...extra,
          selectedSchemaId: database.schemas[0].id,
        },
      };
    }

    this.erdState = { state: 'loaded', database };
  }

  reset() {
    this.erdState = { state: 'idle' };
  }

  get selectedSchemaId(): string | null {
    if (this.erdState.state !== 'loaded') return null;

    const extra = getDatabaseExtra(this.erdState.database.extra);
    return extra.selectedSchemaId || null;
  }

  get selectedSchema(): Schema | null {
    if (this.erdState.state !== 'loaded') return null;

    const selectedSchemaId = this.selectedSchemaId;
    if (!selectedSchemaId) return null;

    return (
      this.erdState.database.schemas.find((s) => s.id === selectedSchemaId) ||
      null
    );
  }

  get database(): Database | null {
    if (this.erdState.state !== 'loaded') return null;
    return this.erdState.database;
  }

  selectSchema(schemaId: string) {
    this.update((db) => {
      const schema = db.schemas.find((s) => s.id === schemaId);
      if (!schema) throw new Error(`Schema ${schemaId} not found`);

      const extra = getDatabaseExtra(db.extra);
      return {
        ...db,
        extra: {
          ...extra,
          selectedSchemaId: schemaId,
        },
      };
    });
  }

  validate() {
    if (this.erdState.state !== 'loaded') {
      throw new Error('Database is not loaded');
    }

    try {
      ERD_VALIDATOR.validate(this.erdState.database);
    } catch (e) {
      console.error(e);
      throw e;
    }
  }

  private update(updater: (db: Database) => Database) {
    if (this.erdState.state !== 'loaded') {
      throw new Error('Database is not loaded');
    }
    try {
      const next = updater(this.erdState.database);
      runInAction(() => {
        this.erdState = { state: 'loaded', database: next };
      });
      console.log(this.erdState);
    } catch (e) {
      console.error(e);
      throw e;
    }
  }

  private replaceIdInArray<T extends { id: string }>(
    items: T[],
    oldId: string,
    newId: string,
  ): T[] {
    return items.map((item) =>
      item.id === oldId ? { ...item, id: newId } : item,
    );
  }

  private updateSchema(
    schemaId: Schema['id'],
    updateFn: (schema: Schema) => Schema,
  ) {
    this.update((db) => ({
      ...db,
      schemas: db.schemas.map((schema) =>
        schema.id !== schemaId ? schema : updateFn(schema),
      ),
    }));
  }

  private updateTable(
    schemaId: Schema['id'],
    tableId: Table['id'],
    updateFn: (table: Table) => Table,
  ) {
    this.updateSchema(schemaId, (schema) => ({
      ...schema,
      tables: schema.tables.map((table) =>
        table.id !== tableId ? table : updateFn(table),
      ),
    }));
  }

  private updateConstraint(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    updateFn: (constraint: Constraint) => Constraint,
  ) {
    this.updateTable(schemaId, tableId, (table) => ({
      ...table,
      constraints: table.constraints.map((constraint) =>
        constraint.id !== constraintId ? constraint : updateFn(constraint),
      ),
    }));
  }

  private updateIndex(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    updateFn: (index: Index) => Index,
  ) {
    this.updateTable(schemaId, tableId, (table) => ({
      ...table,
      indexes: table.indexes.map((index) =>
        index.id !== indexId ? index : updateFn(index),
      ),
    }));
  }

  private updateRelationshipInSchema(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    updateFn: (relationship: Relationship) => Relationship,
  ) {
    this.updateSchema(schemaId, (schema) => ({
      ...schema,
      tables: schema.tables.map((table) => ({
        ...table,
        relationships: table.relationships.map((relationship) =>
          relationship.id !== relationshipId
            ? relationship
            : updateFn(relationship),
        ),
      })),
    }));
  }

  // replace ID
  replaceSchemaId(oldId: Schema['id'], newId: Schema['id']) {
    this.update((db) => {
      const extra = getDatabaseExtra(db.extra);
      const selectedSchemaId =
        extra.selectedSchemaId === oldId ? newId : extra.selectedSchemaId;

      return {
        ...db,
        schemas: this.replaceIdInArray(db.schemas, oldId, newId),
        extra: {
          ...extra,
          selectedSchemaId,
        },
      };
    });
  }

  replaceTableId(
    schemaId: Schema['id'],
    oldId: Table['id'],
    newId: Table['id'],
  ) {
    this.updateSchema(schemaId, (schema) => ({
      ...schema,
      tables: this.replaceIdInArray(schema.tables, oldId, newId),
    }));
  }

  replaceColumnId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    oldId: Column['id'],
    newId: Column['id'],
  ) {
    this.updateTable(schemaId, tableId, (table) => ({
      ...table,
      columns: this.replaceIdInArray(table.columns, oldId, newId),
    }));
  }

  replaceIndexId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    oldId: Index['id'],
    newId: Index['id'],
  ) {
    this.updateTable(schemaId, tableId, (table) => ({
      ...table,
      indexes: this.replaceIdInArray(table.indexes, oldId, newId),
    }));
  }

  replaceConstraintId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    oldId: Constraint['id'],
    newId: Constraint['id'],
  ) {
    this.updateTable(schemaId, tableId, (table) => ({
      ...table,
      constraints: table.constraints.map((constraint) =>
        constraint.id !== oldId
          ? constraint
          : {
              ...constraint,
              id: newId,
              columns: constraint.columns.map((col) => ({
                ...col,
                constraintId: newId,
              })),
            },
      ),
    }));
  }

  replaceConstraintColumnId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    oldId: ConstraintColumn['id'],
    newId: ConstraintColumn['id'],
  ) {
    this.updateConstraint(schemaId, tableId, constraintId, (constraint) => ({
      ...constraint,
      columns: constraint.columns.map((col) =>
        col.id === oldId ? { ...col, id: newId } : col,
      ),
    }));
  }

  replaceConstraintColumnColumnId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    constraintId: Constraint['id'],
    constraintColumnId: ConstraintColumn['id'],
    newColumnId: Column['id'],
  ) {
    this.updateConstraint(schemaId, tableId, constraintId, (constraint) => ({
      ...constraint,
      columns: constraint.columns.map((col) =>
        col.id === constraintColumnId ? { ...col, columnId: newColumnId } : col,
      ),
    }));
  }

  replaceIndexColumnId(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    oldId: IndexColumn['id'],
    newId: IndexColumn['id'],
  ) {
    this.updateIndex(schemaId, tableId, indexId, (index) => ({
      ...index,
      columns: index.columns.map((col) =>
        col.id === oldId ? { ...col, id: newId } : col,
      ),
    }));
  }

  replaceRelationshipColumnId(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    oldId: RelationshipColumn['id'],
    newId: RelationshipColumn['id'],
  ) {
    this.updateRelationshipInSchema(
      schemaId,
      relationshipId,
      (relationship) => ({
        ...relationship,
        columns: relationship.columns.map((col) =>
          col.id === oldId ? { ...col, id: newId } : col,
        ),
      }),
    );
  }

  replaceRelationshipColumnFkId(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    relationshipColumnId: RelationshipColumn['id'],
    newFkColumnId: Column['id'],
  ) {
    this.updateRelationshipInSchema(
      schemaId,
      relationshipId,
      (relationship) => ({
        ...relationship,
        columns: relationship.columns.map((col) =>
          col.id === relationshipColumnId
            ? { ...col, fkColumnId: newFkColumnId }
            : col,
        ),
      }),
    );
  }

  replaceRelationshipId(
    schemaId: Schema['id'],
    oldId: Relationship['id'],
    newId: Relationship['id'],
  ) {
    this.updateSchema(schemaId, (schema) => ({
      ...schema,
      tables: schema.tables.map((table) => ({
        ...table,
        relationships: table.relationships.map((relationship) =>
          relationship.id !== oldId
            ? relationship
            : {
                ...relationship,
                id: newId,
                columns: relationship.columns.map((col) => ({
                  ...col,
                  relationshipId: newId,
                })),
              },
        ),
      })),
    }));
  }

  // schemas
  createSchema(schema: Schema) {
    this.update((db) => ERD_VALIDATOR.createSchema(db, schema));
  }

  changeSchemaName(schemaId: Schema['id'], newName: Schema['name']) {
    this.update((db) => ERD_VALIDATOR.changeSchemaName(db, schemaId, newName));
  }

  deleteSchema(schemaId: Schema['id']) {
    this.update((db) => ERD_VALIDATOR.deleteSchema(db, schemaId));
  }

  updateSchemaExtra(schemaId: Schema['id'], extra: unknown) {
    this.update((db) => {
      return {
        ...db,
        schemas: db.schemas.map((schema) => {
          if (schema.id !== schemaId) return schema;

          return {
            ...schema,
            extra,
          };
        }),
      };
    });
  }

  // tables
  createTable(schemaId: Schema['id'], table: Omit<Table, 'schemaId'>) {
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

  updateTableExtra(
    schemaId: Schema['id'],
    tableId: Table['id'],
    extra: unknown,
  ) {
    this.update((db) => {
      return {
        ...db,
        schemas: db.schemas.map((schema) => {
          if (schema.id !== schemaId) return schema;

          return {
            ...schema,
            tables: schema.tables.map((table) => {
              if (table.id !== tableId) return table;

              return {
                ...table,
                extra,
              };
            }),
          };
        }),
      };
    });
  }

  deleteTable(schemaId: Schema['id'], tableId: Table['id']) {
    this.update((db) => ERD_VALIDATOR.deleteTable(db, schemaId, tableId));
  }

  // columns
  createColumn(
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId'>,
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

  changeIndexType(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    newType: Index['type'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeIndexType(db, schemaId, tableId, indexId, newType),
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

  changeIndexColumnSortDir(
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumnId: IndexColumn['id'],
    newSortDir: IndexColumn['sortDir'],
  ) {
    this.update((db) =>
      ERD_VALIDATOR.changeIndexColumnSortDir(
        db,
        schemaId,
        tableId,
        indexId,
        indexColumnId,
        newSortDir,
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

  updateRelationshipExtra(
    schemaId: Schema['id'],
    relationshipId: Relationship['id'],
    extra: unknown,
  ) {
    this.update((db) => {
      return {
        ...db,
        schemas: db.schemas.map((schema) => {
          if (schema.id !== schemaId) return schema;

          return {
            ...schema,
            tables: schema.tables.map((table) => {
              return {
                ...table,
                relationships: table.relationships.map((relationship) => {
                  if (relationship.id !== relationshipId) return relationship;

                  return {
                    ...relationship,
                    extra,
                  };
                }),
              };
            }),
          };
        }),
      };
    });
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
