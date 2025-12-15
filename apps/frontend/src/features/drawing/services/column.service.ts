import { ulid } from 'ulid';
import type { Column } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createColumnAPI,
  getColumnAPI,
  updateColumnNameAPI,
  updateColumnTypeAPI,
  updateColumnPositionAPI,
  deleteColumnAPI,
} from '../api/column.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';

const getErdStore = () => ErdStore.getInstance();

export async function getColumn(columnId: string) {
  const response = await getColumnAPI(columnId);
  return response.result;
}

export async function createColumn(
  schemaId: string,
  tableId: string,
  name: string,
  ordinalPosition: number,
  dataType: string,
  lengthScale?: string,
  isAutoIncrement?: boolean,
  charset?: string,
  collation?: string,
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

  const columnId = ulid();

  const newColumn: Column = {
    id: columnId,
    tableId,
    name,
    ordinalPosition,
    dataType,
    lengthScale: lengthScale ?? '',
    isAutoIncrement: isAutoIncrement ?? false,
    charset: charset ?? '',
    collation: collation ?? '',
    comment: comment ?? null,
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () => erdStore.createColumn(schemaId, tableId, newColumn),
    () =>
      createColumnAPI({
        database,
        schemaId,
        tableId,
        column: {
          id: columnId,
          tableId,
          name,
          ordinalPosition,
          dataType,
          lengthScale: lengthScale ?? '',
          charset: charset ?? '',
          collation: collation ?? '',
          comment: comment ?? '',
        },
      }),
    () => erdStore.deleteColumn(schemaId, tableId, columnId),
  );

  const realId = response.result?.columns[tableId]?.[columnId];
  if (realId && realId !== columnId) {
    erdStore.replaceColumnId(schemaId, tableId, columnId, realId);
    return realId;
  }

  return columnId;
}

export async function updateColumnName(
  schemaId: string,
  tableId: string,
  columnId: string,
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

  const column = table.columns.find((c) => c.id === columnId);
  if (!column) {
    throw new Error(`Column ${columnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldName = column.name;
      erdStore.changeColumnName(schemaId, tableId, columnId, newName);
      return oldName;
    },
    () =>
      updateColumnNameAPI(columnId, {
        database,
        schemaId,
        tableId,
        columnId,
        newName,
      }),
    (oldName) => erdStore.changeColumnName(schemaId, tableId, columnId, oldName),
  );
}

export async function updateColumnType(
  schemaId: string,
  tableId: string,
  columnId: string,
  dataType: string,
  lengthScale?: string,
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

  const column = table.columns.find((c) => c.id === columnId);
  if (!column) {
    throw new Error(`Column ${columnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldDataType = column.dataType;
      const oldLengthScale = column.lengthScale;
      erdStore.changeColumnType(
        schemaId,
        tableId,
        columnId,
        dataType,
        lengthScale ?? '',
      );
      return { oldDataType, oldLengthScale };
    },
    () =>
      updateColumnTypeAPI(columnId, {
        database,
        schemaId,
        tableId,
        columnId,
        dataType,
        lengthScale,
      }),
    ({ oldDataType, oldLengthScale }) =>
      erdStore.changeColumnType(
        schemaId,
        tableId,
        columnId,
        oldDataType,
        oldLengthScale,
      ),
  );
}

export async function updateColumnPosition(
  schemaId: string,
  tableId: string,
  columnId: string,
  newPosition: number,
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

  const column = table.columns.find((c) => c.id === columnId);
  if (!column) {
    throw new Error(`Column ${columnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldPosition = column.ordinalPosition;
      erdStore.changeColumnPosition(schemaId, tableId, columnId, newPosition);
      return oldPosition;
    },
    () =>
      updateColumnPositionAPI(columnId, {
        database,
        schemaId,
        tableId,
        columnId,
        newPosition,
      }),
    (oldPosition) =>
      erdStore.changeColumnPosition(schemaId, tableId, columnId, oldPosition),
  );
}

export async function deleteColumn(
  schemaId: string,
  tableId: string,
  columnId: string,
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

  const column = table.columns.find((c) => c.id === columnId);
  if (!column) {
    throw new Error(`Column ${columnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const columnSnapshot = structuredClone(column);
      erdStore.deleteColumn(schemaId, tableId, columnId);
      return columnSnapshot;
    },
    () =>
      deleteColumnAPI(columnId, {
        database,
        schemaId,
        tableId,
        columnId,
      }),
    (columnSnapshot) =>
      erdStore.createColumn(schemaId, tableId, columnSnapshot),
  );
}
