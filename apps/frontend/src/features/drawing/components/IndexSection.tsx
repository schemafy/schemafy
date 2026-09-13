import { Plus } from 'lucide-react';
import { IndexRow } from './IndexRow';
import type { IndexSectionProps } from '../types';

export const IndexSection = ({
  indexes,
  tableColumns,
  isEditMode,
  onCreateIndex,
  onDeleteIndex,
  onUpdateIndexName,
  onUpdateIndexType,
  onAddColumnToIndex,
  onRemoveColumnFromIndex,
  onUpdateSortDir,
}: IndexSectionProps) => {
  if (indexes.length === 0 && !isEditMode) {
    return null;
  }

  return (
    <div className="border-t border-schemafy-glass-border/55">
      <div className="flex items-center justify-between bg-schemafy-secondary/35 px-3 py-1.5">
        <span className="font-overline-xs text-schemafy-dark-gray">
          INDEXES
        </span>
        {isEditMode && (
          <button
            type="button"
            onClick={onCreateIndex}
            className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
            title="Add Index"
          >
            <Plus size={14} />
          </button>
        )}
      </div>

      <div>
        {indexes.length === 0 ? (
          <div className="px-3 py-2.5 text-center text-xs text-schemafy-dark-gray">
            No indexes defined
          </div>
        ) : (
          indexes.map((index) => (
            <IndexRow
              key={index.id}
              index={index}
              tableColumns={tableColumns}
              isEditMode={isEditMode}
              onDeleteIndex={onDeleteIndex}
              onUpdateIndexName={onUpdateIndexName}
              onUpdateIndexType={onUpdateIndexType}
              onAddColumnToIndex={onAddColumnToIndex}
              onRemoveColumnFromIndex={onRemoveColumnFromIndex}
              onUpdateSortDir={onUpdateSortDir}
            />
          ))
        )}
      </div>
    </div>
  );
};
