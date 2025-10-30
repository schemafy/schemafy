import { ulid } from 'ulid';
import type { ErdStore } from '@/store/erd.store';
import type { IndexType, IndexSortDir } from '../../types';
import type { Index } from '@schemafy/validator';

interface UseIndexActionsProps {
  erdStore: ErdStore;
  schemaId: string;
  tableId: string;
  tableName: string;
  indexes: Index[];
}

export const useIndexActions = ({ erdStore, schemaId, tableId, tableName, indexes }: UseIndexActionsProps) => {
  const createIndex = () => {
    erdStore.createIndex(schemaId, tableId, {
      id: ulid(),
      name: `idx_${tableName}_${indexes.length + 1}`,
      type: 'BTREE' as IndexType,
      comment: null,
      columns: [],
    });
  };

  const deleteIndex = (indexId: string) => {
    erdStore.deleteIndex(schemaId, tableId, indexId);
  };

  const changeIndexName = (indexId: string, newName: string) => {
    erdStore.changeIndexName(schemaId, tableId, indexId, newName);
  };

  const changeIndexType = (indexId: string, newType: IndexType) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    erdStore.deleteIndex(schemaId, tableId, indexId);
    erdStore.createIndex(schemaId, tableId, {
      ...index,
      type: newType,
    });
  };

  const addColumnToIndex = (indexId: string, columnId: string) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (index) {
      erdStore.addColumnToIndex(schemaId, tableId, indexId, {
        id: ulid(),
        columnId,
        seqNo: index.columns.length + 1,
        sortDir: 'ASC' as IndexSortDir,
      });
    }
  };

  const removeColumnFromIndex = (indexId: string, indexColumnId: string) => {
    erdStore.removeColumnFromIndex(schemaId, tableId, indexId, indexColumnId);
  };

  const changeSortDir = (indexId: string, indexColumnId: string, sortDir: IndexSortDir) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (!index) return;

    const indexColumn = index.columns.find((col) => col.id === indexColumnId);
    if (!indexColumn) return;

    erdStore.deleteIndex(schemaId, tableId, indexId);
    erdStore.createIndex(schemaId, tableId, {
      ...index,
      columns: index.columns.map((col) => (col.id === indexColumnId ? { ...col, sortDir } : col)),
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
  };
};
