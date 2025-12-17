import { ulid } from 'ulid';
import type { Constraint, ConstraintColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createConstraintAPI,
  getConstraintAPI,
  updateConstraintNameAPI,
  addColumnToConstraintAPI,
  removeColumnFromConstraintAPI,
  deleteConstraintAPI,
} from '../api/constraint.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';

const getErdStore = () => ErdStore.getInstance();

export async function getConstraint(constraintId: string) {
  const response = await getConstraintAPI(constraintId);
  return response.result;
}

export async function createConstraint(
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

  const response = await withOptimisticUpdate(
    () => erdStore.createConstraint(schemaId, tableId, newConstraint),
    () =>
      createConstraintAPI({
        database,
        schemaId,
        tableId,
        constraint: {
          id: constraintId,
          tableId,
          name,
          columns: constraintColumns.map((col) => ({
            id: col.id,
            constraintId,
            columnId: col.columnId,
            seqNo: col.seqNo,
          })),
        },
      }),
    () => erdStore.deleteConstraint(schemaId, tableId, constraintId),
  );

  const realId = response.result?.constraints[tableId]?.[constraintId];
  if (realId && realId !== constraintId) {
    erdStore.replaceConstraintId(schemaId, tableId, constraintId, realId);
    return realId;
  }

  return constraintId;
}

export async function updateConstraintName(
  schemaId: string,
  tableId: string,
  constraintId: string,
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

  const constraint = table.constraints.find((c) => c.id === constraintId);
  if (!constraint) {
    throw new Error(`Constraint ${constraintId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const oldName = constraint.name;
      erdStore.changeConstraintName(schemaId, tableId, constraintId, newName);
      return oldName;
    },
    () =>
      updateConstraintNameAPI(constraintId, {
        database,
        schemaId,
        tableId,
        constraintId,
        newName,
      }),
    (oldName) =>
      erdStore.changeConstraintName(schemaId, tableId, constraintId, oldName),
  );
}

export async function addColumnToConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
  columnId: string,
  seqNo: number,
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

  const constraint = table.constraints.find((c) => c.id === constraintId);
  if (!constraint) {
    throw new Error(`Constraint ${constraintId} not found`);
  }

  const constraintColumnId = ulid();

  const newConstraintColumn: ConstraintColumn = {
    id: constraintColumnId,
    constraintId,
    columnId,
    seqNo,
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () =>
      erdStore.addColumnToConstraint(
        schemaId,
        tableId,
        constraintId,
        newConstraintColumn,
      ),
    () =>
      addColumnToConstraintAPI(constraintId, {
        database,
        schemaId,
        tableId,
        constraintId,
        constraintColumn: {
          id: constraintColumnId,
          constraintId,
          columnId,
          seqNo,
        },
      }),
    () =>
      erdStore.removeColumnFromConstraint(
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      ),
  );

  const realId =
    response.result?.constraintColumns?.[constraintId]?.[constraintColumnId];

  if (realId && realId !== constraintColumnId) {
    erdStore.replaceConstraintColumnId(
      schemaId,
      tableId,
      constraintId,
      constraintColumnId,
      realId,
    );
    return realId;
  }

  return constraintColumnId;
}

export async function removeColumnFromConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
  constraintColumnId: string,
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

  const constraint = table.constraints.find((c) => c.id === constraintId);
  if (!constraint) {
    throw new Error(`Constraint ${constraintId} not found`);
  }

  const constraintColumn = constraint.columns.find(
    (c) => c.id === constraintColumnId,
  );

  if (!constraintColumn) {
    throw new Error(`Constraint column ${constraintColumnId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const columnSnapshot = structuredClone(constraintColumn);
      erdStore.removeColumnFromConstraint(
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      );
      return columnSnapshot;
    },
    () =>
      removeColumnFromConstraintAPI(constraintId, constraintColumnId, {
        database,
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      }),
    (columnSnapshot) =>
      erdStore.addColumnToConstraint(
        schemaId,
        tableId,
        constraintId,
        columnSnapshot,
      ),
  );
}

export async function deleteConstraint(
  schemaId: string,
  tableId: string,
  constraintId: string,
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

  const constraint = table.constraints.find((c) => c.id === constraintId);
  if (!constraint) {
    throw new Error(`Constraint ${constraintId} not found`);
  }

  await withOptimisticUpdate(
    () => {
      const constraintSnapshot = structuredClone(constraint);
      erdStore.deleteConstraint(schemaId, tableId, constraintId);
      return constraintSnapshot;
    },
    () =>
      deleteConstraintAPI(constraintId, {
        database,
        schemaId,
        tableId,
        constraintId,
      }),
    (constraintSnapshot) =>
      erdStore.createConstraint(schemaId, tableId, constraintSnapshot),
  );
}
