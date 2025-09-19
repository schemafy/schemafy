import { Handle, Position } from '@xyflow/react';
import { useState } from 'react';
import { DATA_TYPES, type TableNodeData, type FieldType } from '../types';
import {
  Plus,
  Trash2,
  GripVertical,
  Edit,
  Check,
  Settings,
} from 'lucide-react';

export const TableNode = ({
  data,
  id,
}: {
  data: TableNodeData;
  id: string;
}) => {
  const [isEditingTableName, setIsEditingTableName] = useState(false);
  const [isFieldEditMode, setIsFieldEditMode] = useState(false);
  const [editingTableName, setEditingTableName] = useState(data.tableName);
  const [fields, setFields] = useState(data.fields || []);
  const [draggedItem, setDraggedItem] = useState<string | null>(null);
  const [dragOverItem, setDragOverItem] = useState<string | null>(null);

  const saveTableName = () => {
    if (data.updateNode) {
      data.updateNode(id, { tableName: editingTableName });
    }
    setIsEditingTableName(false);
  };

  const addField = () => {
    const newField = {
      id: `field_${Date.now()}`,
      name: `new_field`,
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

  const updateField = (
    fieldId: string,
    key: keyof FieldType,
    value: string | boolean,
  ) => {
    const newFields = fields.map((field) =>
      field.id === fieldId ? { ...field, [key]: value } : field,
    );
    setFields(newFields);
    updateFields(newFields);
  };

  const updateFields = (newFields: FieldType[]) => {
    if (data.updateNode) {
      data.updateNode(id, { fields: newFields });
    }
  };

  const handleDragStart = (e: React.DragEvent, fieldId: string) => {
    console.log('drag:', fieldId);
    e.stopPropagation();
    setDraggedItem(fieldId);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', fieldId);
  };

  const handleDragOver = (e: React.DragEvent, fieldId: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverItem(fieldId);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOverItem(null);
  };

  const handleDrop = (e: React.DragEvent, dropTargetId: string) => {
    e.preventDefault();

    if (!draggedItem || draggedItem === dropTargetId) {
      setDraggedItem(null);
      setDragOverItem(null);
      return;
    }

    const draggedIndex = fields.findIndex((field) => field.id === draggedItem);
    const targetIndex = fields.findIndex((field) => field.id === dropTargetId);

    if (draggedIndex !== -1 && targetIndex !== -1) {
      const newFields = [...fields];
      const [draggedField] = newFields.splice(draggedIndex, 1);
      newFields.splice(targetIndex, 0, draggedField);

      setFields(newFields);
      updateFields(newFields);
    }

    setDraggedItem(null);
    setDragOverItem(null);
  };

  const handleDragEnd = () => {
    setDraggedItem(null);
    setDragOverItem(null);
  };

  return (
    <div className="bg-schemafy-bg border-2 border-schemafy-button-bg rounded-lg shadow-md min-w-48 overflow-hidden">
      <Handle
        type="target"
        position={Position.Top}
        id={`${id}-top-handle`}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="source"
        position={Position.Bottom}
        id={`${id}-bottom-handle`}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="target"
        position={Position.Left}
        id={`${id}-left-handle`}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="source"
        position={Position.Right}
        id={`${id}-right-handle`}
        style={{ background: '#141414', width: 10, height: 10 }}
      />

      <div className="bg-schemafy-button-bg text-schemafy-button-text p-3 flex items-center justify-between">
        <div className="flex items-center gap-2 flex-1">
          {isEditingTableName ? (
            <div className="flex items-center gap-2 flex-1">
              <input
                type="text"
                value={editingTableName}
                placeholder="Entity"
                onChange={(e) => setEditingTableName(e.target.value)}
                className="bg-transparent border-b border-schemafy-button-text text-schemafy-button-text placeholder-schemafy-dark-gray outline-none flex-1"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') saveTableName();
                  if (e.key === 'Escape') setIsEditingTableName(false);
                }}
                autoFocus
              />
              <button
                onClick={saveTableName}
                className="p-1 hover:bg-schemafy-dark-gray rounded"
              >
                <Check size={14} />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2 flex-1">
              <span className="font-medium">{data.tableName}</span>
              <button
                onClick={() => setIsEditingTableName(true)}
                className="p-1 hover:bg-schemafy-dark-gray rounded"
              >
                <Edit size={14} />
              </button>
            </div>
          )}
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={() => setIsFieldEditMode(!isFieldEditMode)}
            className={`p-1 rounded ${isFieldEditMode ? 'bg-schemafy-dark-gray' : 'hover:schemafy-dark-gray'}`}
            title="Toggle Edit Mode"
          >
            <Settings size={14} />
          </button>
          <button
            onClick={addField}
            className="p-1 hover:schemafy-dark-gray rounded"
            title="Add Field"
          >
            <Plus size={14} />
          </button>
        </div>
      </div>

      <div className="max-h-96 overflow-y-auto">
        {fields.map((field) => (
          <div
            key={field.id}
            className={`border-b border-schemafy-light-gray last:border-b-0 transition-colors duration-200 ${
              isFieldEditMode ? 'hover:bg-schemafy-secondary' : ''
            } ${draggedItem === field.id ? 'opacity-50' : ''} ${
              dragOverItem === field.id ? 'bg-blue-50 border-blue-200' : ''
            }`}
            onDragOver={(e) => handleDragOver(e, field.id)}
            onDragLeave={handleDragLeave}
            onDrop={(e) => handleDrop(e, field.id)}
          >
            {isFieldEditMode ? (
              <div className="p-2 space-y-2">
                <div className="flex items-center gap-2">
                  <span
                    draggable={true}
                    onDragStart={(e) => handleDragStart(e, field.id)}
                    onDragEnd={handleDragEnd}
                    className="cursor-move p-1 hover:bg-schemafy-light-gray rounded transition-colors nodrag"
                    title="Drag to reorder"
                    onMouseDown={(e) => e.stopPropagation()}
                  >
                    <GripVertical
                      size={12}
                      className="text-schemafy-dark-gray"
                    />
                  </span>

                  <input
                    type="text"
                    value={field.name}
                    onChange={(e) =>
                      updateField(field.id, 'name', e.target.value)
                    }
                    className="flex-1 px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                    placeholder="Field name"
                  />

                  <select
                    value={field.type}
                    onChange={(e) =>
                      updateField(field.id, 'type', e.target.value)
                    }
                    className="px-2 py-1 text-sm border border-schemafy-light-gray rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                  >
                    {DATA_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>

                  <button
                    onClick={() => removeField(field.id)}
                    className="p-1 text-schemafy-destructive hover:bg-red-100 rounded flex-shrink-0"
                    title="Remove Field"
                  >
                    <Trash2 size={12} />
                  </button>
                </div>

                <div className="flex flex-wrap gap-3 text-xs ml-4">
                  <label className="flex items-center gap-1 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={field.isPrimaryKey}
                      onChange={(e) =>
                        updateField(field.id, 'isPrimaryKey', e.target.checked)
                      }
                      className="w-3 h-3"
                    />
                    <span className="text-yellow-600 font-medium">PK</span>
                  </label>

                  <label className="flex items-center gap-1 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={field.isNotNull}
                      onChange={(e) =>
                        updateField(field.id, 'isNotNull', e.target.checked)
                      }
                      className="w-3 h-3"
                    />
                    <span className="text-red-600 font-medium">NOT NULL</span>
                  </label>

                  <label className="flex items-center gap-1 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={field.isUnique}
                      onChange={(e) =>
                        updateField(field.id, 'isUnique', e.target.checked)
                      }
                      className="w-3 h-3"
                    />
                    <span className="text-blue-600 font-medium">UNIQUE</span>
                  </label>
                </div>
              </div>
            ) : (
              <div className="p-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-sm ${field.isPrimaryKey ? 'font-bold text-yellow-600' : 'text-schemafy-text'}`}
                    >
                      {field.name}
                    </span>
                    <span className="text-xs text-schemafy-dark-gray">
                      ({field.type})
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    {field.isPrimaryKey && (
                      <span className="text-xs text-yellow-600 font-medium">
                        PK
                      </span>
                    )}
                    {field.isNotNull && (
                      <span className="text-xs text-red-600 font-medium">
                        *
                      </span>
                    )}
                    {field.isUnique && (
                      <span className="text-xs text-blue-600 font-medium">
                        UQ
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {fields.length === 0 && (
        <div className="p-4 text-center text-schemafy-dark-gray text-sm">
          Click + to add a field.
        </div>
      )}
    </div>
  );
};
