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
  const parts: string[] = [];
  for (const v of Object.values(parsed)) {
    if (typeof v === 'number') {
      parts.push(String(v));
    } else if (Array.isArray(v) && v.length > 0) {
      parts.push(v.join(', '));
    }
  }
  if (parts.length === 0) return type;
  return `${type}(${parts.join(',')})`;
};

export const CATEGORY_LABELS: Record<string, string> = {
  numeric: 'Numeric',
  datetime: 'Date & Time',
  string: 'String',
  binary: 'Binary',
  json: 'JSON',
};
