import type { Constraint } from '@/types/erd.types';
import { generateUniqueName } from '../utils/nameGenerator';
import {
  useCreateConstraint,
  useDeleteConstraint,
  useChangeConstraintName,
  useAddConstraintColumn,
  useRemoveConstraintColumn,
} from './useConstraintMutations';
import { useDebouncedMutation } from './useDebouncedMutation';

export type CompositeConstraintKind = 'PRIMARY_KEY' | 'UNIQUE';

interface UseConstraintsProps {
  schemaId: string;
  tableId: string;
  tableName: string;
  constraints: Constraint[];
}

export const useConstraints = ({
  schemaId,
  tableId,
  tableName,
  constraints,
}: UseConstraintsProps) => {
  const createConstraintMutation = useCreateConstraint(schemaId);
  const deleteConstraintMutation = useDeleteConstraint(schemaId);
  const changeConstraintNameMutation = useChangeConstraintName(schemaId);
  const addColumnMutation = useAddConstraintColumn(schemaId);
  const removeColumnMutation = useRemoveConstraintColumn(schemaId);

  const debouncedChangeConstraintName = useDebouncedMutation(
    changeConstraintNameMutation,
  );

  const createConstraint = (kind: CompositeConstraintKind = 'UNIQUE') => {
    const existingConstraintNames = constraints.map((c) => c.name);
    const prefixMap: Record<CompositeConstraintKind, string> = {
      PRIMARY_KEY: 'pk',
      UNIQUE: 'uq',
    };
    const prefix = prefixMap[kind];

    createConstraintMutation.mutate({
      tableId,
      name: generateUniqueName(
        existingConstraintNames,
        `${prefix}_${tableName}_`,
      ),
      kind,
    });
  };

  const deleteConstraint = (constraintId: string) => {
    deleteConstraintMutation.mutate(constraintId);
  };

  const updateConstraintName = (constraintId: string, newName: string) => {
    debouncedChangeConstraintName.mutate({
      constraintId,
      data: { newName },
    });
  };

  const addColumnToConstraint = (constraintId: string, columnId: string) => {
    const constraint = constraints.find((c) => c.id === constraintId);
    if (constraint) {
      addColumnMutation.mutate({
        constraintId,
        data: {
          columnId,
          seqNo: constraint.columns.length,
        },
      });
    }
  };

  const removeColumnFromConstraint = (constraintColumnId: string) => {
    removeColumnMutation.mutate(constraintColumnId);
  };

  const saveAllPendingChanges = () => {
    debouncedChangeConstraintName.flush();
  };

  return {
    createConstraint,
    deleteConstraint,
    updateConstraintName,
    addColumnToConstraint,
    removeColumnFromConstraint,
    saveAllPendingChanges,
  };
};
