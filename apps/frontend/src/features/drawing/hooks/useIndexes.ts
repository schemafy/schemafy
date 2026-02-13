import type { IndexType, IndexSortDir } from '../types';
import type { Index } from '@/types';
import { generateUniqueName } from '../utils/nameGenerator';
import {
  useCreateIndex,
  useDeleteIndex,
  useChangeIndexName,
  useChangeIndexType,
  useAddIndexColumn,
  useRemoveIndexColumn,
  useChangeIndexColumnSortDirection,
} from './useIndexMutations';
import { useDebouncedMutation } from './useDebouncedMutation';

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
  const createIndexMutation = useCreateIndex(schemaId);
  const deleteIndexMutation = useDeleteIndex(schemaId);
  const changeIndexNameMutation = useChangeIndexName(schemaId);
  const changeIndexTypeMutation = useChangeIndexType(schemaId);
  const addColumnMutation = useAddIndexColumn(schemaId);
  const removeColumnMutation = useRemoveIndexColumn(schemaId);
  const changeSortDirMutation = useChangeIndexColumnSortDirection(schemaId);

  const debouncedChangeIndexName = useDebouncedMutation(
    changeIndexNameMutation,
  );

  const createIndex = () => {
    const existingIndexNames = indexes.map((idx) => idx.name);

    createIndexMutation.mutate({
      tableId,
      name: generateUniqueName(existingIndexNames, `idx_${tableName}_`),
      type: 'BTREE' as IndexType,
    });
  };

  const deleteIndex = (indexId: string) => {
    deleteIndexMutation.mutate(indexId);
  };

  const changeIndexName = (indexId: string, newName: string) => {
    debouncedChangeIndexName.mutate({
      indexId,
      data: { newName },
    });
  };

  const changeIndexType = (indexId: string, newType: IndexType) => {
    changeIndexTypeMutation.mutate({
      indexId,
      data: { type: newType },
    });
  };

  const addColumnToIndex = (indexId: string, columnId: string) => {
    const index = indexes.find((idx) => idx.id === indexId);
    if (index) {
      addColumnMutation.mutate({
        indexId,
        data: {
          columnId,
          seqNo: index.columns.length,
          sortDirection: 'ASC' as IndexSortDir,
        },
      });
    }
  };

  const removeColumnFromIndex = (indexColumnId: string) => {
    removeColumnMutation.mutate(indexColumnId);
  };

  const changeSortDir = (indexColumnId: string, sortDir: IndexSortDir) => {
    changeSortDirMutation.mutate({
      indexColumnId,
      data: { sortDirection: sortDir },
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
