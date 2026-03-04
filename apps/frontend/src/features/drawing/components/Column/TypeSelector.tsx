import { useState, useMemo, useEffect, Fragment } from 'react';
import type { TypeSelectorProps } from '../../types';
import type { VendorDatatype, DatatypeParameter } from '../../api';
import {
  Select,
  SelectGroup,
  SelectContent,
  SelectItem,
  SelectLabel,
  SelectTrigger,
} from '@/components';
import { parseLengthScale, CATEGORY_LABELS } from './utils';

const getCategoryGroup = (category: string): string => {
  const prefix = category.split('_')[0];
  return prefix;
};

const groupTypesByCategory = (
  types: VendorDatatype[],
): Map<string, VendorDatatype[]> => {
  const groups = new Map<string, VendorDatatype[]>();
  for (const type of types) {
    const group = getCategoryGroup(type.category);
    if (!groups.has(group)) {
      groups.set(group, []);
    }
    groups.get(group)!.push(type);
  }
  return groups;
};

export const TypeSelector = ({
  value,
  lengthScale,
  vendorTypes,
  disabled,
  onChange,
  onPendingChange,
}: TypeSelectorProps) => {
  const parsed = parseLengthScale(lengthScale);
  const [pendingType, setPendingType] = useState<string | null>(null);
  const [pendingParams, setPendingParams] = useState<
    Record<string, number | null>
  >({});

  const displayType = pendingType ?? value;
  const displayTypeConfig = vendorTypes.find((t) => t.sqlType === displayType);
  const params: DatatypeParameter[] = displayTypeConfig?.parameters ?? [];
  const displayParams = pendingType ? pendingParams : parsed;

  useEffect(() => {
    return () => {
      if (pendingType) onPendingChange?.(false);
    };
  }, [pendingType, onPendingChange]);

  const grouped = useMemo(
    () => groupTypesByCategory(vendorTypes),
    [vendorTypes],
  );

  const handleTypeSelect = (newType: string) => {
    const newTypeConfig = vendorTypes.find((t) => t.sqlType === newType);
    const hasRequiredParams =
      newTypeConfig?.parameters.some((p) => p.required) ?? false;

    if (!hasRequiredParams) {
      setPendingType(null);
      setPendingParams({});
      onPendingChange?.(false);
      onChange(newType, '{}');
    } else {
      setPendingType(newType);
      setPendingParams({});
      onPendingChange?.(true);
    }
  };

  const handleParamBlur = (paramName: string, paramValue: string) => {
    const trimmed = paramValue.trim();
    const num = Number(trimmed);
    const paramVal = trimmed && !isNaN(num) ? num : null;

    if (pendingType) {
      const updated = { ...pendingParams, [paramName]: paramVal };
      setPendingParams(updated);

      const allRequiredFilled = params
        .filter((p) => p.required)
        .every((p) => updated[p.name] != null);

      if (allRequiredFilled) {
        setPendingType(null);
        setPendingParams({});
        onPendingChange?.(false);
        onChange(pendingType, JSON.stringify(updated));
      }
    } else {
      const updated = { ...parsed, [paramName]: paramVal };
      onChange(value, JSON.stringify(updated));
    }
  };

  return (
    <div className="flex items-center gap-0.5 text-xs font-mono">
      <Select
        onValueChange={handleTypeSelect}
        value={displayType}
        disabled={disabled}
      >
        <SelectTrigger
          className="text-xs font-mono px-2 py-1 border border-schemafy-light-gray rounded focus:outline-none w-auto min-w-[5rem] [&>span]:line-clamp-none"
          title={
            disabled
              ? 'Cannot change the type of a foreign key column'
              : undefined
          }
        >
          <span className="flex items-center gap-0.5 whitespace-nowrap">
            <span>{displayType || 'Type'}</span>
            {params.length > 0 && (
              <>
                <span>(</span>
                {[...params]
                  .sort((a, b) => a.order - b.order)
                  .map((param, i) => (
                    <Fragment key={param.name}>
                      {i > 0 && <span>,</span>}
                      <input
                        key={`${displayType}-${param.name}`}
                        type="number"
                        defaultValue={displayParams[param.name] ?? ''}
                        placeholder={param.label}
                        onPointerDown={(e) => e.stopPropagation()}
                        onClick={(e) => {
                          e.stopPropagation();
                          e.preventDefault();
                        }}
                        onMouseDown={(e) => e.stopPropagation()}
                        onKeyDown={(e) => {
                          e.stopPropagation();
                          if (e.key === 'Enter') e.currentTarget.blur();
                        }}
                        onBlur={(e) =>
                          handleParamBlur(param.name, e.target.value)
                        }
                        className="w-8 text-center bg-transparent border-b border-schemafy-dark-gray focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                      />
                    </Fragment>
                  ))}
                <span>)</span>
              </>
            )}
          </span>
        </SelectTrigger>
        <SelectContent className="max-h-60">
          {[...grouped.entries()].map(([group, types], i) => (
            <SelectGroup
              key={group}
              className={
                i > 0 ? 'border-t border-schemafy-light-gray mt-1 pt-1' : ''
              }
            >
              <SelectLabel>{CATEGORY_LABELS[group] ?? group}</SelectLabel>
              {types.map((type) => (
                <SelectItem key={type.sqlType} value={type.sqlType}>
                  {type.displayName}
                </SelectItem>
              ))}
            </SelectGroup>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
};
