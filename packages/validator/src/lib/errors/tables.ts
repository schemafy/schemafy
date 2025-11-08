import { ERROR_CODES } from './codes';
import { createErrorClass } from './base';

export const TableNameNotInvalidError = createErrorClass('TableNameNotInvalid', {
  code: ERROR_CODES.TABLE_NAME_NOT_INVALID,
  messageTemplate: "Table name '{0}' is invalid. Name must be between {1} and {2} characters",
  createDetails: (name: string, minLength = 1, maxLength = 20) => ({
    name,
    minLength,
    maxLength,
    actualLength: name.length,
  }),
});

export const TableNameNotUniqueError = createErrorClass('TableNameNotUnique', {
  code: ERROR_CODES.TABLE_NAME_NOT_UNIQUE,
  messageTemplate: "Table name '{0}' already exists in the schema. Table names must be unique within a schema",
  createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
});

export const TableNotExistError = createErrorClass('TableNotExist', {
  code: ERROR_CODES.TABLE_NOT_EXIST,
  messageTemplate: "Table '{0}' does not exist in schema '{1}'",
  createDetails: (tableId: string, schemaId: string) => ({ tableId, schemaId }),
});
