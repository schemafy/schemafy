import { ERROR_CODES } from "./codes";
import { createErrorClass } from "./base";

export const ConstraintNameNotUniqueError = createErrorClass(
  "ConstraintNameNotUnique",
  {
    code: ERROR_CODES.CONSTRAINT_NAME_NOT_UNIQUE,
    messageTemplate:
      "Constraint name '{0}' already exists in the schema. Constraint names must be unique within a schema.",
    createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
  },
);

export const ConstraintColumnNotExistError = createErrorClass(
  "ConstraintColumnNotExist",
  {
    code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_EXIST,
    messageTemplate:
      "Column '{0}' specified in constraint '{1}' does not exist in the table.",
    createDetails: (columnName: string, constraintName: string) => ({
      columnName,
      constraintName,
    }),
  },
);

export const ConstraintColumnNotUniqueError = createErrorClass(
  "ConstraintColumnNotUnique",
  {
    code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_UNIQUE,
    messageTemplate:
      "Column '{0}' is already included in the constraint '{1}'.",
    createDetails: (columnName: string, constraintName: string) => ({
      columnName,
      constraintName,
    }),
  },
);

export const DuplicateKeyDefinitionError = createErrorClass(
  "DuplicateKeyDefinition",
  {
    code: ERROR_CODES.DUPLICATE_KEY_DEFINITION,
    messageTemplate:
      "Constraint '{0}' has the same definition as existing constraint '{1}'.",
    createDetails: (
      constraintName: string,
      existingConstraintName: string,
    ) => ({
      constraintName,
      existingConstraintName,
    }),
  },
);

export const ConstraintNotExistError = createErrorClass("ConstraintNotExist", {
  code: ERROR_CODES.CONSTRAINT_NOT_EXIST,
  messageTemplate: "Constraint '{0}' does not exist in table '{1}'",
  createDetails: (constraintId: string, tableId: string) => ({
    constraintId,
    tableId,
  }),
});

export const UniqueSameAsPrimaryKeyError = createErrorClass(
  "UniqueSameAsPrimaryKey",
  {
    code: ERROR_CODES.UNIQUE_SAME_AS_PRIMARY_KEY,
    messageTemplate:
      "Unique constraint '{0}' duplicates the Primary Key columns '{1}'",
    createDetails: (constraintName: string, primaryKeyName: string) => ({
      constraintName,
      primaryKeyName,
    }),
  },
);
