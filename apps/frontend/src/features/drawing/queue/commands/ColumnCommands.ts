import type { Database, Column } from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { SyncContext } from '../../types';
import type { ErdStore } from '@/store/erd.store';
import {
  createColumnAPI,
  updateColumnNameAPI,
  updateColumnTypeAPI,
  updateColumnPositionAPI,
  deleteColumnAPI,
} from '../../api/column.api';

export class CreateColumnCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private columnData: Column,
  ) {
    super('CREATE_COLUMN', 'column', columnData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.createColumn(this.schemaId, this.tableId, this.columnData);
  }

  async executeAPI(db: Database) {
    const response = await createColumnAPI({
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      column: {
        id: this.columnData.id,
        tableId: this.tableId,
        name: this.columnData.name,
        seqNo: this.columnData.seqNo,
        dataType: this.columnData.dataType,
        lengthScale: this.columnData.lengthScale ?? '',
        charset: this.columnData.charset ?? '',
        collation: this.columnData.collation ?? '',
        comment: this.columnData.comment ?? '',
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping): CreateColumnCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedColumnId = mapping[this.columnData.id] || this.columnData.id;

    return new CreateColumnCommand(mappedSchemaId, mappedTableId, {
      ...this.columnData,
      id: mappedColumnId,
      tableId: mappedTableId,
    });
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnData.id,
    };
  }
}

export class UpdateColumnNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private columnId: string,
    private newName: string,
  ) {
    super('UPDATE_COLUMN_NAME', 'column', columnId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.changeColumnName(
      this.schemaId,
      this.tableId,
      this.columnId,
      this.newName,
    );
  }

  async executeAPI(db: Database) {
    await updateColumnNameAPI(this.columnId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): UpdateColumnNameCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedColumnId = mapping[this.columnId] || this.columnId;

    return new UpdateColumnNameCommand(
      mappedSchemaId,
      mappedTableId,
      mappedColumnId,
      this.newName,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
    };
  }
}

export class UpdateColumnTypeCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private columnId: string,
    private dataType: string,
    private lengthScale?: string,
  ) {
    super('UPDATE_COLUMN_TYPE', 'column', columnId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.changeColumnType(
      this.schemaId,
      this.tableId,
      this.columnId,
      this.dataType,
      this.lengthScale ?? '',
    );
  }

  async executeAPI(db: Database) {
    await updateColumnTypeAPI(this.columnId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
      dataType: this.dataType,
      lengthScale: this.lengthScale,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): UpdateColumnTypeCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedColumnId = mapping[this.columnId] || this.columnId;

    return new UpdateColumnTypeCommand(
      mappedSchemaId,
      mappedTableId,
      mappedColumnId,
      this.dataType,
      this.lengthScale,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
    };
  }
}

export class UpdateColumnPositionCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private columnId: string,
    private newPosition: number,
  ) {
    super('UPDATE_COLUMN_POSITION', 'column', columnId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.changeColumnPosition(
      this.schemaId,
      this.tableId,
      this.columnId,
      this.newPosition,
    );
  }

  async executeAPI(db: Database) {
    await updateColumnPositionAPI(this.columnId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
      newPosition: this.newPosition,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): UpdateColumnPositionCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedColumnId = mapping[this.columnId] || this.columnId;

    return new UpdateColumnPositionCommand(
      mappedSchemaId,
      mappedTableId,
      mappedColumnId,
      this.newPosition,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
    };
  }
}

export class DeleteColumnCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private columnId: string,
    private columnSnapshot: Column,
  ) {
    super('DELETE_COLUMN', 'column', columnId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.deleteColumn(this.schemaId, this.tableId, this.columnId);
  }

  async executeAPI(db: Database) {
    await deleteColumnAPI(this.columnId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): DeleteColumnCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedColumnId = mapping[this.columnId] || this.columnId;

    return new DeleteColumnCommand(
      mappedSchemaId,
      mappedTableId,
      mappedColumnId,
      this.columnSnapshot,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      columnId: this.columnId,
    };
  }
}
