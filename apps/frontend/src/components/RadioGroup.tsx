import * as React from 'react';
import { cn } from '@/lib/utils';

interface RadioGroupProps {
  value: string;
  onValueChange: (value: string) => void;
  children: React.ReactNode;
  className?: string;
  name?: string;
}

export const RadioGroup = ({
  value,
  onValueChange,
  children,
  className,
  name = 'radio-group',
}: RadioGroupProps) => {
  return (
    <div className={cn('flex flex-col gap-1', className)} role="radiogroup">
      {React.Children.map(children, (child) => {
        if (React.isValidElement<RadioGroupItemProps>(child)) {
          return React.cloneElement(child, {
            selectedValue: value,
            onSelect: onValueChange,
            name,
          });
        }
        return child;
      })}
    </div>
  );
};

interface RadioGroupItemProps {
  value: string;
  children: React.ReactNode;
  className?: string;
  disabled?: boolean;
  selectedValue?: string;
  onSelect?: (value: string) => void;
  name?: string;
}

export const RadioGroupItem = ({
  value,
  children,
  className,
  disabled = false,
  selectedValue,
  onSelect,
  name,
}: RadioGroupItemProps) => {
  const isChecked = selectedValue === value;

  const handleClick = () => {
    if (!disabled && onSelect) {
      onSelect(value);
    }
  };

  return (
    <div
      className={cn(
        'schemafy-focus-ring flex cursor-pointer select-none items-center gap-2 rounded-xl py-2 pl-2.5 pr-3 font-body-xs text-schemafy-text outline-none transition-colors hover:bg-schemafy-secondary',
        isChecked && 'bg-schemafy-secondary text-schemafy-text',
        disabled && 'opacity-50 pointer-events-none cursor-not-allowed',
        className,
      )}
      onClick={handleClick}
      role="radio"
      aria-checked={isChecked}
      aria-disabled={disabled}
      tabIndex={disabled ? -1 : 0}
    >
      <input
        type="radio"
        name={name}
        value={value}
        checked={isChecked}
        onChange={() => {}}
        disabled={disabled}
        className="h-2.5 w-2.5 cursor-pointer accent-schemafy-soft-blue pointer-events-none"
        tabIndex={-1}
      />
      {children}
    </div>
  );
};
