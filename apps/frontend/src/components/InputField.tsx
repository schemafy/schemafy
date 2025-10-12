import { cn } from '@/lib';
import { memo, useId } from 'react';

interface InputFieldProps {
  label: string;
  name: string;
  value: string | number;
  type?: 'text' | 'email' | 'password' | 'number';
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  error?: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
}

export const InputField = memo(
  ({ label, type, name, value, placeholder, required, disabled, error, onChange, onBlur }: InputFieldProps) => {
    const inputId = useId();

    return (
      <div className="flex flex-col flex-grow gap-1 py-3 px-4">
        <label htmlFor={inputId} className="mb-1 font-overline-md">
          {label} {required && '*'}
        </label>
        <input
          id={inputId}
          type={type}
          name={name}
          placeholder={placeholder}
          value={value}
          required={required}
          disabled={disabled}
          onChange={onChange}
          onBlur={onBlur}
          className={cn(
            'border border-schemafy-light-gray placeholder-schemafy-dark-gray font-body-md rounded-[12px] p-4',
            disabled && 'text-schemafy-dark-gray bg-schemafy-secondary',
          )}
        />
        {error && (
          <p className="text-schemafy-destructive font-caption-md" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  },
);
