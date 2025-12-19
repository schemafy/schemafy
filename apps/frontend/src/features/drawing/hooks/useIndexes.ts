import type { IndexType, IndexSortDir } from '../types';
import type { Index } from '@schemafy/validator';
import { generateUniqueName } from '../utils/nameGenerator';
import * as indexService from '../services/index.service';
import { toast } from 'sonner';
import { ErdStore } from '@/store/erd.store';

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

  const changeIndexName = async (indexId: string, newName: string) => {
    try {
      await indexService.updateIndexName(schemaId, tableId, indexId, newName);
    } catch (error) {
      toast.error('Failed to update index name');
      console.error(error);
    }
  };

  const changeIndexType = async (indexId: string, newType: IndexType) => {
    const erdStore = ErdStore.getInstance();
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    try {
      erdStore.deleteIndex(schemaId, tableId, indexId);
      erdStore.createIndex(schemaId, tableId, {
        ...index,
        type: newType,
      });
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
    const erdStore = ErdStore.getInstance();
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    const indexColumn = index.columns.find((col) => col.id === indexColumnId);
    if (!indexColumn) return;

    try {
      erdStore.deleteIndex(schemaId, tableId, indexId);
      erdStore.createIndex(schemaId, tableId, {
        ...index,
        columns: index.columns.map((col) =>
          col.id === indexColumnId ? { ...col, sortDir } : col,
        ),
      });
    } catch (error) {
      toast.error('Failed to change sort direction');
      console.error(error);
    }
  };

  return {
    createIndex,
    deleteIndex,
    changeIndexName,
    changeIndexType,
    addColumnToIndex,
    removeColumnFromIndex,
    changeSortDir,
  };
};
