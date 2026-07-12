import { useState, useEffect } from 'react';
import { Trash2, GripVertical } from 'lucide-react';
import { CONSTRAINTS } from '../../types';
import type {
  EditModeColumnProps,
  DragHandleProps,
  ColumnConstraintsProps,
} from '../../types';
import { TypeSelector } from './TypeSelector';

export const EditModeColumn = ({
  column,
  isLastColumn,
  vendorTypes,
  onDragStart,
  onDragEnd,
  onUpdateColumn,
  onRemoveColumn,
  onPendingChange,
}: EditModeColumnProps) => {
  const [localName, setLocalName] = useState(column.name);

  useEffect(() => {
    setLocalName(column.name);
  }, [column.name]);

  const handleNameChange = (value: string) => {
    setLocalName(value);
    onUpdateColumn(column.id, 'name', value);
  };

  const handleTypeChange = (dataType: string, typeArguments: string) => {
    onUpdateColumn(
      column.id,
      'type',
      JSON.stringify({ dataType, typeArguments }),
    );
  };

  const isFk = column.isForeignKey;
  const isDeleteDisabled = isLastColumn || isFk;
  const deleteTitle = isFk
    ? 'Cannot delete a foreign key column'
    : isLastColumn
      ? 'Cannot delete the last column'
      : 'Remove Column';

  return (
    <div className="space-y-2.5 px-3.5 py-3 text-schemafy-text">
      <div className="flex items-center gap-2">
        <DragHandle
          columnId={column.id}
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
        />

        <input
          type="text"
          value={localName}
          onChange={(e) => handleNameChange(e.target.value)}
          className="schemafy-focus-ring flex-1 rounded-lg border border-schemafy-glass-border bg-schemafy-secondary/60 px-2.5 py-1.5 text-sm text-schemafy-text"
          placeholder="Column name"
        />

        <TypeSelector
          value={column.type}
          typeArguments={column.typeArguments}
          vendorTypes={vendorTypes}
          disabled={isFk}
          onChange={handleTypeChange}
          onPendingChange={onPendingChange}
        />

        <button
          type="button"
          onClick={() => {
            onPendingChange?.(false);
            onRemoveColumn(column.id);
          }}
          disabled={isDeleteDisabled}
          className={`schemafy-focus-ring flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg transition-colors ${
            isDeleteDisabled
              ? 'cursor-not-allowed text-schemafy-dark-gray'
              : 'text-schemafy-destructive hover:bg-schemafy-destructive/10'
          }`}
          title={deleteTitle}
        >
          <Trash2 size={12} />
        </button>
      </div>

      <ColumnConstraints column={column} onUpdateColumn={onUpdateColumn} />
    </div>
  );
};

const DragHandle = ({ columnId, onDragStart, onDragEnd }: DragHandleProps) => {
  return (
    <span
      draggable
      onDragStart={(e) => onDragStart(e, columnId)}
      onDragEnd={onDragEnd}
      className="nodrag flex h-8 w-8 cursor-move items-center justify-center rounded-lg transition-colors hover:bg-schemafy-secondary"
      title="Drag to reorder"
      onMouseDown={(e) => e.stopPropagation()}
    >
      <GripVertical size={12} className="text-schemafy-dark-gray" />
    </span>
  );
};

const ColumnConstraints = ({
  column,
  onUpdateColumn,
}: ColumnConstraintsProps) => {
  return (
    <div className="ml-10 flex flex-wrap gap-x-4 gap-y-2 text-xs">
      {CONSTRAINTS.filter(({ visible }) => visible).map(
        ({ key, label, color }) => {
          const isFkDisabled = column.isForeignKey && key === 'isPrimaryKey';
          const isDisabled =
            isFkDisabled || (column.isPrimaryKey && key === 'isNotNull');
          const title = isFkDisabled
            ? 'Cannot change PK constraint on a foreign key column'
            : undefined;

          return (
            <label
              key={key}
              className={`flex items-center gap-1.5 ${isDisabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
              title={title}
            >
              <input
                type="checkbox"
                checked={column[key]}
                onChange={(e) =>
                  onUpdateColumn(column.id, key, e.target.checked)
                }
                disabled={isDisabled}
                className="h-3.5 w-3.5"
              />
              <span className={`${color} font-medium`}>{label}</span>
            </label>
          );
        },
      )}
    </div>
  );
};
