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
    border-b border-schemafy-glass-border/45 last:border-b-0 transition-colors duration-200
    ${isEditMode ? 'hover:bg-schemafy-secondary/70' : 'hover:bg-schemafy-secondary/35'}
    ${draggedItem === column.id ? 'opacity-50' : ''}
    ${dragOverItem === column.id ? 'border-schemafy-soft-blue bg-schemafy-soft-blue/10' : ''}
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
