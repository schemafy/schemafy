import { Button } from '@/components';
import { SCHEMA_NAME_CONSTRAINTS } from '@/types';
import { validateSchemaName } from '../utils/validateSchemaName';

interface SchemaInputProps {
  value: string;
  onChange: (value: string) => void;
  onSave: () => void;
  onCancel: () => void;
  placeholder?: string;
  saveLabel?: string;
  cancelLabel?: string;
}

export const SchemaInput = ({
  value,
  onChange,
  onSave,
  onCancel,
  placeholder = 'Schema name',
  saveLabel = 'Save',
  cancelLabel = 'Cancel',
}: SchemaInputProps) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      if (validateSchemaName(value)) {
        onSave();
      }
    } else if (e.key === 'Escape') {
      onCancel();
    }
  };

  const isValid = validateSchemaName(value);

  return (
    <div className="flex flex-col gap-2" onClick={(e) => e.stopPropagation()}>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        maxLength={SCHEMA_NAME_CONSTRAINTS.MAX_LENGTH}
        className="p-3 font-body-xs w-full rounded-[8px] bg-schemafy-secondary text-schemafy-text focus:outline-none focus:ring-1 focus:ring-schemafy-primary"
        autoFocus
      />
      <div className="flex w-full gap-2">
        <Button onClick={onSave} disabled={!isValid} size="dropdown" fullWidth>
          {saveLabel}
        </Button>
        <Button onClick={onCancel} size="dropdown" fullWidth>
          {cancelLabel}
        </Button>
      </div>
    </div>
  );
};
