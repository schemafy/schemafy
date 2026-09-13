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
  ({
    label,
    type,
    name,
    value,
    placeholder,
    required,
    disabled,
    error,
    onChange,
    onBlur,
  }: InputFieldProps) => {
    const inputId = useId();

    return (
      <div className="flex flex-grow flex-col gap-1.5 py-2.5">
        <label
          htmlFor={inputId}
          className="font-overline-xs text-schemafy-dark-gray"
        >
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
            'schemafy-input px-4 py-3.5 font-body-sm',
            disabled && 'bg-schemafy-secondary text-schemafy-dark-gray',
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
