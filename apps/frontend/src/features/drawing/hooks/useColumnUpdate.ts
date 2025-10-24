import { useRef, useCallback } from 'react';
import { debounce } from 'lodash-es';
import type { ErdStore } from '@/store/erd.store';
import type { ColumnType } from '../types';
import { saveColumnField } from '../utils/columnHelpers';

export const useColumnUpdate = (erdStore: ErdStore, schemaId: string, tableId: string) => {
  const debouncedSaveRef = useRef<Map<string, ReturnType<typeof debounce>>>(new Map());

  const updateColumn = useCallback(
    (columnId: string, key: keyof ColumnType, value: string | boolean) => {
      if (typeof value === 'boolean' || key === 'type') {
        if (erdStore.erdState.state !== 'loaded') return;

        try {
          saveColumnField(erdStore, schemaId, tableId, columnId, key, value);
        } catch (error) {
          console.error('Failed to save column change:', error);
          // TODO: 토스트 메시지 표시
        }
        return;
      }

      const operationKey = `${columnId}-${key}`;

      let debouncedSave = debouncedSaveRef.current.get(operationKey);
      if (!debouncedSave) {
        debouncedSave = debounce((col: string, k: keyof ColumnType, val: string | boolean) => {
          if (erdStore.erdState.state !== 'loaded') return;

          try {
            saveColumnField(erdStore, schemaId, tableId, col, k, val);
          } catch (error) {
            console.error('Failed to save column change:', error);
            // TODO: 토스트 메시지 표시
          }
        }, 300);

        debouncedSaveRef.current.set(operationKey, debouncedSave);
      }

      debouncedSave(columnId, key, value);
    },
    [erdStore, schemaId, tableId],
  );

  const saveAllPendingChanges = useCallback(() => {
    if (erdStore.erdState.state !== 'loaded') return;

    debouncedSaveRef.current.forEach((debouncedFn) => {
      debouncedFn.flush();
    });
  }, [erdStore]);

  return {
    updateColumn,
    saveAllPendingChanges,
  };
};
