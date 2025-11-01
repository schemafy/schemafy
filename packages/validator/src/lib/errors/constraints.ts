import { ERROR_CODES } from './codes';
import { createErrorClass } from './base';

export const ConstraintNameNotUniqueError = createErrorClass('ConstraintNameNotUnique', {
  code: ERROR_CODES.CONSTRAINT_NAME_NOT_UNIQUE,
  messageTemplate:
    "Constraint name '{0}' already exists in the schema. Constraint names must be unique within a schema.",
  createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
});

export const ConstraintColumnRequiredError = createErrorClass('ConstraintColumnRequired', {
  code: ERROR_CODES.CONSTRAINT_COLUMN_REQUIRED,
  messageTemplate: "Constraint '{0}' must have at least one column.",
  createDetails: (constraintName: string) => ({ constraintName }),
});

export const ConstraintColumnNotExistError = createErrorClass('ConstraintColumnNotExist', {
  code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_EXIST,
  messageTemplate: "Column '{0}' specified in constraint '{1}' does not exist in the table.",
  createDetails: (columnName: string, constraintName: string) => ({
    columnName,
    constraintName,
  }),
});

export const ConstraintColumnNotUniqueError = createErrorClass('ConstraintColumnNotUnique', {
  code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_UNIQUE,
  messageTemplate: "Column '{0}' is already included in the constraint '{1}'.",
  createDetails: (columnName: string, constraintName: string) => ({
    columnName,
    constraintName,
  }),
});

export const PrimaryKeyRequiredError = createErrorClass('PrimaryKeyRequired', {
  code: ERROR_CODES.PRIMARY_KEY_REQUIRED,
  messageTemplate: "Table '{0}' must have a primary key.",
  createDetails: (tableName: string) => ({ tableName }),
});

export const PrimaryKeyNullableError = createErrorClass('PrimaryKeyNullable', {
  code: ERROR_CODES.PRIMARY_KEY_NULLABLE,
  messageTemplate: "Primary key column '{0}' cannot be nullable.",
  createDetails: (columnName: string) => ({ columnName }),
});

export const DuplicateKeyDefinitionError = createErrorClass('DuplicateKeyDefinition', {
  code: ERROR_CODES.DUPLICATE_KEY_DEFINITION,
  messageTemplate: "Constraint '{0}' has the same definition as existing constraint '{1}'.",
  createDetails: (constraintName: string, existingConstraintName: string) => ({
    constraintName,
    existingConstraintName,
  }),
});

export const CheckExpressionRequiredError = createErrorClass('CheckExpressionRequired', {
  code: ERROR_CODES.CHECK_EXPRESSION_REQUIRED,
  messageTemplate: "Check expression is required for CHECK constraint '{0}'.",
  createDetails: (constraintName: string) => ({ constraintName }),
});

export const DefaultConstraintError = createErrorClass('DefaultConstraint', {
  code: ERROR_CODES.DEFAULT_CONSTRAINT_ERROR,
  messageTemplate: 'Invalid DEFAULT constraint: {0}',
  createDetails: (reason: string) => ({ reason }),
});

export const ConstraintColumnSeqError = createErrorClass('ConstraintColumnSeq', {
  code: ERROR_CODES.CONSTRAINT_COLUMN_SEQ_ERROR,
  messageTemplate: "Column sequence in constraint '{0}' must be consecutive starting from 1.",
  createDetails: (constraintName: string) => ({ constraintName }),
});

export const ConstraintNotExistError = createErrorClass('ConstraintNotExist', {
  code: ERROR_CODES.CONSTRAINT_NOT_EXIST,
  messageTemplate: "Constraint '{0}' does not exist in table '{1}'",
  createDetails: (constraintId: string, tableId: string) => ({
    constraintId,
    tableId,
  }),
});

export const ConstraintInUseError = createErrorClass('ConstraintInUse', {
  code: ERROR_CODES.CONSTRAINT_IN_USE,
  messageTemplate: "Constraint '{0}' cannot be deleted because it is referenced by relationships: {1}",
  createDetails: (constraintId: string, referencingRelationships: string[]) => ({
    constraintId,
    referencingRelationships,
  }),
});

export const PrimaryKeyCannotDeleteError = createErrorClass('PrimaryKeyCannotDelete', {
  code: ERROR_CODES.PRIMARY_KEY_CANNOT_DELETE,
  messageTemplate: "Primary Key constraint '{0}' cannot be deleted from table '{1}'",
  createDetails: (constraintName: string, tableId: string) => ({
    constraintName,
    tableId,
  }),
});

export const UniqueSameAsPrimaryKeyError = createErrorClass('UniqueSameAsPrimaryKey', {
  code: ERROR_CODES.UNIQUE_SAME_AS_PRIMARY_KEY,
  messageTemplate: "Unique constraint '{0}' duplicates the Primary Key columns '{1}'",
  createDetails: (constraintName: string, primaryKeyName: string) => ({
    constraintName,
    primaryKeyName,
  }),
});
