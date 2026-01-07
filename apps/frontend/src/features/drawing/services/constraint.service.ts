import { ulid } from 'ulid';
import type { Constraint, ConstraintColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import { getConstraintAPI } from '../api/constraint.api';
import {
  validateAndGetTable,
  validateAndGetConstraint,
} from '../utils/entityValidators';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';
import {
  CreateConstraintCommand,
  UpdateConstraintNameCommand,
  AddColumnToConstraintCommand,
  RemoveColumnFromConstraintCommand,
  DeleteConstraintCommand,
} from '../queue/commands/ConstraintCommands';

const getErdStore = () => ErdStore.getInstance();

export async function getConstraint(constraintId: string) {
  const response = await getConstraintAPI(constraintId);
  return response.result;
}

export function createConstraint(
  schemaId: string,
  tableId: string,
  name: string,
  kind: string,
  columns: Array<{
    columnId: string;
    seqNo: number;
  }>,
  checkExpr?: string,
  defaultExpr?: string,
) {
  const erdStore = getErdStore();
  validateAndGetTable(schemaId, tableId);

  const constraintId = ulid();

  const constraintColumns: ConstraintColumn[] = columns.map((col) => ({
    id: ulid(),
    constraintId,
    columnId: col.columnId,
    seqNo: col.seqNo,
    isAffected: false,
  }));

  const newConstraint: Constraint = {
    id: constraintId,
    tableId,
    name,
    kind: kind as Constraint['kind'],
    checkExpr: checkExpr ?? null,
    defaultExpr: defaultExpr ?? null,
    columns: constraintColumns,
    isAffected: false,
  };

  const command = new CreateConstraintCommand(schemaId, tableId, newConstraint);

  executeCommandWithValidation(command, () => {
    erdStore.createConstraint(schemaId, tableId, newConstraint);
  });

  return constraintId;
}

export function updateConstraintName(
  schemaId: string,
  tableId: string,
  constraintId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  validateAndGetConstraint(schemaId, tableId, constraintId);

  const command = new UpdateConstraintNameCommand(
    schemaId,
    tableId,
    constraintId,
    newName,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeConstraintName(schemaId, tableId, constraintId, newName);
  });
}

export function addColumnToConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
  columnId: string,
  seqNo: number,
) {
  const erdStore = getErdStore();
  validateAndGetConstraint(schemaId, tableId, constraintId);

  const constraintColumnId = ulid();

  const newConstraintColumn: ConstraintColumn = {
    id: constraintColumnId,
    constraintId,
    columnId,
    seqNo,
    isAffected: false,
  };

  const command = new AddColumnToConstraintCommand(
    schemaId,
    tableId,
    constraintId,
    newConstraintColumn,
  );

  executeCommandWithValidation(command, () => {
    erdStore.addColumnToConstraint(
      schemaId,
      tableId,
      constraintId,
      newConstraintColumn,
    );
  });

  return constraintColumnId;
}

export function removeColumnFromConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
  constraintColumnId: string,
) {
  const erdStore = getErdStore();
  const { constraint } = validateAndGetConstraint(
    schemaId,
    tableId,
    constraintId,
  );

  const constraintColumn = constraint.columns.find(
    (c) => c.id === constraintColumnId,
  );

  if (!constraintColumn) {
    throw new Error(`Constraint column ${constraintColumnId} not found`);
  }

  if (constraint.columns.length === 1) {
    return deleteConstraint(schemaId, tableId, constraintId);
  }

  const command = new RemoveColumnFromConstraintCommand(
    schemaId,
    tableId,
    constraintId,
    constraintColumnId,
  );

  executeCommandWithValidation(command, () => {
    erdStore.removeColumnFromConstraint(
      schemaId,
      tableId,
      constraintId,
      constraintColumnId,
    );
  });
}

export function deleteConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
) {
  const erdStore = getErdStore();
  validateAndGetConstraint(schemaId, tableId, constraintId);

  const command = new DeleteConstraintCommand(schemaId, tableId, constraintId);

  executeCommandWithValidation(command, () => {
    erdStore.deleteConstraint(schemaId, tableId, constraintId);
  });
}
