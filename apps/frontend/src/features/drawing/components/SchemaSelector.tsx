import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { ulid } from 'ulid';
import { ErdStore } from '@/store/erd.store';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components';

export const SchemaSelector = observer(() => {
  const erdStore = ErdStore.getInstance();
  const [isAdding, setIsAdding] = useState(false);
  const [newSchemaName, setNewSchemaName] = useState('');
  const [error, setError] = useState<string | null>(null);

  if (erdStore.erdState.state !== 'loaded') {
    return null;
  }

  const { database } = erdStore.erdState;
  const selectedSchemaId = erdStore.selectedSchemaId;

  const handleSchemaChange = (value: string) => {
    erdStore.selectSchema(value);
  };

  const handleAddSchema = () => {
    const trimmedName = newSchemaName.trim();

    if (trimmedName.length < 3 || trimmedName.length > 20) {
      setError('Schema name must be between 3 and 20 characters');
      return;
    }

    try {
      const schemaId = ulid();

      const firstSchema = database.schemas[0];
      const defaultValues = firstSchema
        ? {
            projectId: firstSchema.projectId,
            dbVendorId: firstSchema.dbVendorId,
            charset: firstSchema.charset,
            collation: firstSchema.collation,
            vendorOption: firstSchema.vendorOption,
          }
        : {
            projectId: ulid(),
            dbVendorId: 'mysql' as const,
            charset: 'utf8mb4',
            collation: 'utf8mb4_unicode_ci',
            vendorOption: '',
          };

      const newSchema = {
        id: schemaId,
        name: trimmedName,
        tables: [],
        ...defaultValues,
      };

      erdStore.createSchema(newSchema);
      erdStore.selectSchema(schemaId);

      setNewSchemaName('');
      setIsAdding(false);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add schema');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleAddSchema();
    } else if (e.key === 'Escape') {
      setIsAdding(false);
      setNewSchemaName('');
      setError(null);
    }
  };

  return (
    <div className="flex items-center gap-2 bg-schemafy-bg px-4 py-2 rounded-lg shadow-md border border-schemafy-button-bg">
      <label className="text-sm font-medium text-schemafy-text">Schema:</label>
      <Select value={selectedSchemaId || ''} onValueChange={handleSchemaChange}>
        <SelectTrigger className="w-[180px] h-8 text-sm">
          <SelectValue placeholder="Select a schema" />
        </SelectTrigger>
        <SelectContent>
          {database.schemas.map((schema) => (
            <SelectItem key={schema.id} value={schema.id}>
              {schema.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isAdding ? (
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-1">
            <input
              type="text"
              value={newSchemaName}
              onChange={(e) => {
                setNewSchemaName(e.target.value);
                setError(null);
              }}
              onKeyDown={handleKeyDown}
              placeholder="Schema name (3-20 chars)"
              maxLength={20}
              className="px-2 py-1 text-sm border border-schemafy-light-gray rounded bg-schemafy-bg text-schemafy-text focus:outline-none focus:ring-1 focus:ring-schemafy-primary"
              autoFocus
            />
            <button
              onClick={handleAddSchema}
              disabled={newSchemaName.trim().length < 3 || newSchemaName.trim().length > 20}
              className="px-2 py-1 text-sm bg-schemafy-button-bg text-schemafy-button-text rounded hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Add
            </button>
            <button
              onClick={() => {
                setIsAdding(false);
                setNewSchemaName('');
                setError(null);
              }}
              className="px-2 py-1 text-sm bg-schemafy-button-bg text-schemafy-button-text rounded hover:opacity-80"
            >
              Cancel
            </button>
          </div>
          {error && <p className="text-xs text-schemafy-destructive">{error}</p>}
        </div>
      ) : (
        <button
          onClick={() => setIsAdding(true)}
          className="px-2 py-1 text-sm bg-schemafy-button-bg text-schemafy-button-text rounded hover:opacity-80"
        >
          + Add Schema
        </button>
      )}
    </div>
  );
});
