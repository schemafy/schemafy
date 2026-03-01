import { Edit, Check, Settings, Plus, Trash, Loader2 } from 'lucide-react';

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
    <div className="bg-schemafy-button-bg text-schemafy-button-text p-3 flex items-center justify-between gap-4">
      <div className="flex items-center gap-2 flex-1">
        {isEditing ? (
          <div className="flex items-center gap-2 flex-1">
            <input
              type="text"
              value={editingName}
              placeholder="Entity"
              onChange={(e) => onEditingNameChange(e.target.value)}
              className="bg-transparent border-b border-schemafy-button-text text-schemafy-button-text placeholder-schemafy-dark-gray outline-none flex-1"
              onKeyDown={handleKeyDown}
              autoFocus
            />
            <button
              onClick={onSaveEdit}
              className="p-1 hover:bg-schemafy-dark-gray rounded"
            >
              <Check size={14} />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2 flex-1">
            <span className="font-medium">{tableName}</span>
            <button
              onClick={onStartEdit}
              className="p-1 hover:bg-schemafy-dark-gray rounded"
              title="Edit Table Name"
            >
              <Edit size={14} />
            </button>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1">
        {isColumnEditMode ? (
          <button
            onClick={onSaveAllPendingChanges}
            className="p-1 hover:bg-schemafy-dark-gray rounded"
            title="Save and Exit Edit Mode"
          >
            <Check size={14} />
          </button>
        ) : (
          <button
            onClick={onToggleColumnEditMode}
            className="p-1 hover:bg-schemafy-dark-gray rounded"
            title="Edit Columns"
          >
            <Settings size={14} />
          </button>
        )}
        <button
          onClick={onAddColumn}
          disabled={isAddingColumn}
          className="p-1 hover:bg-schemafy-dark-gray rounded disabled:opacity-50"
          title="Add Column"
        >
          {isAddingColumn ? (
            <Loader2 size={14} className="animate-spin" />
          ) : (
            <Plus size={14} />
          )}
        </button>
        <button
          onClick={onDeleteTable}
          className="p-1 hover:bg-schemafy-dark-gray rounded"
          title="Delete Table"
        >
          <Trash size={14} />
        </button>
      </div>
    </div>
  );
};
