import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType, ConstraintKind } from '../types';
import type { Constraint, ConstraintColumn } from '@schemafy/validator';
import * as columnService from '../services/column.service';
import { toast } from 'sonner';

export const getColumnName = (
  columns: Array<{ id: string; name: string }>,
  columnId: string,
): string => {
  return columns.find((col) => col.id === columnId)?.name || 'Unknown';
};

export const saveColumnName = async (
  _erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  name: string,
) => {
  try {
    await columnService.updateColumnName(schemaId, tableId, columnId, name);
  } catch (error) {
    toast.error('Failed to update column name');
    throw error;
  }
};

export const saveColumnType = async (
  _erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  dataType: string,
) => {
  try {
    await columnService.updateColumnType(schemaId, tableId, columnId, dataType);
  } catch (error) {
    toast.error('Failed to update column type');
    throw error;
  }
};

const CONSTRAINT_PREFIX_MAP: Record<ConstraintKind, string> = {
  PRIMARY_KEY: 'pk',
  NOT_NULL: 'nn',
  UNIQUE: 'uq',
};

const createConstraintColumn = (columnId: string, seqNo: number) => ({
  id: ulid(),
  columnId,
  seqNo,
  constraintId: '',
  isAffected: false,
});

const addPrimaryKeyConstraint = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  table: { name: string; constraints: Constraint[] },
) => {
  const existingPk = table.constraints.find((c) => c.kind === 'PRIMARY_KEY');

  if (existingPk) {
    erdStore.addColumnToConstraint(
      schemaId,
      tableId,
      existingPk.id,
      createConstraintColumn(columnId, existingPk.columns.length),
    );
  } else {
    erdStore.createConstraint(schemaId, tableId, {
      id: ulid(),
      name: `pk_${table.name}`,
      kind: 'PRIMARY_KEY',
      isAffected: false,
      columns: [createConstraintColumn(columnId, 0)],
    });
  }
};

const addSingleColumnConstraint = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  constraintKind: ConstraintKind,
) => {
  const prefix = CONSTRAINT_PREFIX_MAP[constraintKind];
  erdStore.createConstraint(schemaId, tableId, {
    id: ulid(),
    name: `${prefix}_${columnId}`,
    kind: constraintKind,
    isAffected: false,
    columns: [createConstraintColumn(columnId, 0)],
  });
};

const removePrimaryKeyConstraint = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  existingConstraint: Constraint,
  columnId: string,
) => {
  const pkColumn = existingConstraint.columns.find(
    (cc: ConstraintColumn) => cc.columnId === columnId,
  );
  if (pkColumn) {
    erdStore.removeColumnFromConstraint(
      schemaId,
      tableId,
      existingConstraint.id,
      pkColumn.id,
    );
  }
};

const saveColumnConstraint = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  constraintKind: ConstraintKind,
  enabled: boolean,
) => {
  if (erdStore.erdState.state !== 'loaded') {
    throw new Error('ERD state not loaded');
  }

  const table = erdStore.erdState.database.schemas
    .find((s) => s.id === schemaId)
    ?.tables.find((t) => t.id === tableId);

  if (!table) {
    throw new Error('Table not found');
  }

  const existingConstraint = table.constraints.find(
    (c) =>
      c.kind === constraintKind &&
      c.columns.some((cc) => cc.columnId === columnId),
  );

  if (enabled && !existingConstraint) {
    if (constraintKind === 'PRIMARY_KEY') {
      addPrimaryKeyConstraint(erdStore, schemaId, tableId, columnId, table);
    } else {
      addSingleColumnConstraint(
        erdStore,
        schemaId,
        tableId,
        columnId,
        constraintKind,
      );
    }
  } else if (!enabled && existingConstraint) {
    if (constraintKind === 'PRIMARY_KEY') {
      removePrimaryKeyConstraint(
        erdStore,
        schemaId,
        tableId,
        existingConstraint,
        columnId,
      );
    } else {
      erdStore.deleteConstraint(schemaId, tableId, existingConstraint.id);
    }
  }
};

export const saveColumnPrimaryKey = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isPrimaryKey: boolean,
) => {
  saveColumnConstraint(
    erdStore,
    schemaId,
    tableId,
    columnId,
    'PRIMARY_KEY',
    isPrimaryKey,
  );
};

export const saveColumnNotNull = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isNotNull: boolean,
) => {
  saveColumnConstraint(
    erdStore,
    schemaId,
    tableId,
    columnId,
    'NOT_NULL',
    isNotNull,
  );
};

type ColumnFieldSaver = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  value: string | boolean,
) => void;

const COLUMN_FIELD_SAVERS: Partial<Record<keyof ColumnType, ColumnFieldSaver>> =
  {
    name: (erdStore, schemaId, tableId, columnId, value) =>
      saveColumnName(erdStore, schemaId, tableId, columnId, value as string),
    type: (erdStore, schemaId, tableId, columnId, value) =>
      saveColumnType(erdStore, schemaId, tableId, columnId, value as string),
    isPrimaryKey: (erdStore, schemaId, tableId, columnId, value) =>
      saveColumnPrimaryKey(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as boolean,
      ),
    isNotNull: (erdStore, schemaId, tableId, columnId, value) =>
      saveColumnNotNull(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as boolean,
      ),
  };

export const saveColumnField = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  key: keyof ColumnType,
  value: string | boolean,
) => {
  const saver = COLUMN_FIELD_SAVERS[key];
  if (saver) {
    saver(erdStore, schemaId, tableId, columnId, value);
  }
};
