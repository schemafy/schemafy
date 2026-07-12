import { Edit, Trash } from 'lucide-react';
import { SchemaInput } from './SchemaInput';

interface SchemaListItemProps {
  schema: {
    id: string;
    name: string;
  };
  tableCount?: number;
  isEditing: boolean;
  editingName: string;
  onSelect: (schemaId: string) => void;
  onStartEdit: (schemaId: string, name: string) => void;
  onSaveEdit: (schemaId: string) => void;
  onCancelEdit: () => void;
  onDelete: (schemaId: string) => void;
  onEditingNameChange: (name: string) => void;
}

export const SchemaListItem = ({
  schema,
  tableCount = 0,
  isEditing,
  editingName,
  onSelect,
  onStartEdit,
  onSaveEdit,
  onCancelEdit,
  onDelete,
  onEditingNameChange,
}: SchemaListItemProps) => {
  if (isEditing) {
    return (
      <SchemaInput
        value={editingName}
        onChange={onEditingNameChange}
        onSave={() => onSaveEdit(schema.id)}
        onCancel={onCancelEdit}
      />
    );
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => onSelect(schema.id)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onSelect(schema.id);
        }
      }}
      className="schemafy-focus-ring flex h-11 w-full items-center gap-2.5 rounded-xl border border-schemafy-glass-border bg-schemafy-secondary/35 px-3.5 text-left transition-colors hover:bg-schemafy-secondary/60"
    >
      <span className="min-w-0 flex-1 truncate font-overline-sm text-schemafy-text">
        {schema.name}
      </span>
      <span className="flex shrink-0 items-center gap-1.5">
        <button
          type="button"
          title="Edit"
          onClick={(event) => {
            event.stopPropagation();
            onStartEdit(schema.id, schema.name);
          }}
          className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center"
        >
          <Edit size={12} />
        </button>
        <button
          type="button"
          title="Delete"
          onClick={(event) => {
            event.stopPropagation();
            onDelete(schema.id);
          }}
          className="schemafy-icon-button schemafy-focus-ring flex h-7 w-7 items-center justify-center hover:text-schemafy-destructive"
        >
          <Trash size={12} />
        </button>
      </span>
      <span className="schemafy-badge shrink-0 px-2.5 py-0.5 font-body-xs">
        {tableCount} entities
      </span>
    </div>
  );
};
