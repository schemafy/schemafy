import { useRef, useEffect } from 'react';
import { debounce } from 'lodash-es';
import type { DebouncedFunc } from 'lodash-es';
import type { IndexType, IndexSortDir } from '../types';
import type { Index } from '@schemafy/validator';
import { generateUniqueName } from '../utils/nameGenerator';
import * as indexService from '../services/index.service';
import { toast } from 'sonner';

interface UseIndexesProps {
  schemaId: string;
  tableId: string;
  tableName: string;
  indexes: Index[];
}

export const useIndexes = ({
  schemaId,
  tableId,
  tableName,
  indexes,
}: UseIndexesProps) => {
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

  const createIndex = async () => {
    const existingIndexNames = indexes.map((idx) => idx.name);

    try {
      await indexService.createIndex(
        schemaId,
        tableId,
        generateUniqueName(existingIndexNames, `idx_${tableName}_`),
        'BTREE',
        [],
      );
    } catch (error) {
      toast.error('Failed to create index');
      console.error(error);
    }
  };

  const deleteIndex = async (indexId: string) => {
    try {
      await indexService.deleteIndex(schemaId, tableId, indexId);
    } catch (error) {
      toast.error('Failed to delete index');
      console.error(error);
    }
  };

  const saveIndexName = async (indexId: string, newName: string) => {
    try {
      await indexService.updateIndexName(schemaId, tableId, indexId, newName);
    } catch (error) {
      toast.error('Failed to update index name');
      console.error(error);
    }
  };

  const changeIndexName = (indexId: string, newName: string) => {
    let debouncedSave = debouncedSaveRef.current.get(indexId);
    if (!debouncedSave) {
      debouncedSave = debounce((name: string) => {
        saveIndexName(indexId, name);
      }, 300);

      debouncedSaveRef.current.set(indexId, debouncedSave);
    }

    debouncedSave(newName);
  };

  const changeIndexType = async (indexId: string, newType: IndexType) => {
    try {
      await indexService.updateIndexType(schemaId, tableId, indexId, newType);
    } catch (error) {
      toast.error('Failed to update index type');
      console.error(error);
    }
  };

  const addColumnToIndex = async (indexId: string, columnId: string) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    try {
      await indexService.addColumnToIndex(
        schemaId,
        tableId,
        indexId,
        columnId,
        index.columns.length + 1,
      );
    } catch (error) {
      toast.error('Failed to add column to index');
      console.error(error);
    }
  };

  const removeColumnFromIndex = async (
    indexId: string,
    indexColumnId: string,
  ) => {
    try {
      await indexService.removeColumnFromIndex(
        schemaId,
        tableId,
        indexId,
        indexColumnId,
      );
    } catch (error) {
      toast.error('Failed to remove column from index');
      console.error(error);
    }
  };

  const changeSortDir = async (
    indexId: string,
    indexColumnId: string,
    sortDir: IndexSortDir,
  ) => {
    try {
      await indexService.updateIndexColumnSortDir(
        schemaId,
        tableId,
        indexId,
        indexColumnId,
        sortDir,
      );
    } catch (error) {
      toast.error('Failed to change sort direction');
      console.error(error);
    }
  };

  const saveAllPendingChanges = () => {
    debouncedSaveRef.current.forEach((debouncedFn) => {
      debouncedFn.flush();
    });
  };

  return {
    createIndex,
    deleteIndex,
    changeIndexName,
    changeIndexType,
    addColumnToIndex,
    removeColumnFromIndex,
    changeSortDir,
    saveAllPendingChanges,
  };
};
