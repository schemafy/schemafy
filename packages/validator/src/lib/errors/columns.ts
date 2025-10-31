import { ERROR_CODES } from "./codes";
import { createErrorClass } from "./base";

export const ColumnNotExistError = createErrorClass("ColumnNotExist", {
  code: ERROR_CODES.COLUMN_NOT_EXIST,
  messageTemplate: "Column with ID '{0}' does not exist in table '{1}'",
  createDetails: (columnId: string, tableId: string) => ({ columnId, tableId }),
});

export const ColumnNameNotUniqueError = createErrorClass(
  "ColumnNameNotUnique",
  {
    code: ERROR_CODES.COLUMN_NAME_NOT_UNIQUE,
    messageTemplate:
      "Column name '{0}' already exists in the table. Column names must be unique within a table",
    createDetails: (name: string, tableId: string) => ({ name, tableId }),
  },
);

export const ColumnDataTypeRequiredError = createErrorClass(
  "ColumnDataTypeRequired",
  {
    code: ERROR_CODES.COLUMN_DATA_TYPE_REQUIRED,
    messageTemplate: "Data type is required for column '{0}' in table '{1}'",
    createDetails: (columnName: string, tableId: string) => ({
      columnName,
      tableId,
    }),
  },
);

export const ColumnDataTypeInvalidError = createErrorClass(
  "ColumnDataTypeInvalid",
  {
    code: ERROR_CODES.COLUMN_DATA_TYPE_INVALID,
    messageTemplate:
      "Data type '{0}' is not valid for the database vendor '{1}'",
    createDetails: (dataType: string, dbVendor: string) => ({
      dataType,
      dbVendor,
    }),
  },
);

export const ColumnLengthRequiredError = createErrorClass(
  "ColumnLengthRequired",
  {
    code: ERROR_CODES.COLUMN_LENGTH_REQUIRED,
    messageTemplate:
      "Length must be specified for data type '{1}' in column '{0}'",
    createDetails: (columnName: string, dataType: string) => ({
      columnName,
      dataType,
    }),
  },
);

export const ColumnPrecisionRequiredError = createErrorClass(
  "ColumnPrecisionRequired",
  {
    code: ERROR_CODES.COLUMN_PRECISION_REQUIRED,
    messageTemplate:
      "Precision and scale must be specified for data type '{1}' in column '{0}'",
    createDetails: (columnName: string, dataType: string) => ({
      columnName,
      dataType,
    }),
  },
);

export const ColumnInvalidError = createErrorClass("ColumnNameInvalid", {
  code: ERROR_CODES.COLUMN_INVALID,
  messageTemplate:
    "Column parsing '{0}' is invalid. Name must be between {1} and {2} characters.",
  createDetails: (name: string, minLength = 3, maxLength = 40) => ({
    name,
    minLength,
    maxLength,
    actualLength: name.length,
  }),
});

export const ColumnInvalidFormatError = createErrorClass(
  "ColumnNameInvalidFormat",
  {
    code: ERROR_CODES.COLUMN_INVALID_FORMAT,
    messageTemplate:
      "Column name '{0}' has an invalid format. It must start with a letter and contain only letters, numbers, and underscores.",
    createDetails: (name: string) => ({ name }),
  },
);

export const ColumnNameIsReservedKeywordError = createErrorClass(
  "ColumnNameIsReservedKeyword",
  {
    code: ERROR_CODES.COLUMN_NAME_IS_RESERVED,
    messageTemplate:
      "Column name '{0}' is a reserved keyword for the database vendor '{1}'.",
    createDetails: (name: string, dbVendor: string) => ({ name, dbVendor }),
  },
);

export const MultipleAutoIncrementColumnsError = createErrorClass(
  "MultipleAutoIncrementColumns",
  {
    code: ERROR_CODES.MULTIPLE_AUTO_INCREMENT,
    messageTemplate:
      "A table can only have one auto-increment column. Table ID: '{0}'",
    createDetails: (tableId: string) => ({ tableId }),
  },
);

export const AutoIncrementColumnError = createErrorClass(
  "AutoIncrementColumn",
  {
    code: ERROR_CODES.AUTO_INCREMENT_INVALID,
    messageTemplate: "Invalid Auto Increment column: {0}",
    createDetails: (reason: string) => ({ reason }),
  },
);

export const DefaultValueIncompatibleError = createErrorClass(
  "DefaultValueIncompatible",
  {
    code: ERROR_CODES.DEFAULT_VALUE_INCOMPATIBLE,
    messageTemplate:
      "Default value '{0}' is not compatible with data type '{1}' for column '{2}'.",
    createDetails: (
      defaultValue: string,
      dataType: string,
      columnName: string,
    ) => ({
      defaultValue,
      dataType,
      columnName,
    }),
  },
);

export const ColumnInUseError = createErrorClass("ColumnInUse", {
  code: ERROR_CODES.COLUMN_IN_USE,
  messageTemplate: "Column '{0}' cannot be deleted because it is used by: {1}",
  createDetails: (columnId: string, usage: string[]) => ({ columnId, usage }),
});

export const ColumnInPrimaryKeyError = createErrorClass("ColumnInPrimaryKey", {
  code: ERROR_CODES.COLUMN_IN_PRIMARY_KEY,
  messageTemplate:
    "Column '{0}' cannot be deleted because it is part of Primary Key '{1}'",
  createDetails: (columnId: string, constraintName: string) => ({
    columnId,
    constraintName,
  }),
});

export const LastColumnCannotDeleteError = createErrorClass(
  "LastColumnCannotDelete",
  {
    code: ERROR_CODES.LAST_COLUMN_CANNOT_DELETE,
    messageTemplate:
      "Cannot delete the last column '{0}' from table '{1}'. Table must have at least one column",
    createDetails: (columnId: string, tableId: string) => ({
      columnId,
      tableId,
    }),
  },
);

export const ColumnTypeIncompatibleError = createErrorClass(
  "ColumnTypeIncompatible",
  {
    code: ERROR_CODES.COLUMN_TYPE_INCOMPATIBLE,
    messageTemplate: "Cannot change column '{0}' type from '{1}' to '{2}': {3}",
    createDetails: (
      columnId: string,
      currentType: string,
      newType: string,
      reason: string,
    ) => ({
      columnId,
      currentType,
      newType,
      reason,
    }),
  },
);

export const ColumnNameChangeSameError = createErrorClass(
  "ColumnNameChangeSame",
  {
    code: ERROR_CODES.COLUMN_NAME_CHANGE_SAME,
    messageTemplate: "Cannot change column '{0}' same name as before",
    createDetails: (columnId: string) => ({ columnId }),
  },
);

export const ColumnTypeChangeSameError = createErrorClass(
  "ColumnTypeChangeSame",
  {
    code: ERROR_CODES.COLUMN_TYPE_CHANGE_SAME,
    messageTemplate: "Cannot change column '{0}' type same as before",
    createDetails: (columnId: string) => ({ columnId }),
  },
);

export const ColumnPositionChangeSameError = createErrorClass(
  "ColumnPositionChangeSame",
  {
    code: ERROR_CODES.COLUMN_POSITION_CHANGE_SAME,
    messageTemplate: "Cannot change column '{0}' position same as before",
    createDetails: (columnId: string) => ({ columnId }),
  },
);
