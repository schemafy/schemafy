import type { ColumnTypeArguments } from '../../api';

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
  if (precision != null && scale != null) {
    return `${type}(${precision},${scale})`;
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
