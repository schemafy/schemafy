import type {
  IndexRowProps,
  ViewModeIndexProps,
  EditModeIndexProps,
  IndexType,
  IndexSortDir,
} from '../types';
import { INDEX_TYPES } from '../types';
import {
  Select,
  SelectGroup,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components';
import {
  EditableRowBase,
  EditableNameInput,
  DeleteButton,
  ColumnItem,
  AddColumnSelector,
} from './EditableRowBase';
import { getColumnName } from '../utils/columnUtils';

export const IndexRow = ({
  index,
  tableColumns,
  isEditMode,
  onDeleteIndex,
  onUpdateIndexName,
  onUpdateIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onUpdateSortDir,
}: IndexRowProps) => {
  return (
    <EditableRowBase
      item={index}
      tableColumns={tableColumns}
      isEditMode={isEditMode}
      renderViewMode={(item, cols) => (
        <ViewModeIndex index={item} tableColumns={cols} />
      )}
      renderEditMode={(item, cols) => (
        <EditModeIndex
          index={item}
          tableColumns={cols}
          onDeleteIndex={onDeleteIndex}
          onUpdateIndexName={onUpdateIndexName}
          onUpdateIndexType={onUpdateIndexType}
          onAddColumnToIndex={onAddColumnToIndex}
          onRemoveColumnFromIndex={onRemoveColumnFromIndex}
          onUpdateSortDir={onUpdateSortDir}
        />
      )}
    />
  );
};

export const ViewModeIndex = ({ index, tableColumns }: ViewModeIndexProps) => {
  const columnsStr = index.columns
    .sort((a, b) => a.seqNo - b.seqNo)
    .map((col) => `${getColumnName(tableColumns, col.columnId)} ${col.sortDir}`)
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
      {index.comment && (
        <div className="text-xs text-dark-gray mt-1 ml-4">
          /* {index.comment} */
        </div>
      )}
    </div>
  );
};

export const EditModeIndex = ({
  index,
  tableColumns,
  onDeleteIndex,
  onUpdateIndexName,
  onUpdateIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onUpdateSortDir,
}: EditModeIndexProps) => {
  const availableColumns = tableColumns.filter(
    (col) => !index.columns.some((idxCol) => idxCol.columnId === col.id),
  );

  return (
    <div className="p-2 space-y-2 bg-schemafy-bg text-schemafy-text">
      <div className="flex items-center gap-2">
        <EditableNameInput
          name={index.name}
          placeholder="Index name"
          onNameChange={(newName) => onUpdateIndexName(index.id, newName)}
        />
        <Select
          onValueChange={(value) =>
            onUpdateIndexType(index.id, value as IndexType)
          }
          value={index.type}
        >
          <SelectTrigger className="text-xs font-mono p-1.5 border border-schemafy-light-gray rounded focus:outline-none w-[6rem]">
            <SelectValue placeholder={index.type} />
          </SelectTrigger>
          <SelectContent popover="auto">
            <SelectGroup>
              {INDEX_TYPES.map((type) => (
                <SelectItem key={type} value={type}>
                  {type}
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>

        <DeleteButton
          onDelete={() => onDeleteIndex(index.id)}
          title="Remove Index"
        />
      </div>

      <div className="ml-4 space-y-1">
        {index.columns
          .sort((a, b) => a.seqNo - b.seqNo)
          .map((indexColumn) => (
            <ColumnItem
              key={indexColumn.id}
              columnName={getColumnName(tableColumns, indexColumn.columnId)}
              onRemove={() => onRemoveColumnFromIndex(indexColumn.id)}
              additionalControls={
                <Select
                  onValueChange={(value) =>
                    onUpdateSortDir(indexColumn.id, value as IndexSortDir)
                  }
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
              }
            />
          ))}

        <AddColumnSelector
          availableColumns={availableColumns}
          onAddColumn={(columnId) => onAddColumnToIndex(index.id, columnId)}
        />
      </div>
    </div>
  );
};
