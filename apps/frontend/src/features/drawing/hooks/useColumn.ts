import { type ColumnType, CONSTRAINT_PREFIX_MAP } from '../types';
import type { Constraint } from '@/types';
import { useChangeColumnName, useChangeColumnType } from './useColumnMutations';
import {
  useCreateConstraint,
  useDeleteConstraint,
  useAddConstraintColumn,
  useRemoveConstraintColumn,
} from './useConstraintMutations';
import { useDebouncedMutation } from './useDebouncedMutation';

export const useColumn = (
  schemaId: string,
  tableId: string,
  tableName: string,
  tableConstraints: Constraint[],
) => {
  const changeColumnNameMutation = useChangeColumnName(schemaId);
  const debouncedChangeColumnName = useDebouncedMutation(
    changeColumnNameMutation,
  );
  const changeColumnTypeMutation = useChangeColumnType(schemaId);
  const createConstraintMutation = useCreateConstraint(schemaId);
  const deleteConstraintMutation = useDeleteConstraint(schemaId);
  const addConstraintColumnMutation = useAddConstraintColumn(schemaId);
  const removeConstraintColumnMutation = useRemoveConstraintColumn(schemaId);

  const saveColumnName = (columnId: string, name: string) => {
    debouncedChangeColumnName.mutate({
      columnId,
      data: { newName: name },
    });
  };

  const saveColumnType = (columnId: string, dataType: string) => {
    changeColumnTypeMutation.mutate({
      columnId,
      data: { dataType },
    });
  };

  const saveColumnConstraint = (
    columnId: string,
    constraintKind: string,
    enabled: boolean,
  ) => {
    const existingConstraint = tableConstraints.find(
      (c) =>
        c.kind === constraintKind &&
        c.columns.some((cc) => cc.columnId === columnId),
    );

    if (enabled && !existingConstraint) {
      if (constraintKind === 'PRIMARY_KEY') {
        const existingPk = tableConstraints.find(
          (c) => c.kind === 'PRIMARY_KEY',
        );

        if (existingPk) {
          addConstraintColumnMutation.mutate({
            constraintId: existingPk.id,
            data: {
              columnId,
              seqNo: existingPk.columns.length,
            },
          });
        } else {
          createConstraintMutation.mutate({
            tableId,
            name: `pk_${tableName}`,
            kind: 'PRIMARY_KEY',
            columns: [{ columnId, seqNo: 0 }],
          });
        }
      } else {
        const prefix = CONSTRAINT_PREFIX_MAP[constraintKind];
        createConstraintMutation.mutate({
          tableId,
          name: `${prefix}_${columnId}`,
          kind: constraintKind,
          columns: [{ columnId, seqNo: 0 }],
        });
      }
    } else if (!enabled && existingConstraint) {
      if (constraintKind === 'PRIMARY_KEY') {
        const pkColumn = existingConstraint.columns.find(
          (cc) => cc.columnId === columnId,
        );
        if (pkColumn) {
          removeConstraintColumnMutation.mutate(pkColumn.id);
        }
      } else {
        deleteConstraintMutation.mutate(existingConstraint.id);
      }
    }
  };

  const updateColumn = (
    columnId: string,
    key: keyof ColumnType,
    value: string | boolean,
  ) => {
    switch (key) {
      case 'name':
        saveColumnName(columnId, value as string);
        break;
      case 'type':
        saveColumnType(columnId, value as string);
        break;
      case 'isPrimaryKey':
        saveColumnConstraint(columnId, 'PRIMARY_KEY', value as boolean);
        break;
      case 'isNotNull':
        saveColumnConstraint(columnId, 'NOT_NULL', value as boolean);
        break;
    }
  };

  const saveAllPendingChanges = () => {
    debouncedChangeColumnName.flush();
  };

  return {
    updateColumn,
    saveAllPendingChanges,
  };
};
