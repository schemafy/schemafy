import { SCHEMA_NAME_CONSTRAINTS } from '@/types/erd.types';

export const validateSchemaName = (name: string): boolean => {
  const trimmed = name.trim();
  return (
    trimmed.length >= SCHEMA_NAME_CONSTRAINTS.MIN_LENGTH &&
    trimmed.length <= SCHEMA_NAME_CONSTRAINTS.MAX_LENGTH
  );
};
