import { useState, useEffect, type ReactNode } from 'react';
import { Trash2, X } from 'lucide-react';
import { Select, SelectGroup, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components';

export interface BaseItem {
  id: string;
  name: string;
  columns: Array<{ id: string; columnId: string; seqNo: number }>;
}

export interface EditableRowBaseProps<T extends BaseItem> {
  item: T;
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  renderViewMode: (item: T, tableColumns: Array<{ id: string; name: string }>) => ReactNode;
  renderEditMode: (item: T, tableColumns: Array<{ id: string; name: string }>) => ReactNode;
}

export function EditableRowBase<T extends BaseItem>({
  item,
  tableColumns,
  isEditMode,
  renderViewMode,
  renderEditMode,
}: EditableRowBaseProps<T>) {
  return (
    <div className="border-b border-schemafy-light-gray last:border-b-0">
      {isEditMode ? renderEditMode(item, tableColumns) : renderViewMode(item, tableColumns)}
    </div>
  );
}

export interface EditableNameInputProps {
  name: string;
  placeholder: string;
  onNameChange: (newName: string) => void;
}

export const EditableNameInput = ({ name, placeholder, onNameChange }: EditableNameInputProps) => {
  const [localName, setLocalName] = useState(name);

  useEffect(() => {
    setLocalName(name);
  }, [name]);

  const handleChange = (value: string) => {
    setLocalName(value);
    onNameChange(value);
  };

  return (
    <input
      type="text"
      value={localName}
      onChange={(e) => handleChange(e.target.value)}
      className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none"
      placeholder={placeholder}
    />
  );
};

export interface DeleteButtonProps {
  onDelete: () => void;
  title: string;
}

export const DeleteButton = ({ onDelete, title }: DeleteButtonProps) => {
  return (
    <button
      onClick={onDelete}
      className="p-1 rounded flex-shrink-0 text-schemafy-destructive hover:bg-red-100"
      title={title}
    >
      <Trash2 size={12} />
    </button>
  );
};

export interface ColumnItemProps {
  columnName: string;
  onRemove: () => void;
  additionalControls?: ReactNode;
}

export const ColumnItem = ({ columnName, onRemove, additionalControls }: ColumnItemProps) => {
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="text-schemafy-blue font-medium">{columnName}</span>
      {additionalControls}
      <button onClick={onRemove} className="p-0.5 rounded" title="Remove column">
        <X size={12} />
      </button>
    </div>
  );
};

export interface AddColumnSelectorProps {
  availableColumns: Array<{ id: string; name: string }>;
  onAddColumn: (columnId: string) => void;
}

export const AddColumnSelector = ({ availableColumns, onAddColumn }: AddColumnSelectorProps) => {
  if (availableColumns.length === 0) return null;

  return (
    <div className="flex items-center gap-2">
      <Select
        onValueChange={(value) => {
          if (value) {
            onAddColumn(value);
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
  );
};
