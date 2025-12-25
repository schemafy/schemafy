import { useRef, useEffect } from 'react';
import { debounce } from 'lodash-es';
import type { DebouncedFunc } from 'lodash-es';
import type { Constraint } from '@schemafy/validator';
import { generateUniqueName } from '../utils/nameGenerator';
import * as constraintService from '../services/constraint.service';
import { toast } from 'sonner';

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
  const debouncedSaveRef = useRef<
    Map<string, DebouncedFunc<(name: string) => void>>
  >(new Map());

  useEffect(() => {
    const currentMap = debouncedSaveRef.current;
    return () => {
      currentMap.forEach((debouncedFn) => {
        debouncedFn.cancel();
      });
      currentMap.clear();
    };
  }, []);

  const createConstraint = async (kind: CompositeConstraintKind = 'UNIQUE') => {
    const existingConstraintNames = constraints.map((c) => c.name);
    const prefixMap: Record<CompositeConstraintKind, string> = {
      PRIMARY_KEY: 'pk',
      UNIQUE: 'uq',
    };
    const prefix = prefixMap[kind];

    try {
      await constraintService.createConstraint(
        schemaId,
        tableId,
        generateUniqueName(existingConstraintNames, `${prefix}_${tableName}_`),
        kind,
        [],
      );
    } catch (error) {
      toast.error('Failed to create constraint');
      console.error(error);
    }
  };

  const deleteConstraint = async (constraintId: string) => {
    try {
      await constraintService.deleteConstraint(schemaId, tableId, constraintId);
    } catch (error) {
      toast.error('Failed to delete constraint');
      console.error(error);
    }
  };

  const saveConstraintName = async (constraintId: string, newName: string) => {
    try {
      await constraintService.updateConstraintName(
        schemaId,
        tableId,
        constraintId,
        newName,
      );
    } catch (error) {
      toast.error('Failed to update constraint name');
      console.error(error);
    }
  };

  const changeConstraintName = (constraintId: string, newName: string) => {
    let debouncedSave = debouncedSaveRef.current.get(constraintId);
    if (!debouncedSave) {
      debouncedSave = debounce((name: string) => {
        saveConstraintName(constraintId, name);
      }, 300);

      debouncedSaveRef.current.set(constraintId, debouncedSave);
    }

    debouncedSave(newName);
  };

  const addColumnToConstraint = async (
    constraintId: string,
    columnId: string,
  ) => {
    const constraint = constraints.find((c) => c.id === constraintId);
    if (!constraint) return;

    try {
      await constraintService.addColumnToConstraint(
        schemaId,
        tableId,
        constraintId,
        columnId,
        constraint.columns.length,
      );
    } catch (error) {
      toast.error('Failed to add column to constraint');
      console.error(error);
    }
  };

  const removeColumnFromConstraint = async (
    constraintId: string,
    constraintColumnId: string,
  ) => {
    try {
      await constraintService.removeColumnFromConstraint(
        schemaId,
        tableId,
        constraintId,
        constraintColumnId,
      );
    } catch (error) {
      toast.error('Failed to remove column from constraint');
      console.error(error);
    }
  };

  const saveAllPendingChanges = () => {
    debouncedSaveRef.current.forEach((debouncedFn) => {
      debouncedFn.flush();
    });
  };

  return {
    createConstraint,
    deleteConstraint,
    changeConstraintName,
    addColumnToConstraint,
    removeColumnFromConstraint,
    saveAllPendingChanges,
  };
};
