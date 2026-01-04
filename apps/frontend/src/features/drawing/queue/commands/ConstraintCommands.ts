import type {
  Database,
  Constraint,
  ConstraintColumn,
} from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { ErdStore } from '@/store/erd.store';
import {
  createConstraintAPI,
  updateConstraintNameAPI,
  addColumnToConstraintAPI,
  removeColumnFromConstraintAPI,
  deleteConstraintAPI,
} from '../../api/constraint.api';

export class CreateConstraintCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private constraintData: Constraint,
  ) {
    super('CREATE_CONSTRAINT', 'constraint', constraintData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.createConstraint(
      this.schemaId,
      this.tableId,
      this.constraintData,
    );
  }

  async executeAPI(db: Database) {
    const response = await createConstraintAPI({
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraint: {
        id: this.constraintData.id,
        tableId: this.tableId,
        name: this.constraintData.name,
        kind: this.constraintData.kind,
        columns: this.constraintData.columns.map((col) => ({
          id: col.id,
          constraintId: this.constraintData.id,
          columnId: col.columnId,
          seqNo: col.seqNo,
        })),
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedConstraintId =
      mapping[this.constraintData.id] || this.constraintData.id;

    return new CreateConstraintCommand(mappedSchemaId, mappedTableId, {
      ...this.constraintData,
      id: mappedConstraintId,
      tableId: mappedTableId,
      columns: this.constraintData.columns.map((col) => ({
        ...col,
        id: mapping[col.id] || col.id,
        constraintId: mappedConstraintId,
        columnId: mapping[col.columnId] || col.columnId,
      })),
    });
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintData.id,
    };
  }
}

export class UpdateConstraintNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private constraintId: string,
    private newName: string,
  ) {
    super('UPDATE_CONSTRAINT_NAME', 'constraint', constraintId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeConstraintName(
      this.schemaId,
      this.tableId,
      this.constraintId,
      this.newName,
    );
  }

  async executeAPI(db: Database) {
    await updateConstraintNameAPI(this.constraintId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedConstraintId = mapping[this.constraintId] || this.constraintId;

    return new UpdateConstraintNameCommand(
      mappedSchemaId,
      mappedTableId,
      mappedConstraintId,
      this.newName,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
    };
  }
}

export class AddColumnToConstraintCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private constraintId: string,
    private constraintColumnData: ConstraintColumn,
  ) {
    super(
      'ADD_COLUMN_TO_CONSTRAINT',
      'constraintColumn',
      constraintColumnData.id,
    );
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.addColumnToConstraint(
      this.schemaId,
      this.tableId,
      this.constraintId,
      this.constraintColumnData,
    );
  }

  async executeAPI(db: Database) {
    const response = await addColumnToConstraintAPI(this.constraintId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
      constraintColumn: {
        id: this.constraintColumnData.id,
        constraintId: this.constraintId,
        columnId: this.constraintColumnData.columnId,
        seqNo: this.constraintColumnData.seqNo,
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedConstraintId = mapping[this.constraintId] || this.constraintId;
    const mappedConstraintColumnId =
      mapping[this.constraintColumnData.id] || this.constraintColumnData.id;
    const mappedColumnId =
      mapping[this.constraintColumnData.columnId] ||
      this.constraintColumnData.columnId;

    return new AddColumnToConstraintCommand(
      mappedSchemaId,
      mappedTableId,
      mappedConstraintId,
      {
        ...this.constraintColumnData,
        id: mappedConstraintColumnId,
        constraintId: mappedConstraintId,
        columnId: mappedColumnId,
      },
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
    };
  }
}

export class RemoveColumnFromConstraintCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private constraintId: string,
    private constraintColumnId: string,
  ) {
    super(
      'REMOVE_COLUMN_FROM_CONSTRAINT',
      'constraintColumn',
      constraintColumnId,
    );
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.removeColumnFromConstraint(
      this.schemaId,
      this.tableId,
      this.constraintId,
      this.constraintColumnId,
    );
  }

  async executeAPI(db: Database) {
    await removeColumnFromConstraintAPI(
      this.constraintId,
      this.constraintColumnId,
      {
        database: db,
        schemaId: this.schemaId,
        tableId: this.tableId,
        constraintId: this.constraintId,
        constraintColumnId: this.constraintColumnId,
      },
    );
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedConstraintId = mapping[this.constraintId] || this.constraintId;
    const mappedConstraintColumnId =
      mapping[this.constraintColumnId] || this.constraintColumnId;

    return new RemoveColumnFromConstraintCommand(
      mappedSchemaId,
      mappedTableId,
      mappedConstraintId,
      mappedConstraintColumnId,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
    };
  }
}

export class DeleteConstraintCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private constraintId: string,
  ) {
    super('DELETE_CONSTRAINT', 'constraint', constraintId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.deleteConstraint(
      this.schemaId,
      this.tableId,
      this.constraintId,
    );
  }

  async executeAPI(db: Database) {
    await deleteConstraintAPI(this.constraintId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedConstraintId = mapping[this.constraintId] || this.constraintId;

    return new DeleteConstraintCommand(
      mappedSchemaId,
      mappedTableId,
      mappedConstraintId,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      constraintId: this.constraintId,
    };
  }
}
