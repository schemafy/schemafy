import * as React from 'react';
import { cn } from '@/lib/utils';

interface RadioGroupProps {
  value: string;
  onValueChange: (value: string) => void;
  children: React.ReactNode;
  className?: string;
  name?: string;
}

export const RadioGroup = ({ value, onValueChange, children, className, name = 'radio-group' }: RadioGroupProps) => {
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
        'flex items-center gap-2 rounded-sm py-1.5 pl-2 pr-2 text-schemafy-text font-body-xs cursor-pointer select-none outline-none transition-colors',
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
        className="w-2.5 h-2.5 accent-schemafy-text cursor-pointer pointer-events-none"
        tabIndex={-1}
      />
      {children}
    </div>
  );
};
