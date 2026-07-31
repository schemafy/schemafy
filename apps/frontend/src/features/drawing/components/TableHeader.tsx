import {
  Edit,
  Check,
  Settings,
  Plus,
  Trash,
  Loader2,
  Table2,
} from 'lucide-react';

interface TableHeaderProps {
  tableName: string;
  isEditing: boolean;
  editingName: string;
  isColumnEditMode: boolean;
  onStartEdit: () => void;
  onSaveEdit: () => void;
  onCancelEdit: () => void;
  onEditingNameChange: (name: string) => void;
  onToggleColumnEditMode: () => void;
  onSaveAllPendingChanges: () => void;
  isAddingColumn: boolean;
  onAddColumn: () => void;
  onDeleteTable: () => void;
}

export const TableHeader = ({
  tableName,
  isEditing,
  editingName,
  isColumnEditMode,
  onStartEdit,
  onSaveEdit,
  onCancelEdit,
  onEditingNameChange,
  onToggleColumnEditMode,
  onSaveAllPendingChanges,
  isAddingColumn,
  onAddColumn,
  onDeleteTable,
}: TableHeaderProps) => {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') onSaveEdit();
    if (e.key === 'Escape') onCancelEdit();
  };

  return (
    <div className="flex items-center justify-between gap-3 border-b border-schemafy-glass-border/65 bg-schemafy-table-header-bg px-3 py-2.5 text-schemafy-text">
      <div className="flex min-w-0 flex-1 items-center gap-2">
        {isEditing ? (
          <div className="flex flex-1 items-center gap-2">
            <input
              type="text"
              value={editingName}
              placeholder="Entity"
              onChange={(e) => onEditingNameChange(e.target.value)}
              className="schemafy-focus-ring flex-1 border-b border-schemafy-glass-border bg-transparent text-sm text-schemafy-text placeholder:text-schemafy-dark-gray"
              onKeyDown={handleKeyDown}
              autoFocus
            />
            <button
              type="button"
              onClick={onSaveEdit}
              className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
            >
              <Check size={14} />
            </button>
          </div>
        ) : (
          <div className="flex min-w-0 flex-1 items-center gap-2.5">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-schemafy-secondary text-schemafy-dark-gray">
              <Table2 size={13} />
            </span>
            <span className="min-w-0 max-w-44 truncate text-sm font-semibold">
              {tableName}
            </span>
            <button
              type="button"
              onClick={onStartEdit}
              className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
              title="Edit Table Name"
            >
              <Edit size={13} />
            </button>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1">
        {isColumnEditMode ? (
          <button
            type="button"
            onClick={onSaveAllPendingChanges}
            className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
            title="Save and Exit Edit Mode"
          >
            <Check size={14} />
          </button>
        ) : (
          <button
            type="button"
            onClick={onToggleColumnEditMode}
            className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
            title="Edit Columns"
          >
            <Settings size={13} />
          </button>
        )}
        <button
          type="button"
          onClick={onAddColumn}
          disabled={isAddingColumn}
          className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text disabled:opacity-50"
          title="Add Column"
        >
          {isAddingColumn ? (
            <Loader2 size={13} className="animate-spin" />
          ) : (
            <Plus size={13} />
          )}
        </button>
        <button
          type="button"
          onClick={onDeleteTable}
          className="schemafy-focus-ring flex h-7 w-7 items-center justify-center rounded-lg text-schemafy-dark-gray transition-colors hover:bg-schemafy-secondary hover:text-schemafy-text"
          title="Delete Table"
        >
          <Trash size={13} />
        </button>
      </div>
    </div>
  );
};
