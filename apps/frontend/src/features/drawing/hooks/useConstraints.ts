import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { Constraint } from '@/types/erd.types';
import { generateUniqueName } from '../utils/nameGenerator';

export type CompositeConstraintKind = 'PRIMARY_KEY' | 'UNIQUE';

interface UseConstraintsProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  tableName: string;
  constraints: Constraint[];
}

export const useConstraints = ({
  erdStore,
  schemaId,
  tableId,
  tableName,
  constraints,
}: UseConstraintsProps) => {
  const createConstraint = (kind: CompositeConstraintKind = 'UNIQUE') => {
    const existingConstraintNames = constraints.map((c) => c.name);
    const prefixMap: Record<CompositeConstraintKind, string> = {
      PRIMARY_KEY: 'pk',
      UNIQUE: 'uq',
    };
    const prefix = prefixMap[kind];

    erdStore.createConstraint(schemaId, tableId, {
      id: ulid(),
      name: generateUniqueName(
        existingConstraintNames,
        `${prefix}_${tableName}_`,
      ),
      kind,
      columns: [],
      isAffected: false,
    });
  };

  const deleteConstraint = (constraintId: string) => {
    erdStore.deleteConstraint(schemaId, tableId, constraintId);
  };

  const changeConstraintName = (constraintId: string, newName: string) => {
    erdStore.changeConstraintName(schemaId, tableId, constraintId, newName);
  };

  const addColumnToConstraint = (constraintId: string, columnId: string) => {
    const constraint = constraints.find((c) => c.id === constraintId);
    if (constraint) {
      erdStore.addColumnToConstraint(schemaId, tableId, constraintId, {
        id: ulid(),
        columnId,
        seqNo: constraint.columns.length,
        isAffected: false,
      });
    }
  };

  const removeColumnFromConstraint = (
    constraintId: string,
    constraintColumnId: string,
  ) => {
    erdStore.removeColumnFromConstraint(
      schemaId,
      tableId,
      constraintId,
      constraintColumnId,
    );
  };

  return {
    createConstraint,
    deleteConstraint,
    changeConstraintName,
    addColumnToConstraint,
    removeColumnFromConstraint,
  };
};
