import { ERROR_CODES } from './codes';
import { createErrorClass } from './base';

export const DatabaseEmptySchemaError = createErrorClass('DatabaseEmptySchema', {
  code: ERROR_CODES.DATABASE_EMPTY_SCHEMA,
  messageTemplate: 'Database must contain at least one schema',
  createDetails: () => ({}),
});

export const SchemaNotExistError = createErrorClass('SchemaNotExist', {
  code: ERROR_CODES.SCHEMA_NOT_EXIST,
  messageTemplate: "Schema with ID '{0}' does not exist",
  createDetails: (schemaId: string) => ({ schemaId }),
});

export const SchemaNameInvalidError = createErrorClass('SchemaNameInvalid', {
  code: ERROR_CODES.SCHEMA_INVALID,
  messageTemplate: "Schema name '{0}' is invalid. Name must be between {1} and {2} characters",
  createDetails: (name: string, minLength = 1, maxLength = 20) => ({
    name,
    minLength,
    maxLength,
    actualLength: name.length,
  }),
});

export const SchemaNameNotUniqueError = createErrorClass('SchemaNameNotUnique', {
  code: ERROR_CODES.SCHEMA_NAME_NOT_UNIQUE,
  messageTemplate: "Schema name '{0}' already exists. Schema names must be unique",
  createDetails: (name: string, existingSchemaId: string) => ({
    name,
    existingSchemaId,
  }),
});
