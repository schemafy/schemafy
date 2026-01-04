import { ulid } from 'ulid';
import type { Column } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import { getColumnAPI } from '../api/column.api';
import {
  validateAndGetTable,
  validateAndGetColumn,
} from '../utils/entityValidators';
import {
  CreateColumnCommand,
  UpdateColumnNameCommand,
  UpdateColumnTypeCommand,
  UpdateColumnPositionCommand,
  DeleteColumnCommand,
} from '../queue/commands/ColumnCommands';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';

const getErdStore = () => ErdStore.getInstance();

export async function getColumn(columnId: string) {
  const response = await getColumnAPI(columnId);
  return response.result;
}

export function createColumn(
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
  validateAndGetTable(schemaId, tableId);

  const erdStore = getErdStore();
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

  const command = new CreateColumnCommand(schemaId, tableId, newColumn);

  executeCommandWithValidation(command, () => {
    erdStore.createColumn(schemaId, tableId, newColumn);
  });

  return columnId;
}

export function updateColumnName(
  schemaId: string,
  tableId: string,
  columnId: string,
  newName: string,
) {
  validateAndGetColumn(schemaId, tableId, columnId);

  const erdStore = getErdStore();
  const command = new UpdateColumnNameCommand(
    schemaId,
    tableId,
    columnId,
    newName,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeColumnName(schemaId, tableId, columnId, newName);
  });
}

export function updateColumnType(
  schemaId: string,
  tableId: string,
  columnId: string,
  dataType: string,
  lengthScale?: string,
) {
  validateAndGetColumn(schemaId, tableId, columnId);

  const erdStore = getErdStore();
  const command = new UpdateColumnTypeCommand(
    schemaId,
    tableId,
    columnId,
    dataType,
    lengthScale,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeColumnType(
      schemaId,
      tableId,
      columnId,
      dataType,
      lengthScale ?? '',
    );
  });
}

export function updateColumnPosition(
  schemaId: string,
  tableId: string,
  columnId: string,
  newPosition: number,
) {
  validateAndGetColumn(schemaId, tableId, columnId);

  const erdStore = getErdStore();
  const command = new UpdateColumnPositionCommand(
    schemaId,
    tableId,
    columnId,
    newPosition,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeColumnPosition(schemaId, tableId, columnId, newPosition);
  });
}

export function deleteColumn(
  schemaId: string,
  tableId: string,
  columnId: string,
) {
  validateAndGetColumn(schemaId, tableId, columnId);

  const erdStore = getErdStore();
  const command = new DeleteColumnCommand(schemaId, tableId, columnId);

  executeCommandWithValidation(command, () => {
    erdStore.deleteColumn(schemaId, tableId, columnId);
  });
}
