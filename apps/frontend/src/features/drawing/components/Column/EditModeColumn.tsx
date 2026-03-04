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

  const handleTypeChange = (dataType: string, lengthScale: string) => {
    onUpdateColumn(
      column.id,
      'type',
      JSON.stringify({ dataType, lengthScale }),
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
    <div className="p-2 space-y-2 text-schemafy-text">
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
          className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="Column name"
        />

        <TypeSelector
          value={column.type}
          lengthScale={column.lengthScale}
          vendorTypes={vendorTypes}
          disabled={isFk}
          onChange={handleTypeChange}
          onPendingChange={onPendingChange}
        />

        <button
          onClick={() => {
            onPendingChange?.(false);
            onRemoveColumn(column.id);
          }}
          disabled={isDeleteDisabled}
          className={`p-1 rounded flex-shrink-0 ${
            isDeleteDisabled
              ? 'text-schemafy-dark-gray cursor-not-allowed'
              : 'text-schemafy-destructive hover:bg-red-100'
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
      className="cursor-move p-1 hover:bg-schemafy-light-gray rounded transition-colors nodrag"
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
    <div className="flex flex-wrap gap-3 text-xs ml-4">
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
              className={`flex items-center gap-1 ${isDisabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
              title={title}
            >
              <input
                type="checkbox"
                checked={column[key]}
                onChange={(e) =>
                  onUpdateColumn(column.id, key, e.target.checked)
                }
                disabled={isDisabled}
                className="w-3 h-3"
              />
              <span className={`${color} font-medium`}>{label}</span>
            </label>
          );
        },
      )}
    </div>
  );
};
