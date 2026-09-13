import { useState, useEffect, type ReactNode } from 'react';
import { Trash2, X } from 'lucide-react';
import {
  Select,
  SelectGroup,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components';

export interface BaseItem {
  id: string;
  name: string;
  columns: Array<{ id: string; columnId: string; seqNo: number }>;
}

export interface EditableRowBaseProps<T extends BaseItem> {
  item: T;
  tableColumns: Array<{ id: string; name: string }>;
  isEditMode: boolean;
  renderViewMode: (
    item: T,
    tableColumns: Array<{ id: string; name: string }>,
  ) => ReactNode;
  renderEditMode: (
    item: T,
    tableColumns: Array<{ id: string; name: string }>,
  ) => ReactNode;
}

export function EditableRowBase<T extends BaseItem>({
  item,
  tableColumns,
  isEditMode,
  renderViewMode,
  renderEditMode,
}: EditableRowBaseProps<T>) {
  return (
    <div className="border-b border-schemafy-glass-border/60 last:border-b-0">
      {isEditMode
        ? renderEditMode(item, tableColumns)
        : renderViewMode(item, tableColumns)}
    </div>
  );
}

interface EditableNameInputProps {
  name: string;
  placeholder: string;
  onNameChange: (newName: string) => void;
}

export const EditableNameInput = ({
  name,
  placeholder,
  onNameChange,
}: EditableNameInputProps) => {
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
      className="schemafy-focus-ring flex-1 rounded-lg border border-schemafy-glass-border bg-schemafy-secondary/60 px-2.5 py-1.5 text-sm text-schemafy-text placeholder:text-schemafy-dark-gray"
      placeholder={placeholder}
    />
  );
};

interface DeleteButtonProps {
  onDelete: () => void;
  title: string;
}

export const DeleteButton = ({ onDelete, title }: DeleteButtonProps) => {
  return (
    <button
      type="button"
      onClick={onDelete}
      className="schemafy-focus-ring flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg text-schemafy-destructive transition-colors hover:bg-schemafy-destructive/10"
      title={title}
    >
      <Trash2 size={12} />
    </button>
  );
};

interface ColumnItemProps {
  columnName: string;
  onRemove: () => void;
  additionalControls?: ReactNode;
}

export const ColumnItem = ({
  columnName,
  onRemove,
  additionalControls,
}: ColumnItemProps) => {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg border border-schemafy-glass-border/70 bg-schemafy-secondary/45 px-2.5 py-1.5 text-xs">
      <span className="font-medium text-schemafy-blue">{columnName}</span>
      {additionalControls}
      <button
        type="button"
        onClick={onRemove}
        className="schemafy-focus-ring flex h-6 w-6 items-center justify-center rounded-md text-schemafy-dark-gray transition-colors hover:bg-schemafy-destructive/10 hover:text-schemafy-destructive"
        title="Remove column"
      >
        <X size={12} />
      </button>
    </div>
  );
};

interface AddColumnSelectorProps {
  availableColumns: Array<{ id: string; name: string }>;
  onAddColumn: (columnId: string) => void;
}

export const AddColumnSelector = ({
  availableColumns,
  onAddColumn,
}: AddColumnSelectorProps) => {
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
        <SelectTrigger className="schemafy-focus-ring w-[8.5rem] rounded-lg border border-schemafy-glass-border bg-schemafy-secondary/60 px-2 py-1.5 font-mono text-xs">
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
