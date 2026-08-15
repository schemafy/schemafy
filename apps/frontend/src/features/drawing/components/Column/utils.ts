import type { ColumnTypeArguments } from '../../api/types';
import type { DatatypeParameter } from '@/features/vendor/api/types';

export type DatatypeParameterValue = number | string[] | null;

export type DatatypeParameterInputState = {
  values: Record<string, DatatypeParameterValue>;
  invalidNames: Set<string>;
};

export const applyDatatypeParameterInput = (
  state: DatatypeParameterInputState,
  parameter: DatatypeParameter,
  rawValue: string,
): DatatypeParameterInputState => {
  const parsed = parseDatatypeParameterInput(parameter, rawValue);
  const invalidNames = new Set(state.invalidNames);
  if (!parsed.valid) {
    invalidNames.add(parameter.name);
    return { values: state.values, invalidNames };
  }

  invalidNames.delete(parameter.name);
  const values = { ...state.values };
  if (parsed.value == null) {
    delete values[parameter.name];
  } else {
    values[parameter.name] = parsed.value;
  }
  return { values, invalidNames };
};

export const hasCompleteRequiredParameters = (
  parameters: DatatypeParameter[],
  state: DatatypeParameterInputState,
): boolean =>
  parameters
    .filter((parameter) => parameter.required)
    .every(
      (parameter) =>
        !state.invalidNames.has(parameter.name) &&
        state.values[parameter.name] != null,
    );

const hasValidParameterRelationships = (
  values: Record<string, DatatypeParameterValue>,
): boolean => {
  const precision = values.precision;
  const scale = values.scale;
  if (typeof scale !== 'number') return true;
  return typeof precision === 'number' && scale <= precision;
};

export const editDatatypeParameterDraft = (
  state: DatatypeParameterInputState,
  parameters: DatatypeParameter[],
  parameter: DatatypeParameter,
  rawValue: string,
): { state: DatatypeParameterInputState; isPending: boolean } => {
  const updated = applyDatatypeParameterInput(state, parameter, rawValue);
  return {
    state: updated,
    isPending:
      updated.invalidNames.size > 0 ||
      !hasCompleteRequiredParameters(parameters, updated) ||
      !hasValidParameterRelationships(updated.values),
  };
};

export const serializeDatatypeParameterValues = (
  values: Record<string, DatatypeParameterValue>,
): string =>
  JSON.stringify(
    Object.fromEntries(
      Object.entries(values).filter(([, value]) => value != null),
    ),
  );

const parseDatatypeParameterInput = (
  parameter: DatatypeParameter,
  rawValue: string,
): { valid: true; value: DatatypeParameterValue } | { valid: false } => {
  const trimmed = rawValue.trim();
  if (!trimmed) {
    return parameter.required ? { valid: false } : { valid: true, value: null };
  }

  if (parameter.valueType === 'integer') {
    const value = Number(trimmed);
    if (
      !Number.isInteger(value) ||
      (parameter.minValue != null && value < parameter.minValue) ||
      (parameter.maxValue != null && value > parameter.maxValue)
    ) {
      return { valid: false };
    }
    return { valid: true, value };
  }

  const values = trimmed
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean);
  if (
    (parameter.minItems != null && values.length < parameter.minItems) ||
    (parameter.maxItems != null && values.length > parameter.maxItems) ||
    new Set(values).size !== values.length ||
    values.some((value) => {
      const length = Array.from(value).length;
      return (
        (parameter.minItemLength != null && length < parameter.minItemLength) ||
        (parameter.maxItemLength != null && length > parameter.maxItemLength)
      );
    })
  ) {
    return { valid: false };
  }
  return { valid: true, value: values };
};

export const parseTypeArguments = (
  typeArguments: string,
): ColumnTypeArguments => {
  try {
    const parsed = JSON.parse(typeArguments || '{}');
    return {
      length: parsed?.length ?? null,
      precision: parsed?.precision ?? null,
      scale: parsed?.scale ?? null,
      values: parsed?.values ?? null,
    };
  } catch {
    return { length: null, precision: null, scale: null, values: null };
  }
};

export const formatTypeDisplay = (
  type: string,
  typeArguments: string,
): string => {
  const { length, precision, scale, values } =
    parseTypeArguments(typeArguments);

  if (values && values.length > 0) {
    return `${type}(${values.join(',')})`;
  }
  if (precision != null) {
    return scale != null
      ? `${type}(${precision},${scale})`
      : `${type}(${precision})`;
  }
  if (length != null) {
    return `${type}(${length})`;
  }
  return type;
};

export const CATEGORY_LABELS: Record<string, string> = {
  numeric: 'Numeric',
  datetime: 'Date & Time',
  string: 'String',
  binary: 'Binary',
  json: 'JSON',
};
