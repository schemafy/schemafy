export const parseIsoDate = (value: string | null | undefined): Date | null => {
  if (!value || value === '') return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) throw new Error(`Invalid ISO date: ${value}`);
  return d;
};

export const emptyStringToNull = (value: string | null | undefined): string | null | undefined => {
  return value === '' ? null : value;
};
