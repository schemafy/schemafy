import { ListItem } from '@/components';
import { SchemaInput } from './SchemaInput';

interface SchemaListItemProps {
  schema: {
    id: string;
    name: string;
    tables: unknown[];
    updatedAt: Date;
  };
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
    <div onClick={() => onSelect(schema.id)} className="cursor-pointer">
      <ListItem
        name={schema.name}
        count={schema.tables.length}
        date={schema.updatedAt}
        onChange={() => onStartEdit(schema.id, schema.name)}
        onDelete={() => onDelete(schema.id)}
      />
    </div>
  );
};
