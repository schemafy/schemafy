export type IndexType = 'BTREE' | 'HASH' | 'FULLTEXT' | 'SPATIAL' | 'OTHER';
export type IndexSortDir = 'ASC' | 'DESC';

export type IndexColumnType = {
  id: string;
  indexId: string;
  columnId: string;
  seqNo: number;
  sortDir: IndexSortDir;
};

export type IndexDataType = {
  id: string;
  tableId: string;
  name: string;
  type: IndexType;
  comment?: string | null;
  columns: IndexColumnType[];
};

export interface IndexSectionProps {
  schemaId: string;
  tableId: string;
  indexes: IndexDataType[];
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  onCreateIndex: () => void;
  onDeleteIndex: (indexId: string) => void;
  onChangeIndexName: (indexId: string, newName: string) => void;
  onChangeIndexType: (indexId: string, newType: IndexType) => void;
  onAddColumnToIndex: (indexId: string, columnId: string) => void;
  onRemoveColumnFromIndex: (indexId: string, indexColumnId: string) => void;
  onChangeSortDir: (indexId: string, indexColumnId: string, sortDir: IndexSortDir) => void;
}

export interface IndexRowProps {
  index: IndexDataType;
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  onDeleteIndex: (indexId: string) => void;
  onChangeIndexName: (indexId: string, newName: string) => void;
  onChangeIndexType: (indexId: string, newType: IndexType) => void;
  onAddColumnToIndex: (indexId: string, columnId: string) => void;
  onRemoveColumnFromIndex: (indexId: string, indexColumnId: string) => void;
  onChangeSortDir: (indexId: string, indexColumnId: string, sortDir: IndexSortDir) => void;
}

export interface ViewModeIndexProps {
  index: IndexDataType;
  tableColumns: Array<{ id: string; name: string }>;
}

export interface EditModeIndexProps {
  index: IndexDataType;
  tableColumns: Array<{ id: string; name: string }>;
  onDeleteIndex: (indexId: string) => void;
  onChangeIndexName: (indexId: string, newName: string) => void;
  onChangeIndexType: (indexId: string, newType: IndexType) => void;
  onAddColumnToIndex: (indexId: string, columnId: string) => void;
  onRemoveColumnFromIndex: (indexId: string, indexColumnId: string) => void;
  onChangeSortDir: (indexId: string, indexColumnId: string, sortDir: IndexSortDir) => void;
}

export const INDEX_TYPES: IndexType[] = ['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL', 'OTHER'];
