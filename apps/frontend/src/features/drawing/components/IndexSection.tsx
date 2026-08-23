import { Plus } from 'lucide-react';
import { IndexRow } from './IndexRow';
import type { IndexSectionProps } from '../types';
import { getCapabilitiesUnavailableMessage } from '../utils/indexUtils';

export const IndexSection = ({
  indexes,
  tableColumns,
  isEditMode,
  indexCapabilities,
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

  const canCreateIndex = indexCapabilities.supportedTypes.length > 0;

  return (
    <div className="border-t border-schemafy-glass-border/55">
      <div className="flex items-center justify-between bg-schemafy-secondary/35 px-3 py-1.5">
        <span className="font-overline-xs text-schemafy-dark-gray">
          INDEXES
        </span>
        {isEditMode && (
          <span
            title={
              canCreateIndex
                ? undefined
                : getCapabilitiesUnavailableMessage(indexCapabilities)
            }
          >
            <button
              type="button"
              onClick={onCreateIndex}
              disabled={!canCreateIndex}
              title={canCreateIndex ? 'Add Index' : undefined}
              className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text disabled:pointer-events-none disabled:opacity-50"
            >
              <Plus size={14} />
            </button>
          </span>
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
              indexCapabilities={indexCapabilities}
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
