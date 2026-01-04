import type { Database, Table } from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { SyncContext } from '../../types';
import type { ErdStore } from '@/store/erd.store';
import {
  createTableAPI,
  updateTableNameAPI,
  updateTableExtraAPI,
  deleteTableAPI,
} from '../../api/table.api';

export class CreateTableCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableData: Omit<Table, 'schemaId'>,
  ) {
    super('CREATE_TABLE', 'table', tableData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.createTable(this.schemaId, this.tableData);
  }

  async executeAPI(db: Database) {
    const response = await createTableAPI(
      {
        database: db,
        schemaId: this.schemaId,
        table: {
          id: this.tableData.id,
          schemaId: this.schemaId,
          name: this.tableData.name,
          comment: this.tableData.comment ?? '',
          tableOptions: this.tableData.tableOptions ?? '',
        },
      },
      this.tableData.extra ? JSON.stringify(this.tableData.extra) : undefined,
    );

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping): CreateTableCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableData.id] || this.tableData.id;

    return new CreateTableCommand(mappedSchemaId, {
      ...this.tableData,
      id: mappedTableId,
    });
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableData.id,
    };
  }
}

export class UpdateTableNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private newName: string,
  ) {
    super('UPDATE_TABLE_NAME', 'table', tableId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.changeTableName(this.schemaId, this.tableId, this.newName);
  }

  async executeAPI(db: Database) {
    await updateTableNameAPI(this.tableId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): UpdateTableNameCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;

    return new UpdateTableNameCommand(
      mappedSchemaId,
      mappedTableId,
      this.newName,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
    };
  }
}

export class UpdateTableExtraCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private newExtra: unknown,
  ) {
    super('UPDATE_TABLE_EXTRA', 'table', tableId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.updateTableExtra(this.schemaId, this.tableId, this.newExtra);
  }

  async executeAPI() {
    await updateTableExtraAPI(this.tableId, {
      extra: JSON.stringify(this.newExtra),
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): UpdateTableExtraCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;

    return new UpdateTableExtraCommand(
      mappedSchemaId,
      mappedTableId,
      this.newExtra,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
    };
  }
}

export class DeleteTableCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private tableSnapshot: Omit<Table, 'schemaId'>,
  ) {
    super('DELETE_TABLE', 'table', tableId);
  }

  applyToSyncedStore(syncedStore: ErdStore): void {
    syncedStore.deleteTable(this.schemaId, this.tableId);
  }

  async executeAPI(db: Database) {
    await deleteTableAPI(this.tableId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping): DeleteTableCommand {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;

    return new DeleteTableCommand(
      mappedSchemaId,
      mappedTableId,
      this.tableSnapshot,
    );
  }

  getContext(): SyncContext {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
    };
  }
}
