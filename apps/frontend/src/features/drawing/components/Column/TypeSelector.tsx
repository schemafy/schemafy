import { useState, useMemo, useEffect, useRef, Fragment } from 'react';
import type { DatatypeParameter, VendorDatatype } from '@/features/vendor';
import type { TypeSelectorProps } from '../../types';
import {
  Select,
  SelectGroup,
  SelectContent,
  SelectItem,
  SelectLabel,
  SelectTrigger,
} from '@/components';
import {
  CATEGORY_LABELS,
  editDatatypeParameterDraft,
  parseTypeArguments,
  serializeDatatypeParameterValues,
} from './utils';
import type {
  DatatypeParameterInputState,
  DatatypeParameterValue,
} from './utils';

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

const parseDraftValues = (
  typeArguments: string,
): Record<string, DatatypeParameterValue> => ({
  ...parseTypeArguments(typeArguments),
});

export const TypeSelector = ({
  value,
  typeArguments,
  vendorTypes,
  disabled,
  onChange,
  onPendingChange,
}: TypeSelectorProps) => {
  const incomingValues = useMemo(
    () => parseDraftValues(typeArguments),
    [typeArguments],
  );
  const incomingSignature = useMemo(
    () => serializeDatatypeParameterValues(incomingValues),
    [incomingValues],
  );
  const [draftType, setDraftType] = useState(value);
  const [draftState, setDraftState] = useState<DatatypeParameterInputState>(
    () => ({
      values: parseDraftValues(typeArguments),
      invalidNames: new Set(),
    }),
  );
  const [hasLocalEdits, setHasLocalEdits] = useState(false);

  const displayType = draftType;
  const displayTypeConfig = vendorTypes.find((t) => t.sqlType === displayType);
  const params: DatatypeParameter[] = displayTypeConfig?.parameters ?? [];
  const sortedParams = [...params].sort((a, b) => a.order - b.order);
  const draftSignature = serializeDatatypeParameterValues(draftState.values);

  const pendingRef = useRef(false);
  const onPendingChangeRef = useRef(onPendingChange);
  onPendingChangeRef.current = onPendingChange;

  const notifyPendingChange = (isPending: boolean) => {
    pendingRef.current = isPending;
    onPendingChange?.(isPending);
  };

  useEffect(() => {
    if (hasLocalEdits) {
      // Ignore stale mutation snapshots until the server reflects the full draft.
      if (
        draftState.invalidNames.size === 0 &&
        value === draftType &&
        incomingSignature === draftSignature
      ) {
        setHasLocalEdits(false);
      }
      return;
    }
    setDraftType(value);
    setDraftState({ values: incomingValues, invalidNames: new Set() });
  }, [
    draftSignature,
    draftState.invalidNames.size,
    draftType,
    hasLocalEdits,
    incomingSignature,
    incomingValues,
    value,
  ]);

  useEffect(() => {
    return () => {
      if (pendingRef.current) onPendingChangeRef.current?.(false);
    };
  }, []);

  const grouped = useMemo(
    () => groupTypesByCategory(vendorTypes),
    [vendorTypes],
  );

  const handleTypeSelect = (newType: string) => {
    const newTypeConfig = vendorTypes.find((t) => t.sqlType === newType);
    const hasRequiredParams =
      newTypeConfig?.parameters.some((p) => p.required) ?? false;
    const nextState = { values: {}, invalidNames: new Set<string>() };

    setDraftType(newType);
    setDraftState(nextState);
    setHasLocalEdits(true);
    notifyPendingChange(hasRequiredParams);
    if (!hasRequiredParams) {
      onChange(newType, '{}');
    }
  };

  const handleParamBlur = (
    parameter: DatatypeParameter,
    paramValue: string,
  ) => {
    const updated = editDatatypeParameterDraft(
      draftState,
      params,
      parameter,
      paramValue,
    );
    setDraftState(updated.state);
    setHasLocalEdits(true);
    notifyPendingChange(updated.isPending);
    if (!updated.isPending) {
      onChange(
        draftType,
        serializeDatatypeParameterValues(updated.state.values),
      );
    }
  };

  return (
    <div className="flex items-center gap-1 text-xs font-mono">
      <Select
        onValueChange={handleTypeSelect}
        value={displayType}
        disabled={disabled}
      >
        <SelectTrigger
          className="schemafy-focus-ring w-auto min-w-[5.5rem] rounded-lg border border-schemafy-glass-border bg-schemafy-secondary/60 px-2.5 py-1.5 font-mono text-xs [&>span]:line-clamp-none"
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
                {sortedParams.map((param, i) => {
                  const isStringArray = param.valueType === 'string_array';
                  const defaultVal = draftState.values[param.name];
                  const displayVal = isStringArray
                    ? Array.isArray(defaultVal)
                      ? defaultVal.join(', ')
                      : ''
                    : (defaultVal ?? '');

                  return (
                    <Fragment key={param.name}>
                      {i > 0 && <span>,</span>}
                      <input
                        key={`${displayType}-${param.name}`}
                        type={isStringArray ? 'text' : 'number'}
                        min={
                          isStringArray
                            ? undefined
                            : (param.minValue ?? undefined)
                        }
                        max={
                          isStringArray
                            ? undefined
                            : (param.maxValue ?? undefined)
                        }
                        defaultValue={displayVal}
                        aria-label={`${displayType} ${param.label}`}
                        aria-invalid={draftState.invalidNames.has(param.name)}
                        data-testid={`datatype-parameter-${param.name}`}
                        placeholder={
                          isStringArray ? 'e.g. a, b, c' : param.label
                        }
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
                        onBlur={(e) => handleParamBlur(param, e.target.value)}
                        className={`${isStringArray ? 'w-28' : 'w-8'} border-b ${draftState.invalidNames.has(param.name) ? 'border-schemafy-destructive' : 'border-schemafy-dark-gray'} bg-transparent text-center focus:border-schemafy-soft-blue focus:outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none`}
                      />
                    </Fragment>
                  );
                })}
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
