import { ERROR_CODES } from "./codes";
import { createErrorClass } from "./base";

export const TableNameNotInvalidError = createErrorClass(
  "TableNameNotInvalid",
  {
    code: ERROR_CODES.TABLE_NAME_NOT_INVALID,
    messageTemplate:
      "Table name '{0}' is invalid. Name must be between {1} and {2} characters",
    createDetails: (name: string, minLength = 3, maxLength = 20) => ({
      name,
      minLength,
      maxLength,
      actualLength: name.length,
    }),
  },
);

export const TableEmptyColumnError = createErrorClass("TableEmptyColumn", {
  code: ERROR_CODES.TABLE_EMPTY_COLUMN,
  messageTemplate: "Table '{0}' must contain at least one column",
  createDetails: (tableId: string) => ({ tableId }),
});

export const TableNameNotUniqueError = createErrorClass("TableNameNotUnique", {
  code: ERROR_CODES.TABLE_NAME_NOT_UNIQUE,
  messageTemplate:
    "Table name '{0}' already exists in the schema. Table names must be unique within a schema",
  createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
});

export const TableNotExistError = createErrorClass("TableNotExist", {
  code: ERROR_CODES.TABLE_NOT_EXIST,
  messageTemplate: "Table '{0}' does not exist in schema '{1}'",
  createDetails: (tableId: string, schemaId: string) => ({ tableId, schemaId }),
});

export const TableInUseError = createErrorClass("TableInUse", {
  code: ERROR_CODES.TABLE_IN_USE,
  messageTemplate:
    "Table '{0}' cannot be deleted because it is referenced by: {1}",
  createDetails: (tableId: string, referencingTables: string[]) => ({
    tableId,
    referencingTables,
  }),
});

export const ColumnPositionSeqError = createErrorClass("ColumnPositionSeq", {
  code: ERROR_CODES.COLUMN_POSITION_SEQ_ERROR,
  messageTemplate:
    "Column positions in table '{0}' must be consecutive starting from 1.",
  createDetails: (tableName: string) => ({ tableName }),
});
