export const parseLengthScale = (
  lengthScale: string,
): Record<string, number | string[] | null> => {
  try {
    return JSON.parse(lengthScale || '{}') ?? {};
  } catch {
    return {};
  }
};

export const formatTypeDisplay = (
  type: string,
  lengthScale: string,
): string => {
  const parsed = parseLengthScale(lengthScale);
  const values = Object.values(parsed).filter((v) => typeof v === 'number');
  if (values.length === 0) return type;
  return `${type}(${values.join(',')})`;
};

export const CATEGORY_LABELS: Record<string, string> = {
  numeric: 'Numeric',
  datetime: 'Date & Time',
  string: 'String',
  binary: 'Binary',
  json: 'JSON',
};
