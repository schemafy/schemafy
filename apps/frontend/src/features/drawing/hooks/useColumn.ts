import { useRef, useEffect } from 'react';
import { debounce } from 'lodash-es';
import type { DebouncedFunc } from 'lodash-es';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType } from '../types';
import { saveColumnField } from '../utils/columnHelpers';
import { toast } from 'sonner';

export const useColumn = (
  erdStore: ErdStore,
  schemaId: string,
  tableId: string,
) => {
  const debouncedSaveRef = useRef<
    Map<string, DebouncedFunc<(value: string | boolean) => void>>
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

  const saveColumn = (
    columnId: string,
    key: keyof ColumnType,
    value: string | boolean,
  ) => {
    if (erdStore.erdState.state !== 'loaded') return;

    try {
      saveColumnField(erdStore, schemaId, tableId, columnId, key, value);
    } catch {
      toast.error('Failed to save column change');
    }
  };

  const updateColumn = (
    columnId: string,
    key: keyof ColumnType,
    value: string | boolean,
  ) => {
    if (typeof value === 'boolean' || key === 'type') {
      saveColumn(columnId, key, value);
      return;
    }

    const operationKey = `${columnId}-${key}`;

    let debouncedSave = debouncedSaveRef.current.get(operationKey);
    if (!debouncedSave) {
      debouncedSave = debounce((val: string | boolean) => {
        saveColumn(columnId, key, val);
      }, 300);

      debouncedSaveRef.current.set(operationKey, debouncedSave);
    }

    debouncedSave(value);
  };

  const saveAllPendingChanges = () => {
    if (erdStore.erdState.state !== 'loaded') return;

    debouncedSaveRef.current.forEach((debouncedFn) => {
      debouncedFn.flush();
    });
  };

  return {
    updateColumn,
    saveAllPendingChanges,
  };
};
