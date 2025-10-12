import { Handle, Position } from '@xyflow/react';
import { useState } from 'react';
import { HANDLE_STYLE, type TableNodeProps, type FieldType } from '../types';
import { Edit, Check, Settings, Plus } from 'lucide-react';
import { FieldRow } from './Field';
import { useDragAndDrop } from '../hooks';

export const TableNode = ({ data, id }: TableNodeProps) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [isFieldEditMode, setIsFieldEditMode] = useState(false);
  const [editingTableName, setEditingTableName] = useState(data.tableName);
  const [fields, setFields] = useState(data.fields || []);

  const dragAndDrop = useDragAndDrop({
    items: fields,
    onReorder: (newFields) => {
      setFields(newFields);
      updateFields(newFields);
    },
  });

  const saveTableName = () => {
    if (data.updateNode) {
      data.updateNode(id, { tableName: editingTableName });
    }
    setIsEditingTableName(false);
  };

  const addField = () => {
    const newField: FieldType = {
      id: `field_${Date.now()}`,
      name: 'new_field',
      type: 'VARCHAR',
      isPrimaryKey: false,
      isNotNull: false,
      isUnique: false,
    };
    const newFields = [...fields, newField];
    setFields(newFields);
    updateFields(newFields);
    setIsFieldEditMode(true);
  };

  const removeField = (fieldId: string) => {
    const newFields = fields.filter((field) => field.id !== fieldId);
    setFields(newFields);
    updateFields(newFields);
  };

  const updateField = (fieldId: string, key: keyof FieldType, value: string | boolean) => {
    const newFields = fields.map((field) => (field.id === fieldId ? { ...field, [key]: value } : field));
    setFields(newFields);
    updateFields(newFields);
  };

  const updateFields = (newFields: FieldType[]) => {
    if (data.updateNode) {
      data.updateNode(id, { fields: newFields });
    }
  };

  return (
    <div className="group bg-schemafy-bg border-2 border-schemafy-button-bg rounded-lg shadow-md min-w-48 overflow-hidden">
      <ConnectionHandles nodeId={id} />
      <TableHeader
        tableName={data.tableName}
        isEditing={isEditingTableName}
        editingName={editingTableName}
        isFieldEditMode={isFieldEditMode}
        onStartEdit={() => setIsEditingTableName(true)}
        onSaveEdit={saveTableName}
        onCancelEdit={() => setIsEditingTableName(false)}
        onEditingNameChange={setEditingTableName}
        onToggleFieldEditMode={() => setIsFieldEditMode(!isFieldEditMode)}
        onAddField={addField}
      />
      <div className="max-h-96 overflow-y-auto">
        {fields.map((field) => (
          <FieldRow
            key={field.id}
            field={field}
            isEditMode={isFieldEditMode}
            draggedItem={dragAndDrop.draggedItem}
            dragOverItem={dragAndDrop.dragOverItem}
            onDragStart={dragAndDrop.handleDragStart}
            onDragOver={dragAndDrop.handleDragOver}
            onDragLeave={dragAndDrop.handleDragLeave}
            onDrop={dragAndDrop.handleDrop}
            onDragEnd={dragAndDrop.handleDragEnd}
            onUpdateField={updateField}
            onRemoveField={removeField}
          />
        ))}
      </div>
      {fields.length === 0 && (
        <div className="p-4 text-center text-schemafy-dark-gray text-sm">Click + to add a field.</div>
      )}
    </div>
  );
};

const TableHeader = ({
  tableName,
  isEditing,
  editingName,
  isFieldEditMode,
  onStartEdit,
  onSaveEdit,
  onCancelEdit,
  onEditingNameChange,
  onToggleFieldEditMode,
  onAddField,
}: {
  tableName: string;
  isEditing: boolean;
  editingName: string;
  isFieldEditMode: boolean;
  onStartEdit: () => void;
  onSaveEdit: () => void;
  onCancelEdit: () => void;
  onEditingNameChange: (name: string) => void;
  onToggleFieldEditMode: () => void;
  onAddField: () => void;
}) => {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') onSaveEdit();
    if (e.key === 'Escape') onCancelEdit();
  };

  return (
    <div className="bg-schemafy-button-bg text-schemafy-button-text p-3 flex items-center justify-between">
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
            <button onClick={onSaveEdit} className="p-1 hover:bg-schemafy-dark-gray rounded">
              <Check size={14} />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2 flex-1">
            <span className="font-medium">{tableName}</span>
            <button onClick={onStartEdit} className="p-1 hover:bg-schemafy-dark-gray rounded">
              <Edit size={14} />
            </button>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={onToggleFieldEditMode}
          className={`p-1 rounded ${isFieldEditMode ? 'bg-schemafy-dark-gray' : 'hover:bg-schemafy-dark-gray'}`}
          title="Toggle Edit Mode"
        >
          <Settings size={14} />
        </button>
        <button onClick={onAddField} className="p-1 hover:bg-schemafy-dark-gray rounded" title="Add Field">
          <Plus size={14} />
        </button>
      </div>
    </div>
  );
};

const ConnectionHandles = ({ nodeId }: { nodeId: string }) => {
  const handles = [
    {
      position: Position.Top,
      id: `${nodeId}-top-handle`,
    },
    {
      position: Position.Bottom,
      id: `${nodeId}-bottom-handle`,
    },
    {
      position: Position.Left,
      id: `${nodeId}-left-handle`,
    },
    {
      position: Position.Right,
      id: `${nodeId}-right-handle`,
    },
  ];

  return (
    <>
      {handles.map(({ position, id }) => (
        <Handle
          key={id}
          type={'source'}
          position={position}
          id={id}
          style={HANDLE_STYLE}
          className="group-hover:!opacity-100"
        />
      ))}
    </>
  );
};
