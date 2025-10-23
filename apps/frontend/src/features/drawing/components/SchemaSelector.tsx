import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { ulid } from 'ulid';
import { ErdStore } from '@/store/erd.store';
import { Button, ListItem } from '@/components';
import { ChevronDown, ChevronUp } from 'lucide-react';

export const SchemaSelector = observer(() => {
  const erdStore = ErdStore.getInstance();
  const [isAdding, setIsAdding] = useState(false);
  const [newSchemaName, setNewSchemaName] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);

  if (erdStore.erdState.state !== 'loaded') {
    return null;
  }

  const { database } = erdStore.erdState;
  const selectedSchemaId = erdStore.selectedSchemaId;
  const selectedSchemaName = database.schemas.find((schema) => schema.id === selectedSchemaId)?.name || '';

  const handleSchemaChange = (value: string) => {
    erdStore.selectSchema(value);
  };

  const handleAddSchema = () => {
    const trimmedName = newSchemaName.trim();
    const schemaId = ulid();
    const firstSchema = database.schemas[0];
    const defaultValues = {
      projectId: firstSchema.projectId,
      dbVendorId: firstSchema.dbVendorId,
      charset: firstSchema.charset,
      collation: firstSchema.collation,
      vendorOption: firstSchema.vendorOption,
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
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleAddSchema();
    } else if (e.key === 'Escape') {
      setIsAdding(false);
      setNewSchemaName('');
    }
  };

  return (
    <div className="flex flex-col items-center bg-schemafy-bg rounded-[10px] shadow-lg p-4 transition-all duration-300 ease-in-out">
      <div className="w-full flex justify-between">
        <label className="text-sm font-heading-base text-schemafy-text">
          {isExpanded ? 'Schemas' : `${selectedSchemaName}`}
        </label>
        {isExpanded ? (
          <ChevronUp
            size={16}
            color="var(--color-schemafy-dark-gray)"
            onClick={() => {
              setIsExpanded((prev) => !prev);
            }}
            cursor={'pointer'}
          />
        ) : (
          <ChevronDown
            size={16}
            color="var(--color-schemafy-dark-gray)"
            onClick={() => {
              setIsExpanded((prev) => !prev);
            }}
            cursor={'pointer'}
          />
        )}
      </div>
      <div
        className={`flex flex-col gap-2 transition-all duration-300 ${
          isExpanded ? 'mt-4 h-auto opacity-100' : 'h-0 opacity-0 overflow-hidden'
        }`}
      >
        {database.schemas.map((schema) => (
          <div
            onClick={() => {
              handleSchemaChange(schema.id);
              setIsExpanded(false);
            }}
          >
            <ListItem key={schema.id} name={schema.name} count={schema.tables.length} date={schema.updatedAt} />
          </div>
        ))}
        <div
          className={`flex transition-all duration-300 gap-1 ${
            isAdding ? 'h-auto opacity-100 w-full' : 'h-0 w-0 opacity-0 overflow-hidden'
          }`}
        >
          <input
            type="text"
            value={newSchemaName}
            onChange={(e) => {
              setNewSchemaName(e.target.value);
            }}
            onKeyDown={handleKeyDown}
            placeholder="Schema name"
            maxLength={20}
            className="p-3 font-body-xs w-full rounded-[8px] bg-schemafy-secondary text-schemafy-text focus:outline-none focus:ring-1 focus:ring-schemafy-primary"
            autoFocus
          />
        </div>
        {isAdding ? (
          <div className="flex w-full gap-2">
            <Button
              onClick={() => {
                handleAddSchema();
                setIsExpanded(false);
              }}
              disabled={newSchemaName.trim().length < 3 || newSchemaName.trim().length > 20}
              size={'dropdown'}
              fullWidth
            >
              Add
            </Button>
            <Button
              onClick={() => {
                setIsAdding(false);
                setNewSchemaName('');
              }}
              size={'dropdown'}
              fullWidth
            >
              Cancel
            </Button>
          </div>
        ) : (
          <Button onClick={() => setIsAdding(true)} fullWidth size={'dropdown'}>
            New Schema
          </Button>
        )}
      </div>
    </div>
  );
});
