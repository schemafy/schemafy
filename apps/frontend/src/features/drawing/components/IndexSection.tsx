import { Plus } from 'lucide-react';
import { IndexRow } from './IndexRow';
import type { IndexSectionProps } from '../types';

export const IndexSection = ({
  indexes,
  tableColumns,
  isEditMode,
  onCreateIndex,
  onDeleteIndex,
  onChangeIndexName,
  onChangeIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onChangeSortDir,
}: IndexSectionProps) => {
  if (indexes.length === 0 && !isEditMode) {
    return null;
  }

  return (
    <div className="border-t-2 border-schemafy-button-bg">
      <div className="bg-schemafy-dark-gray-40 p-2 flex items-center justify-between">
        <span className="text-xs font-medium text-schemafy-text">INDEXES</span>
        {isEditMode && (
          <button
            onClick={onCreateIndex}
            className="p-0.5 text-schemafy-text hover:bg-schemafy-dark-gray-40 rounded transition-colors"
            title="Add Index"
          >
            <Plus size={14} />
          </button>
        )}
      </div>

      <div>
        {indexes.length === 0 ? (
          <div className="p-2 text-center text-schemafy-dark-gray text-xs">No indexes defined</div>
        ) : (
          indexes.map((index) => (
            <IndexRow
              key={index.id}
              index={index}
              tableColumns={tableColumns}
              isEditMode={isEditMode}
              onDeleteIndex={onDeleteIndex}
              onChangeIndexName={onChangeIndexName}
              onChangeIndexType={onChangeIndexType}
              onAddColumnToIndex={onAddColumnToIndex}
              onRemoveColumnFromIndex={onRemoveColumnFromIndex}
              onChangeSortDir={onChangeSortDir}
            />
          ))
        )}
      </div>
    </div>
  );
};
