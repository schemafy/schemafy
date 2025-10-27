import { useState, useEffect } from 'react';
import { Trash2, X } from 'lucide-react';
import type { IndexRowProps, ViewModeIndexProps, EditModeIndexProps, IndexType, IndexSortDir } from '../types';
import { Select, SelectGroup, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components';

export const IndexRow = ({
  index,
  tableColumns,
  isEditMode,
  onDeleteIndex,
  onChangeIndexName,
  onChangeIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onChangeSortDir,
}: IndexRowProps) => {
  return (
    <div className="border-b border-schemafy-light-gray last:border-b-0">
      {isEditMode ? (
        <EditModeIndex
          index={index}
          tableColumns={tableColumns}
          onDeleteIndex={onDeleteIndex}
          onChangeIndexName={onChangeIndexName}
          onChangeIndexType={onChangeIndexType}
          onAddColumnToIndex={onAddColumnToIndex}
          onRemoveColumnFromIndex={onRemoveColumnFromIndex}
          onChangeSortDir={onChangeSortDir}
        />
      ) : (
        <ViewModeIndex index={index} tableColumns={tableColumns} />
      )}
    </div>
  );
};

export const ViewModeIndex = ({ index, tableColumns }: ViewModeIndexProps) => {
  const getColumnName = (columnId: string) => {
    return tableColumns.find((col) => col.id === columnId)?.name || 'Unknown';
  };

  const columnsStr = index.columns
    .sort((a, b) => a.seqNo - b.seqNo)
    .map((col) => `${getColumnName(col.columnId)} ${col.sortDir}`)
    .join(', ');

  return (
    <div className="p-2">
      <div className="text-xs text-schemafy-dark-gray font-mono">
        <span className="text-schemafy-text font-medium">{index.name}</span>
        {columnsStr && (
          <>
            {' '}
            (<span className="text-schemafy-blue">{columnsStr}</span>)
          </>
        )}
        {index.type !== 'BTREE' && (
          <>
            {' '}
            <span className="text-schemafy-dark-gray">USING {index.type}</span>
          </>
        )}
      </div>
      {index.comment && <div className="text-xs text-dark-gray mt-1 ml-4">/* {index.comment} */</div>}
    </div>
  );
};

export const EditModeIndex = ({
  index,
  tableColumns,
  onDeleteIndex,
  onChangeIndexName,
  onChangeIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onChangeSortDir,
}: EditModeIndexProps) => {
  const [localName, setLocalName] = useState(index.name);

  useEffect(() => {
    setLocalName(index.name);
  }, [index.name]);

  const handleNameChange = (value: string) => {
    setLocalName(value);
    onChangeIndexName(index.id, value);
  };

  const getColumnName = (columnId: string) => {
    return tableColumns.find((col) => col.id === columnId)?.name || 'Unknown';
  };

  const availableColumns = tableColumns.filter((col) => !index.columns.some((idxCol) => idxCol.columnId === col.id));

  const indexTypes: IndexType[] = ['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL', 'OTHER'];

  return (
    <div className="p-2 space-y-2 bg-schemafy-bg text-schemafy-text">
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={localName}
          onChange={(e) => handleNameChange(e.target.value)}
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none"
          placeholder="Index name"
        />
        <Select onValueChange={(value) => onChangeIndexType(index.id, value as IndexType)} value={index.type}>
          <SelectTrigger className="text-xs font-mono p-1.5 border border-schemafy-light-gray rounded focus:outline-none w-[6rem]">
            <SelectValue placeholder={index.type} />
          </SelectTrigger>
          <SelectContent popover="auto">
            <SelectGroup>
              {indexTypes.map((type) => (
                <SelectItem key={type} value={type}>
                  {type}
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>

        <button
          onClick={() => onDeleteIndex(index.id)}
          className="p-1 rounded flex-shrink-0 text-schemafy-destructive hover:bg-red-100"
          title="Remove Index"
        >
          <Trash2 size={12} />
        </button>
      </div>

      <div className="ml-4 space-y-1">
        {index.columns
          .sort((a, b) => a.seqNo - b.seqNo)
          .map((indexColumn) => (
            <div key={indexColumn.id} className="flex items-center gap-2 text-xs">
              <span className="text-schemafy-blue font-medium">{getColumnName(indexColumn.columnId)}</span>
              <Select
                onValueChange={(value) => onChangeSortDir(index.id, indexColumn.id, value as IndexSortDir)}
                value={indexColumn.sortDir}
              >
                <SelectTrigger className="text-xs font-mono py-1 px-1.5 border border-schemafy-light-gray rounded focus:outline-none w-[6rem]">
                  <SelectValue placeholder={indexColumn.sortDir} />
                </SelectTrigger>
                <SelectContent popover="auto">
                  <SelectGroup>
                    <SelectItem value="ASC">ASC</SelectItem>
                    <SelectItem value="DESC">DESC</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>

              <button
                onClick={() => onRemoveColumnFromIndex(index.id, indexColumn.id)}
                className="p-0.5 rounded"
                title="Remove column from index"
              >
                <X size={12} />
              </button>
            </div>
          ))}

        {availableColumns.length > 0 && (
          <div className="flex items-center gap-2">
            <Select
              onValueChange={(value) => {
                if (value) {
                  onAddColumnToIndex(index.id, value);
                  value = '';
                }
              }}
              value=""
            >
              <SelectTrigger className="w-[8rem] text-xs font-mono py-1 px-1.5 border border-schemafy-light-gray rounded focus:outline-none">
                <SelectValue placeholder="+ Add column" />
              </SelectTrigger>
              <SelectContent popover="auto">
                <SelectGroup>
                  {availableColumns.map((col) => (
                    <SelectItem value={col.id} key={col.id}>
                      {col.name}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
        )}
      </div>
    </div>
  );
};
