export const SCHEMA_NAME_MIN_LENGTH = 3;
export const SCHEMA_NAME_MAX_LENGTH = 20;

export const validateSchemaName = (name: string): boolean => {
  const trimmed = name.trim();
  return trimmed.length >= SCHEMA_NAME_MIN_LENGTH && trimmed.length <= SCHEMA_NAME_MAX_LENGTH;
};
