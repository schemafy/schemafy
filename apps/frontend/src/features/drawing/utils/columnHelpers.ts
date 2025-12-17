import type { ErdStore } from '@/store/erd.store';
import type { ColumnType, ConstraintKind } from '../types';
import type { Constraint, ConstraintColumn } from '@schemafy/validator';
import * as columnService from '../services/column.service';
import * as constraintService from '../services/constraint.service';
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

const addPrimaryKeyConstraint = async (
  schemaId: string,
  tableId: string,
  columnId: string,
  table: { name: string; constraints: Constraint[] },
) => {
  const existingPk = table.constraints.find((c) => c.kind === 'PRIMARY_KEY');

  if (existingPk) {
    await constraintService.addColumnToConstraint(
      schemaId,
      tableId,
      existingPk.id,
      columnId,
      existingPk.columns.length,
    );
  } else {
    await constraintService.createConstraint(
      schemaId,
      tableId,
      `pk_${table.name}`,
      'PRIMARY_KEY',
      [{ columnId, seqNo: 0 }],
    );
  }
};

const addSingleColumnConstraint = async (
  schemaId: string,
  tableId: string,
  columnId: string,
  constraintKind: ConstraintKind,
) => {
  const prefix = CONSTRAINT_PREFIX_MAP[constraintKind];
  await constraintService.createConstraint(
    schemaId,
    tableId,
    `${prefix}_${columnId}`,
    constraintKind,
    [{ columnId, seqNo: 0 }],
  );
};

const removePrimaryKeyConstraint = async (
  schemaId: string,
  tableId: string,
  existingConstraint: Constraint,
  columnId: string,
) => {
  const pkColumn = existingConstraint.columns.find(
    (cc: ConstraintColumn) => cc.columnId === columnId,
  );
  if (pkColumn) {
    await constraintService.removeColumnFromConstraint(
      schemaId,
      tableId,
      existingConstraint.id,
      pkColumn.id,
    );
  }
};

const saveColumnConstraint = async (
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

  try {
    if (enabled && !existingConstraint) {
      if (constraintKind === 'PRIMARY_KEY') {
        await addPrimaryKeyConstraint(schemaId, tableId, columnId, table);
      } else {
        await addSingleColumnConstraint(
          schemaId,
          tableId,
          columnId,
          constraintKind,
        );
      }
    } else if (!enabled && existingConstraint) {
      if (constraintKind === 'PRIMARY_KEY') {
        await removePrimaryKeyConstraint(
          schemaId,
          tableId,
          existingConstraint,
          columnId,
        );
      } else {
        await constraintService.deleteConstraint(
          schemaId,
          tableId,
          existingConstraint.id,
        );
      }
    }
  } catch (error) {
    toast.error('Failed to update constraint');
    throw error;
  }
};

export const saveColumnPrimaryKey = async (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isPrimaryKey: boolean,
) => {
  await saveColumnConstraint(
    erdStore,
    schemaId,
    tableId,
    columnId,
    'PRIMARY_KEY',
    isPrimaryKey,
  );
};

export const saveColumnNotNull = async (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isNotNull: boolean,
) => {
  await saveColumnConstraint(
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
) => Promise<void>;

const COLUMN_FIELD_SAVERS: Partial<Record<keyof ColumnType, ColumnFieldSaver>> =
  {
    name: async (erdStore, schemaId, tableId, columnId, value) =>
      await saveColumnName(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as string,
      ),
    type: async (erdStore, schemaId, tableId, columnId, value) =>
      await saveColumnType(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as string,
      ),
    isPrimaryKey: async (erdStore, schemaId, tableId, columnId, value) =>
      await saveColumnPrimaryKey(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as boolean,
      ),
    isNotNull: async (erdStore, schemaId, tableId, columnId, value) =>
      await saveColumnNotNull(
        erdStore,
        schemaId,
        tableId,
        columnId,
        value as boolean,
      ),
  };

export const saveColumnField = async (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  key: keyof ColumnType,
  value: string | boolean,
) => {
  const saver = COLUMN_FIELD_SAVERS[key];
  if (saver) {
    await saver(erdStore, schemaId, tableId, columnId, value);
  }
};
