import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType, ConstraintKind } from '../types';

export const saveColumnName = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  name: string,
) => {
  erdStore.changeColumnName(schemaId, tableId, columnId, name);
};

export const saveColumnType = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  dataType: string,
) => {
  erdStore.changeColumnType(schemaId, tableId, columnId, dataType);
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

  const table = erdStore.erdState.database.schemas.find((s) => s.id === schemaId)?.tables.find((t) => t.id === tableId);

  if (!table) {
    throw new Error('Table not found');
  }

  const existingConstraint = table.constraints.find(
    (c) => c.kind === constraintKind && c.columns.some((cc) => cc.columnId === columnId),
  );

  if (enabled && !existingConstraint) {
    const prefixMap: Record<ConstraintKind, string> = {
      PRIMARY_KEY: 'pk',
      NOT_NULL: 'nn',
      UNIQUE: 'uq',
    };
    const prefix = prefixMap[constraintKind];

    erdStore.createConstraint(schemaId, tableId, {
      id: ulid(),
      name: `${prefix}_${columnId}`,
      kind: constraintKind,
      columns: [{ id: ulid(), columnId, seqNo: 0, constraintId: '' }],
    });
  } else if (!enabled && existingConstraint) {
    erdStore.deleteConstraint(schemaId, tableId, existingConstraint.id);
  }
};

export const saveColumnPrimaryKey = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isPrimaryKey: boolean,
) => {
  saveColumnConstraint(erdStore, schemaId, tableId, columnId, 'PRIMARY_KEY', isPrimaryKey);

  if (isPrimaryKey) {
    saveColumnConstraint(erdStore, schemaId, tableId, columnId, 'NOT_NULL', true);
  }
};

export const saveColumnNotNull = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isNotNull: boolean,
) => {
  saveColumnConstraint(erdStore, schemaId, tableId, columnId, 'NOT_NULL', isNotNull);
};

export const saveColumnUnique = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  isUnique: boolean,
) => {
  saveColumnConstraint(erdStore, schemaId, tableId, columnId, 'UNIQUE', isUnique);
};

type ColumnFieldSaver = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
  columnId: string,
  value: string | boolean,
) => void;

const COLUMN_FIELD_SAVERS: Partial<Record<keyof ColumnType, ColumnFieldSaver>> = {
  name: (erdStore, schemaId, tableId, columnId, value) =>
    saveColumnName(erdStore, schemaId, tableId, columnId, value as string),
  type: (erdStore, schemaId, tableId, columnId, value) =>
    saveColumnType(erdStore, schemaId, tableId, columnId, value as string),
  isPrimaryKey: (erdStore, schemaId, tableId, columnId, value) =>
    saveColumnPrimaryKey(erdStore, schemaId, tableId, columnId, value as boolean),
  isNotNull: (erdStore, schemaId, tableId, columnId, value) =>
    saveColumnNotNull(erdStore, schemaId, tableId, columnId, value as boolean),
  isUnique: (erdStore, schemaId, tableId, columnId, value) =>
    saveColumnUnique(erdStore, schemaId, tableId, columnId, value as boolean),
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
