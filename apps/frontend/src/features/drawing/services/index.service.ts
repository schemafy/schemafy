import { ulid } from 'ulid';
import type { Index, IndexColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import { getIndexAPI } from '../api/index.api';
import {
  validateAndGetTable,
  validateAndGetIndex,
} from '../utils/entityValidators';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';
import {
  CreateIndexCommand,
  UpdateIndexNameCommand,
  UpdateIndexTypeCommand,
  AddColumnToIndexCommand,
  UpdateIndexColumnSortDirCommand,
  RemoveColumnFromIndexCommand,
  DeleteIndexCommand,
} from '../queue/commands/IndexCommands';

const getErdStore = () => ErdStore.getInstance();

export async function getIndex(indexId: string) {
  const response = await getIndexAPI(indexId);
  return response.result;
}

export function createIndex(
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
  validateAndGetTable(schemaId, tableId);

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

  const command = new CreateIndexCommand(schemaId, tableId, newIndex);

  executeCommandWithValidation(command, () => {
    erdStore.createIndex(schemaId, tableId, newIndex);
  });

  return indexId;
}

export function updateIndexName(
  schemaId: string,
  tableId: string,
  indexId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  validateAndGetIndex(schemaId, tableId, indexId);

  const command = new UpdateIndexNameCommand(
    schemaId,
    tableId,
    indexId,
    newName,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeIndexName(schemaId, tableId, indexId, newName);
  });
}

export function updateIndexType(
  schemaId: string,
  tableId: string,
  indexId: string,
  newType: Index['type'],
) {
  const erdStore = getErdStore();
  validateAndGetIndex(schemaId, tableId, indexId);

  const command = new UpdateIndexTypeCommand(
    schemaId,
    tableId,
    indexId,
    newType,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeIndexType(schemaId, tableId, indexId, newType);
  });
}

export function addColumnToIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
  columnId: string,
  seqNo: number,
  sortDir?: 'ASC' | 'DESC',
) {
  const erdStore = getErdStore();
  validateAndGetIndex(schemaId, tableId, indexId);

  const indexColumnId = ulid();

  const newIndexColumn: IndexColumn = {
    id: indexColumnId,
    indexId,
    columnId,
    seqNo,
    sortDir: sortDir ?? 'ASC',
    isAffected: false,
  };

  const command = new AddColumnToIndexCommand(
    schemaId,
    tableId,
    indexId,
    newIndexColumn,
  );

  executeCommandWithValidation(command, () => {
    erdStore.addColumnToIndex(schemaId, tableId, indexId, newIndexColumn);
  });

  return indexColumnId;
}

export function updateIndexColumnSortDir(
  schemaId: string,
  tableId: string,
  indexId: string,
  indexColumnId: string,
  newSortDir: 'ASC' | 'DESC',
) {
  const erdStore = getErdStore();
  const { index } = validateAndGetIndex(schemaId, tableId, indexId);

  const indexColumn = index.columns.find((c) => c.id === indexColumnId);
  if (!indexColumn) {
    throw new Error(`Index column ${indexColumnId} not found`);
  }

  const command = new UpdateIndexColumnSortDirCommand(
    schemaId,
    tableId,
    indexId,
    indexColumnId,
    newSortDir,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeIndexColumnSortDir(
      schemaId,
      tableId,
      indexId,
      indexColumnId,
      newSortDir,
    );
  });
}

export function removeColumnFromIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
  indexColumnId: string,
) {
  const erdStore = getErdStore();
  const { index } = validateAndGetIndex(schemaId, tableId, indexId);

  const indexColumn = index.columns.find((c) => c.id === indexColumnId);

  if (!indexColumn) {
    throw new Error(`Index column ${indexColumnId} not found`);
  }

  const command = new RemoveColumnFromIndexCommand(
    schemaId,
    tableId,
    indexId,
    indexColumnId,
  );

  executeCommandWithValidation(command, () => {
    erdStore.removeColumnFromIndex(schemaId, tableId, indexId, indexColumnId);
  });
}

export function deleteIndex(
  schemaId: string,
  tableId: string,
  indexId: string,
) {
  const erdStore = getErdStore();
  validateAndGetIndex(schemaId, tableId, indexId);

  const command = new DeleteIndexCommand(schemaId, tableId, indexId);

  executeCommandWithValidation(command, () => {
    erdStore.deleteIndex(schemaId, tableId, indexId);
  });
}
