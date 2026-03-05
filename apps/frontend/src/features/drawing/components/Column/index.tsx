import type { ColumnRowProps } from '../../types';
import { EditModeColumn } from './EditModeColumn';
import { ViewModeColumn } from './ViewModeColumn';

export const ColumnRow = ({
  column,
  isEditMode,
  isLastColumn,
  vendorTypes,
  draggedItem,
  dragOverItem,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  onUpdateColumn,
  onRemoveColumn,
  onPendingChange,
}: ColumnRowProps) => {
  const rowClassName = `
    border-b border-schemafy-light-gray last:border-b-0 transition-colors duration-200
    ${isEditMode ? 'hover:bg-schemafy-secondary' : ''}
    ${draggedItem === column.id ? 'opacity-50' : ''}
    ${dragOverItem === column.id ? 'bg-blue-50 border-blue-200' : ''}
  `.trim();

  return (
    <div
      className={rowClassName}
      onDragOver={(e) => onDragOver(e, column.id)}
      onDragLeave={onDragLeave}
      onDrop={(e) => onDrop(e, column.id)}
    >
      {isEditMode ? (
        <EditModeColumn
          column={column}
          isLastColumn={isLastColumn}
          vendorTypes={vendorTypes}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
          onUpdateColumn={onUpdateColumn}
          onRemoveColumn={onRemoveColumn}
          onPendingChange={(isPending) =>
            onPendingChange?.(column.id, isPending)
          }
        />
      ) : (
        <ViewModeColumn column={column} />
      )}
    </div>
  );
};
