import { ulid } from 'ulid';
import type { Index, IndexColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createIndexAPI,
  getIndexAPI,
  updateIndexNameAPI,
  updateIndexTypeAPI,
  addColumnToIndexAPI,
  updateIndexColumnSortDirAPI,
  removeColumnFromIndexAPI,
  deleteIndexAPI,
} from '../api/index.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';

const getErdStore = () => ErdStore.getInstance();

export async function getIndex(indexId: string) {
  const response = await getIndexAPI(indexId);
  return response.result;
}

export async function createIndex(
  schemaId: string,
  tableId: string,
  name: string,
  type: string,
  columns: Array<{
    columnId: string;
    seqNo: number;
    sortDir?: 'ASC' | 'DESC';
  }>,
  comment?: string,
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const indexId = ulid();

  const indexColumns: IndexColumn[] = columns.map((col) => ({
    id: ulid(),
    indexId,
    columnId: col.columnId,
    seqNo: col.seqNo,
    sortDir: col.sortDir ?? 'ASC',
    isAffected: false,
  }));

  const newIndex: Omit<Index, 'tableId'> = {
    id: indexId,
    name,
    type: type as Index['type'],
    comment: comment ?? null,
    columns: indexColumns,
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () => erdStore.createIndex(schemaId, tableId, newIndex),
    () =>
      createIndexAPI({
        database,
        schemaId,
        tableId,
        index: {
          id: indexId,
          tableId,
          name,
          type,
          comment: comment ?? '',
          columns: indexColumns.map((col) => ({
            id: col.id,
            indexId,
            columnId: col.columnId,
            seqNo: col.seqNo,
            sortDir: col.sortDir,
          })),
        },
      }),
    () => erdStore.deleteIndex(schemaId, tableId, indexId),
  );

  const realId = response.result?.indexes[tableId]?.[indexId];
  if (realId && realId !== indexId) {
    erdStore.replaceIndexId(schemaId, tableId, indexId, realId);
    return realId;
  }

  return indexId;
}

export async function updateIndexName(
  schemaId: string,
  tableId: string,
  indexId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldName = index.name;
      erdStore.changeIndexName(schemaId, tableId, indexId, newName);
      return oldName;
    },
    () =>
      updateIndexNameAPI(indexId, {
        database,
        schemaId,
        tableId,
        indexId,
        newName,
      }),
    (oldName) => erdStore.changeIndexName(schemaId, tableId, indexId, oldName),
  );
}

export async function updateIndexType(
  schemaId: string,
  tableId: string,
  indexId: string,
  newType: Index['type'],
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldType = index.type;
      erdStore.changeIndexType(schemaId, tableId, indexId, newType);
      return oldType;
    },
    () =>
      updateIndexTypeAPI(indexId, {
        type: newType,
      }),
    (oldType) => erdStore.changeIndexType(schemaId, tableId, indexId, oldType),
  );
}

export async function addColumnToIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
  columnId: string,
  seqNo: number,
  sortDir?: 'ASC' | 'DESC',
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  const indexColumnId = ulid();

  const newIndexColumn: IndexColumn = {
    id: indexColumnId,
    indexId,
    columnId,
    seqNo,
    sortDir: sortDir ?? 'ASC',
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () => erdStore.addColumnToIndex(schemaId, tableId, indexId, newIndexColumn),
    () =>
      addColumnToIndexAPI(indexId, {
        database,
        schemaId,
        tableId,
        indexId,
        indexColumn: {
          id: indexColumnId,
          indexId,
          columnId,
          seqNo,
          sortDir: sortDir ?? 'ASC',
        },
      }),
    () =>
      erdStore.removeColumnFromIndex(schemaId, tableId, indexId, indexColumnId),
  );

  const realId = response.result?.id;

  if (realId && realId !== indexColumnId) {
    erdStore.replaceIndexColumnId(
      schemaId,
      tableId,
      indexId,
      indexColumnId,
      realId,
    );
    return realId;
  }

  return indexColumnId;
}

export async function updateIndexColumnSortDir(
  schemaId: string,
  tableId: string,
  indexId: string,
  indexColumnId: string,
  newSortDir: 'ASC' | 'DESC',
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  const indexColumn = index.columns.find((c) => c.id === indexColumnId);
  if (!indexColumn) {
    throw new Error(`Index column ${indexColumnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldSortDir = indexColumn.sortDir;
      erdStore.changeIndexColumnSortDir(
        schemaId,
        tableId,
        indexId,
        indexColumnId,
        newSortDir,
      );
      return oldSortDir;
    },
    () =>
      updateIndexColumnSortDirAPI(indexId, indexColumnId, {
        sortDir: newSortDir,
      }),
    (oldSortDir) =>
      erdStore.changeIndexColumnSortDir(
        schemaId,
        tableId,
        indexId,
        indexColumnId,
        oldSortDir,
      ),
  );
}

export async function removeColumnFromIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
  indexColumnId: string,
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  const indexColumn = index.columns.find((c) => c.id === indexColumnId);

  if (!indexColumn) {
    throw new Error(`Index column ${indexColumnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const columnSnapshot = structuredClone(indexColumn);
      erdStore.removeColumnFromIndex(schemaId, tableId, indexId, indexColumnId);
      return columnSnapshot;
    },
    () =>
      removeColumnFromIndexAPI(indexId, indexColumnId, {
        database,
        schemaId,
        tableId,
        indexId,
        indexColumnId,
      }),
    (columnSnapshot) =>
      erdStore.addColumnToIndex(schemaId, tableId, indexId, columnSnapshot),
  );
}

export async function deleteIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
) {
  const erdStore = getErdStore();
  const database = erdStore.database;

  if (!database) {
    throw new Error('Database not loaded');
  }

  const schema = database.schemas.find((s) => s.id === schemaId);
  if (!schema) {
    throw new Error(`Schema ${schemaId} not found`);
  }

  const table = schema.tables.find((t) => t.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not found`);
  }

  const index = table.indexes.find((i) => i.id === indexId);
  if (!index) {
    throw new Error(`Index ${indexId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const indexSnapshot = structuredClone(index);
      erdStore.deleteIndex(schemaId, tableId, indexId);
      return indexSnapshot;
    },
    () =>
      deleteIndexAPI(indexId, {
        database,
        schemaId,
        tableId,
        indexId,
      }),
    (indexSnapshot) => erdStore.createIndex(schemaId, tableId, indexSnapshot),
  );
}
