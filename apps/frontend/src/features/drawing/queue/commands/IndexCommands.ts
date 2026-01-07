import type { Database, Index, IndexColumn } from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { ErdStore } from '@/store/erd.store';
import {
  createIndexAPI,
  updateIndexNameAPI,
  updateIndexTypeAPI,
  addColumnToIndexAPI,
  updateIndexColumnSortDirAPI,
  removeColumnFromIndexAPI,
  deleteIndexAPI,
} from '../../api/index.api';

export class CreateIndexCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexData: Omit<Index, 'tableId'>,
  ) {
    super('CREATE_INDEX', 'index', indexData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.createIndex(this.schemaId, this.tableId, this.indexData);
  }

  async executeAPI(db: Database) {
    const response = await createIndexAPI({
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      index: {
        id: this.indexData.id,
        tableId: this.tableId,
        name: this.indexData.name,
        type: this.indexData.type,
        comment: this.indexData.comment ?? '',
        columns: this.indexData.columns.map((col) => ({
          id: col.id,
          indexId: this.indexData.id,
          columnId: col.columnId,
          seqNo: col.seqNo,
          sortDir: col.sortDir,
        })),
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexData.id] || this.indexData.id;

    return new CreateIndexCommand(mappedSchemaId, mappedTableId, {
      ...this.indexData,
      id: mappedIndexId,
      columns: this.indexData.columns.map((col) => ({
        ...col,
        id: mapping[col.id] || col.id,
        indexId: mappedIndexId,
        columnId: mapping[col.columnId] || col.columnId,
      })),
    });
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexData.id,
    };
  }
}

export class UpdateIndexNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
    private newName: string,
  ) {
    super('UPDATE_INDEX_NAME', 'index', indexId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeIndexName(
      this.schemaId,
      this.tableId,
      this.indexId,
      this.newName,
    );
  }

  async executeAPI(db: Database) {
    await updateIndexNameAPI(this.indexId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;

    return new UpdateIndexNameCommand(
      mappedSchemaId,
      mappedTableId,
      mappedIndexId,
      this.newName,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}

export class UpdateIndexTypeCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
    private newType: Index['type'],
  ) {
    super('UPDATE_INDEX_TYPE', 'index', indexId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeIndexType(
      this.schemaId,
      this.tableId,
      this.indexId,
      this.newType,
    );
  }

  async executeAPI() {
    await updateIndexTypeAPI(this.indexId, {
      type: this.newType,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;

    return new UpdateIndexTypeCommand(
      mappedSchemaId,
      mappedTableId,
      mappedIndexId,
      this.newType,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}

export class AddColumnToIndexCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
    private indexColumnData: IndexColumn,
  ) {
    super('ADD_COLUMN_TO_INDEX', 'indexColumn', indexColumnData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.addColumnToIndex(
      this.schemaId,
      this.tableId,
      this.indexId,
      this.indexColumnData,
    );
  }

  async executeAPI(db: Database) {
    const response = await addColumnToIndexAPI(this.indexId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
      indexColumn: {
        id: this.indexColumnData.id,
        indexId: this.indexId,
        columnId: this.indexColumnData.columnId,
        seqNo: this.indexColumnData.seqNo,
        sortDir: this.indexColumnData.sortDir,
      },
    });

    const result = response.result;
    if (result && 'id' in result) {
      return {
        indexColumns: {
          [this.indexId]: {
            [this.indexColumnData.id]: result.id,
          },
        },
      };
    }

    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;
    const mappedIndexColumnId =
      mapping[this.indexColumnData.id] || this.indexColumnData.id;
    const mappedColumnId =
      mapping[this.indexColumnData.columnId] || this.indexColumnData.columnId;

    return new AddColumnToIndexCommand(
      mappedSchemaId,
      mappedTableId,
      mappedIndexId,
      {
        ...this.indexColumnData,
        id: mappedIndexColumnId,
        indexId: mappedIndexId,
        columnId: mappedColumnId,
      },
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}

export class UpdateIndexColumnSortDirCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
    private indexColumnId: string,
    private newSortDir: 'ASC' | 'DESC',
  ) {
    super('UPDATE_INDEX_COLUMN_SORT_DIR', 'indexColumn', indexColumnId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeIndexColumnSortDir(
      this.schemaId,
      this.tableId,
      this.indexId,
      this.indexColumnId,
      this.newSortDir,
    );
  }

  async executeAPI() {
    await updateIndexColumnSortDirAPI(this.indexId, this.indexColumnId, {
      sortDir: this.newSortDir,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;
    const mappedIndexColumnId =
      mapping[this.indexColumnId] || this.indexColumnId;

    return new UpdateIndexColumnSortDirCommand(
      mappedSchemaId,
      mappedTableId,
      mappedIndexId,
      mappedIndexColumnId,
      this.newSortDir,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}

export class RemoveColumnFromIndexCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
    private indexColumnId: string,
  ) {
    super('REMOVE_COLUMN_FROM_INDEX', 'indexColumn', indexColumnId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.removeColumnFromIndex(
      this.schemaId,
      this.tableId,
      this.indexId,
      this.indexColumnId,
    );
  }

  async executeAPI(db: Database) {
    await removeColumnFromIndexAPI(this.indexId, this.indexColumnId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
      indexColumnId: this.indexColumnId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;
    const mappedIndexColumnId =
      mapping[this.indexColumnId] || this.indexColumnId;

    return new RemoveColumnFromIndexCommand(
      mappedSchemaId,
      mappedTableId,
      mappedIndexId,
      mappedIndexColumnId,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}

export class DeleteIndexCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private tableId: string,
    private indexId: string,
  ) {
    super('DELETE_INDEX', 'index', indexId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.deleteIndex(this.schemaId, this.tableId, this.indexId);
  }

  async executeAPI(db: Database) {
    await deleteIndexAPI(this.indexId, {
      database: db,
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedTableId = mapping[this.tableId] || this.tableId;
    const mappedIndexId = mapping[this.indexId] || this.indexId;

    return new DeleteIndexCommand(mappedSchemaId, mappedTableId, mappedIndexId);
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.tableId,
      indexId: this.indexId,
    };
  }
}
