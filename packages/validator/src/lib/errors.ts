export const ERROR_CODES = {
  DATABASE_EMPTY_SCHEMA: 'DATABASE_EMPTY_SCHEMA',
  SCHEMA_NOT_EXIST: 'SCHEMA_NOT_EXIST',
  SCHEMA_NAME_INVALID: 'SCHEMA_NAME_INVALID',
  SCHEMA_NAME_NOT_UNIQUE: 'SCHEMA_NAME_NOT_UNIQUE',
  TABLE_NAME_NOT_INVALID: 'TABLE_NAME_NOT_INVALID',
  TABLE_NAME_NOT_UNIQUE: 'TABLE_NAME_NOT_UNIQUE',
  TABLE_EMPTY_COLUMN: 'TABLE_EMPTY_COLUMN',
  COLUMN_NOT_EXIST: 'COLUMN_NOT_EXIST',
  COLUMN_NAME_NOT_UNIQUE: 'COLUMN_NAME_NOT_UNIQUE',
  COLUMN_DATA_TYPE_REQUIRED: 'COLUMN_DATA_TYPE_REQUIRED',
  COLUMN_DATA_TYPE_INVALID: 'COLUMN_DATA_TYPE_INVALID',
  COLUMN_LENGTH_REQUIRED: 'COLUMN_LENGTH_REQUIRED',
  COLUMN_PRECISION_REQUIRED: 'COLUMN_PRECISION_REQUIRED',
  COLUMN_NAME_INVALID: 'COLUMN_NAME_INVALID',
  COLUMN_NAME_INVALID_FORMAT: 'COLUMN_NAME_INVALID_FORMAT',
  COLUMN_NAME_IS_RESERVED: 'COLUMN_NAME_IS_RESERVED',
  MULTIPLE_AUTO_INCREMENT: 'MULTIPLE_AUTO_INCREMENT',
  AUTO_INCREMENT_INVALID: 'AUTO_INCREMENT_INVALID',
  DEFAULT_VALUE_INCOMPATIBLE: 'DEFAULT_VALUE_INCOMPATIBLE',
  INDEX_NAME_NOT_UNIQUE: 'INDEX_NAME_NOT_UNIQUE',
  INDEX_TYPE_INVALID: 'INDEX_TYPE_INVALID',
  INDEX_COLUMN_NOT_UNIQUE: 'INDEX_COLUMN_NOT_UNIQUE',
  DUPLICATE_INDEX_DEFINITION: 'DUPLICATE_INDEX_DEFINITION',
  INDEX_COLUMN_NOT_EXIST: 'INDEX_COLUMN_NOT_EXIST',
  INDEX_COLUMN_SEQ_ERROR: 'INDEX_COLUMN_SEQ_ERROR',
  INDEX_COLUMN_SORT_DIR_INVALID: 'INDEX_COLUMN_SORT_DIR_INVALID',
  CONSTRAINT_NAME_NOT_UNIQUE: 'CONSTRAINT_NAME_NOT_UNIQUE',
  CONSTRAINT_COLUMN_REQUIRED: 'CONSTRAINT_COLUMN_REQUIRED',
  CONSTRAINT_COLUMN_NOT_EXIST: 'CONSTRAINT_COLUMN_NOT_EXIST',
  CONSTRAINT_COLUMN_NOT_UNIQUE: 'CONSTRAINT_COLUMN_NOT_UNIQUE',
  PRIMARY_KEY_REQUIRED: 'PRIMARY_KEY_REQUIRED',
  PRIMARY_KEY_NULLABLE: 'PRIMARY_KEY_NULLABLE',
  DUPLICATE_KEY_DEFINITION: 'DUPLICATE_KEY_DEFINITION',
  CHECK_EXPRESSION_REQUIRED: 'CHECK_EXPRESSION_REQUIRED',
  DEFAULT_CONSTRAINT_ERROR: 'DEFAULT_CONSTRAINT_ERROR',
  CONSTRAINT_COLUMN_SEQ_ERROR: 'CONSTRAINT_COLUMN_SEQ_ERROR',
  RELATIONSHIP_COLUMN_SEQ_ERROR: 'RELATIONSHIP_COLUMN_SEQ_ERROR',
  COLUMN_POSITION_SEQ_ERROR: 'COLUMN_POSITION_SEQ_ERROR',
  IDENTIFYING_RELATIONSHIP_ORDER_ERROR: 'IDENTIFYING_RELATIONSHIP_ORDER_ERROR',
  COMPOSITE_KEY_ORDER_MISMATCH: 'COMPOSITE_KEY_ORDER_MISMATCH',
  RELATIONSHIP_NOT_EXIST: 'RELATIONSHIP_NOT_EXIST',
  RELATIONSHIP_EMPTY_ERROR: 'RELATIONSHIP_EMPTY_ERROR',
  RELATIONSHIP_NAME_NOT_UNIQUE: 'RELATIONSHIP_NAME_NOT_UNIQUE',
  RELATIONSHIP_COLUNN_NOT_EXIST: 'RELATIONSHIP_COLUNN_NOT_EXIST',
  RELATIONSHIP_COLUMN_MAPPING_DUPLICATE: 'RELATIONSHIP_COLUMN_MAPPING_DUPLICATE',
  RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE: 'RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE',
  RELATIONSHIP_DELETE_SET_NULL_ERROR: 'RELATIONSHIP_DELETE_SET_NULL_ERROR',
  RELATIONSHIP_CYCLIC_REFERENCE: 'RELATIONSHIP_CYCLIC_REFERENCE',
  RELATIONSHIP_TARGET_TABLE_NOT_EXIST: 'RELATIONSHIP_TARGET_TABLE_NOT_EXIST',
  SCHEMA_IN_USE: 'SCHEMA_IN_USE',
  TABLE_NOT_EXIST: 'TABLE_NOT_EXIST',
  TABLE_IN_USE: 'TABLE_IN_USE',
  COLUMN_IN_USE: 'COLUMN_IN_USE',
  COLUMN_IN_PRIMARY_KEY: 'COLUMN_IN_PRIMARY_KEY',
  INDEX_NOT_EXIST: 'INDEX_NOT_EXIST',
  CONSTRAINT_NOT_EXIST: 'CONSTRAINT_NOT_EXIST',
  CONSTRAINT_IN_USE: 'CONSTRAINT_IN_USE',
  LAST_SCHEMA_CANNOT_DELETE: 'LAST_SCHEMA_CANNOT_DELETE',
  LAST_COLUMN_CANNOT_DELETE: 'LAST_COLUMN_CANNOT_DELETE',
  PRIMARY_KEY_CANNOT_DELETE: 'PRIMARY_KEY_CANNOT_DELETE',
  COLUMN_TYPE_INCOMPATIBLE: 'COLUMN_TYPE_INCOMPATIBLE',
  UNIQUE_SAME_AS_PRIMARY_KEY: 'UNIQUE_SAME_AS_PRIMARY_KEY',
} as const;

export type ErrorCode = (typeof ERROR_CODES)[keyof typeof ERROR_CODES];

export class ERDValidationError extends Error {
  public readonly code: ErrorCode;
  public readonly details?: Record<string, unknown>;

  constructor(code: ErrorCode, message: string, details?: Record<string, unknown>) {
    super(message);
    this.name = 'ERDValidationError';
    this.code = code;
    this.details = details;
  }
}

interface ErrorDefinition {
  code: ErrorCode;
  messageTemplate: string;
  createDetails: (...args: any[]) => Record<string, unknown>;
}

function createErrorClass(name: string, definition: ErrorDefinition) {
  return class extends ERDValidationError {
    constructor(...args: any[]) {
      const message = definition.messageTemplate.replace(/\{(\d+)\}/g, (match, index) => {
        return args[parseInt(index)] || match;
      });
      const details = definition.createDetails(...args);
      super(definition.code, message, details);
      this.name = `${name}Error`;
    }
  };
}

const ERROR_DEFINITIONS: Record<keyof typeof ERROR_CODES, ErrorDefinition> = {
  DATABASE_EMPTY_SCHEMA: {
    code: ERROR_CODES.DATABASE_EMPTY_SCHEMA,
    messageTemplate: 'Database must contain at least one schema',
    createDetails: () => ({}),
  },
  SCHEMA_NOT_EXIST: {
    code: ERROR_CODES.SCHEMA_NOT_EXIST,
    messageTemplate: "Schema with ID '{0}' does not exist",
    createDetails: (schemaId: string) => ({ schemaId }),
  },
  SCHEMA_NAME_INVALID: {
    code: ERROR_CODES.SCHEMA_NAME_INVALID,
    messageTemplate: "Schema name '{0}' is invalid. Name must be between {1} and {2} characters",
    createDetails: (name: string, minLength = 3, maxLength = 20) => ({
      name,
      minLength,
      maxLength,
      actualLength: name.length,
    }),
  },
  SCHEMA_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.SCHEMA_NAME_NOT_UNIQUE,
    messageTemplate: "Schema name '{0}' already exists. Schema names must be unique",
    createDetails: (name: string, existingSchemaId: string) => ({ name, existingSchemaId }),
  },
  TABLE_NAME_NOT_INVALID: {
    code: ERROR_CODES.TABLE_NAME_NOT_INVALID,
    messageTemplate: "Table name '{0}' is invalid. Name must be between {1} and {2} characters",
    createDetails: (name: string, minLength = 3, maxLength = 20) => ({
      name,
      minLength,
      maxLength,
      actualLength: name.length,
    }),
  },
  TABLE_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.TABLE_NAME_NOT_UNIQUE,
    messageTemplate: "Table name '{0}' already exists in the schema. Table names must be unique within a schema",
    createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
  },
  TABLE_EMPTY_COLUMN: {
    code: ERROR_CODES.TABLE_EMPTY_COLUMN,
    messageTemplate: "Table '{0}' must contain at least one column",
    createDetails: (tableId: string) => ({ tableId }),
  },
  COLUMN_NOT_EXIST: {
    code: ERROR_CODES.COLUMN_NOT_EXIST,
    messageTemplate: "Column with ID '{0}' does not exist in table '{1}'",
    createDetails: (columnId: string, tableId: string) => ({ columnId, tableId }),
  },
  COLUMN_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.COLUMN_NAME_NOT_UNIQUE,
    messageTemplate: "Column name '{0}' already exists in the table. Column names must be unique within a table",
    createDetails: (name: string, tableId: string) => ({ name, tableId }),
  },
  COLUMN_DATA_TYPE_REQUIRED: {
    code: ERROR_CODES.COLUMN_DATA_TYPE_REQUIRED,
    messageTemplate: "Data type is required for column '{0}' in table '{1}'",
    createDetails: (columnName: string, tableId: string) => ({ columnName, tableId }),
  },
  COLUMN_DATA_TYPE_INVALID: {
    code: ERROR_CODES.COLUMN_DATA_TYPE_INVALID,
    messageTemplate: "Data type '{0}' is not valid for the database vendor '{1}'",
    createDetails: (dataType: string, dbVendor: string) => ({ dataType, dbVendor }),
  },
  COLUMN_LENGTH_REQUIRED: {
    code: ERROR_CODES.COLUMN_LENGTH_REQUIRED,
    messageTemplate: "Length must be specified for data type '{1}' in column '{0}'",
    createDetails: (columnName: string, dataType: string) => ({ columnName, dataType }),
  },
  COLUMN_PRECISION_REQUIRED: {
    code: ERROR_CODES.COLUMN_PRECISION_REQUIRED,
    messageTemplate: "Precision and scale must be specified for data type '{1}' in column '{0}'",
    createDetails: (columnName: string, dataType: string) => ({ columnName, dataType }),
  },
  COLUMN_NAME_INVALID: {
    code: ERROR_CODES.COLUMN_NAME_INVALID,
    messageTemplate: "Column name '{0}' is invalid. Name must be between {1} and {2} characters.",
    createDetails: (name: string, minLength = 3, maxLength = 40) => ({
      name,
      minLength,
      maxLength,
      actualLength: name.length,
    }),
  },
  COLUMN_NAME_INVALID_FORMAT: {
    code: ERROR_CODES.COLUMN_NAME_INVALID_FORMAT,
    messageTemplate:
      "Column name '{0}' has an invalid format. It must start with a letter and contain only letters, numbers, and underscores.",
    createDetails: (name: string) => ({ name }),
  },
  COLUMN_NAME_IS_RESERVED: {
    code: ERROR_CODES.COLUMN_NAME_IS_RESERVED,
    messageTemplate: "Column name '{0}' is a reserved keyword for the database vendor '{1}'.",
    createDetails: (name: string, dbVendor: string) => ({ name, dbVendor }),
  },
  MULTIPLE_AUTO_INCREMENT: {
    code: ERROR_CODES.MULTIPLE_AUTO_INCREMENT,
    messageTemplate: "A table can only have one auto-increment column. Table ID: '{0}'",
    createDetails: (tableId: string) => ({ tableId }),
  },
  AUTO_INCREMENT_INVALID: {
    code: ERROR_CODES.AUTO_INCREMENT_INVALID,
    messageTemplate: 'Invalid Auto Increment column: {0}',
    createDetails: (reason: string) => ({ reason }),
  },
  DEFAULT_VALUE_INCOMPATIBLE: {
    code: ERROR_CODES.DEFAULT_VALUE_INCOMPATIBLE,
    messageTemplate: "Default value '{0}' is not compatible with data type '{1}' for column '{2}'.",
    createDetails: (defaultValue: string, dataType: string, columnName: string) => ({
      defaultValue,
      dataType,
      columnName,
    }),
  },
  INDEX_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.INDEX_NAME_NOT_UNIQUE,
    messageTemplate: "Index name '{0}' already exists in the table. Index names must be unique within a table.",
    createDetails: (name: string, tableId: string) => ({ name, tableId }),
  },
  INDEX_TYPE_INVALID: {
    code: ERROR_CODES.INDEX_TYPE_INVALID,
    messageTemplate: "Index type '{0}' is not valid for the database vendor '{1}'.",
    createDetails: (type: string, dbVendor: string) => ({ type, dbVendor }),
  },
  INDEX_COLUMN_NOT_UNIQUE: {
    code: ERROR_CODES.INDEX_COLUMN_NOT_UNIQUE,
    messageTemplate: "Column '{0}' is already included in the index '{1}'.",
    createDetails: (columnName: string, indexName: string) => ({ columnName, indexName }),
  },
  DUPLICATE_INDEX_DEFINITION: {
    code: ERROR_CODES.DUPLICATE_INDEX_DEFINITION,
    messageTemplate: "Index '{0}' has the same definition as existing index '{1}'.",
    createDetails: (indexName: string, existingIndexName: string) => ({ indexName, existingIndexName }),
  },
  INDEX_COLUMN_NOT_EXIST: {
    code: ERROR_CODES.INDEX_COLUMN_NOT_EXIST,
    messageTemplate: "Column '{0}' specified in index '{1}' does not exist in the table.",
    createDetails: (columnName: string, indexName: string) => ({ columnName, indexName }),
  },
  INDEX_COLUMN_SEQ_ERROR: {
    code: ERROR_CODES.INDEX_COLUMN_SEQ_ERROR,
    messageTemplate: "Column sequence in index '{0}' must be consecutive starting from 1.",
    createDetails: (indexName: string) => ({ indexName }),
  },
  INDEX_COLUMN_SORT_DIR_INVALID: {
    code: ERROR_CODES.INDEX_COLUMN_SORT_DIR_INVALID,
    messageTemplate: "Sort direction '{0}' is invalid for index column in index '{1}'. Use 'ASC' or 'DESC'.",
    createDetails: (sortDir: string, indexName: string) => ({ sortDir, indexName }),
  },
  CONSTRAINT_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.CONSTRAINT_NAME_NOT_UNIQUE,
    messageTemplate:
      "Constraint name '{0}' already exists in the schema. Constraint names must be unique within a schema.",
    createDetails: (name: string, schemaId: string) => ({ name, schemaId }),
  },
  CONSTRAINT_COLUMN_REQUIRED: {
    code: ERROR_CODES.CONSTRAINT_COLUMN_REQUIRED,
    messageTemplate: "Constraint '{0}' must have at least one column.",
    createDetails: (constraintName: string) => ({ constraintName }),
  },
  CONSTRAINT_COLUMN_NOT_EXIST: {
    code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_EXIST,
    messageTemplate: "Column '{0}' specified in constraint '{1}' does not exist in the table.",
    createDetails: (columnName: string, constraintName: string) => ({ columnName, constraintName }),
  },
  CONSTRAINT_COLUMN_NOT_UNIQUE: {
    code: ERROR_CODES.CONSTRAINT_COLUMN_NOT_UNIQUE,
    messageTemplate: "Column '{0}' is already included in the constraint '{1}'.",
    createDetails: (columnName: string, constraintName: string) => ({ columnName, constraintName }),
  },
  PRIMARY_KEY_REQUIRED: {
    code: ERROR_CODES.PRIMARY_KEY_REQUIRED,
    messageTemplate: "Table '{0}' must have a primary key.",
    createDetails: (tableName: string) => ({ tableName }),
  },
  PRIMARY_KEY_NULLABLE: {
    code: ERROR_CODES.PRIMARY_KEY_NULLABLE,
    messageTemplate: "Primary key column '{0}' cannot be nullable.",
    createDetails: (columnName: string) => ({ columnName }),
  },
  DUPLICATE_KEY_DEFINITION: {
    code: ERROR_CODES.DUPLICATE_KEY_DEFINITION,
    messageTemplate: "Constraint '{0}' has the same definition as existing constraint '{1}'.",
    createDetails: (constraintName: string, existingConstraintName: string) => ({
      constraintName,
      existingConstraintName,
    }),
  },
  CHECK_EXPRESSION_REQUIRED: {
    code: ERROR_CODES.CHECK_EXPRESSION_REQUIRED,
    messageTemplate: "Check expression is required for CHECK constraint '{0}'.",
    createDetails: (constraintName: string) => ({ constraintName }),
  },
  DEFAULT_CONSTRAINT_ERROR: {
    code: ERROR_CODES.DEFAULT_CONSTRAINT_ERROR,
    messageTemplate: 'Invalid DEFAULT constraint: {0}',
    createDetails: (reason: string) => ({ reason }),
  },
  CONSTRAINT_COLUMN_SEQ_ERROR: {
    code: ERROR_CODES.CONSTRAINT_COLUMN_SEQ_ERROR,
    messageTemplate: "Column sequence in constraint '{0}' must be consecutive starting from 1.",
    createDetails: (constraintName: string) => ({ constraintName }),
  },
  RELATIONSHIP_COLUMN_SEQ_ERROR: {
    code: ERROR_CODES.RELATIONSHIP_COLUMN_SEQ_ERROR,
    messageTemplate: "Column sequence in relationship '{0}' must be consecutive starting from 1.",
    createDetails: (relationshipName: string) => ({ relationshipName }),
  },
  COLUMN_POSITION_SEQ_ERROR: {
    code: ERROR_CODES.COLUMN_POSITION_SEQ_ERROR,
    messageTemplate: "Column positions in table '{0}' must be consecutive starting from 1.",
    createDetails: (tableName: string) => ({ tableName }),
  },
  IDENTIFYING_RELATIONSHIP_ORDER_ERROR: {
    code: ERROR_CODES.IDENTIFYING_RELATIONSHIP_ORDER_ERROR,
    messageTemplate: "Column order mismatch in identifying relationship '{0}': {1}",
    createDetails: (relationshipName: string, reason: string) => ({ relationshipName, reason }),
  },
  COMPOSITE_KEY_ORDER_MISMATCH: {
    code: ERROR_CODES.COMPOSITE_KEY_ORDER_MISMATCH,
    messageTemplate: "Column order issue in {0} '{1}': {2}",
    createDetails: (keyType: string, keyName: string, reason: string) => ({ keyType, keyName, reason }),
  },
  RELATIONSHIP_NOT_EXIST: {
    code: ERROR_CODES.RELATIONSHIP_NOT_EXIST,
    messageTemplate: "Relationship with ID '{0}' does not exist",
    createDetails: (relationshipId: string) => ({ relationshipId }),
  },
  RELATIONSHIP_EMPTY_ERROR: {
    code: ERROR_CODES.RELATIONSHIP_EMPTY_ERROR,
    messageTemplate: "Relationship '{0}' must have at least one column mapping",
    createDetails: (relationshipName: string) => ({ relationshipName }),
  },
  RELATIONSHIP_NAME_NOT_UNIQUE: {
    code: ERROR_CODES.RELATIONSHIP_NAME_NOT_UNIQUE,
    messageTemplate: "Relationship name '{0}' must be unique within table '{1}'",
    createDetails: (relationshipName: string, tableId: string) => ({ relationshipName, tableId }),
  },
  RELATIONSHIP_COLUNN_NOT_EXIST: {
    code: ERROR_CODES.RELATIONSHIP_COLUNN_NOT_EXIST,
    messageTemplate: "Relationship column with ID '{0}' does not exist in relationship '{1}'",
    createDetails: (relationshipColumnId: string, relationshipId: string) => ({ relationshipColumnId, relationshipId }),
  },
  RELATIONSHIP_COLUMN_MAPPING_DUPLICATE: {
    code: ERROR_CODES.RELATIONSHIP_COLUMN_MAPPING_DUPLICATE,
    messageTemplate: "Column mapping already exists in relationship '{0}': {1} -> {2}",
    createDetails: (relationshipName: string, srcColumnId: string, tgtColumnId: string) => ({
      relationshipName,
      srcColumnId,
      tgtColumnId,
    }),
  },
  RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE: {
    code: ERROR_CODES.RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE,
    messageTemplate: "Related columns must have compatible data types in relationship '{0}': {1} <-> {2}",
    createDetails: (relationshipName: string, srcColumn: string, tgtColumn: string) => ({
      relationshipName,
      srcColumn,
      tgtColumn,
    }),
  },
  RELATIONSHIP_DELETE_SET_NULL_ERROR: {
    code: ERROR_CODES.RELATIONSHIP_DELETE_SET_NULL_ERROR,
    messageTemplate: "ON DELETE SET NULL requires nullable foreign key columns in relationship '{0}': {1}",
    createDetails: (relationshipName: string, nonNullableColumns: string[]) => ({
      relationshipName,
      nonNullableColumns,
    }),
  },
  RELATIONSHIP_CYCLIC_REFERENCE: {
    code: ERROR_CODES.RELATIONSHIP_CYCLIC_REFERENCE,
    messageTemplate: 'Direct cyclic reference detected between tables: {0} <-> {1}',
    createDetails: (table1: string, table2: string) => ({ table1, table2 }),
  },
  RELATIONSHIP_TARGET_TABLE_NOT_EXIST: {
    code: ERROR_CODES.RELATIONSHIP_TARGET_TABLE_NOT_EXIST,
    messageTemplate: "Target table '{1}' does not exist for relationship '{0}'",
    createDetails: (relationshipName: string, targetTableId: string) => ({ relationshipName, targetTableId }),
  },
  SCHEMA_IN_USE: {
    code: ERROR_CODES.SCHEMA_IN_USE,
    messageTemplate: "Schema '{0}' cannot be deleted because it contains {1} table(s)",
    createDetails: (schemaId: string, tableCount: number) => ({ schemaId, tableCount }),
  },
  TABLE_NOT_EXIST: {
    code: ERROR_CODES.TABLE_NOT_EXIST,
    messageTemplate: "Table '{0}' does not exist in schema '{1}'",
    createDetails: (tableId: string, schemaId: string) => ({ tableId, schemaId }),
  },
  TABLE_IN_USE: {
    code: ERROR_CODES.TABLE_IN_USE,
    messageTemplate: "Table '{0}' cannot be deleted because it is referenced by: {1}",
    createDetails: (tableId: string, referencingTables: string[]) => ({ tableId, referencingTables }),
  },
  COLUMN_IN_USE: {
    code: ERROR_CODES.COLUMN_IN_USE,
    messageTemplate: "Column '{0}' cannot be deleted because it is used by: {1}",
    createDetails: (columnId: string, usage: string[]) => ({ columnId, usage }),
  },
  COLUMN_IN_PRIMARY_KEY: {
    code: ERROR_CODES.COLUMN_IN_PRIMARY_KEY,
    messageTemplate: "Column '{0}' cannot be deleted because it is part of Primary Key '{1}'",
    createDetails: (columnId: string, constraintName: string) => ({ columnId, constraintName }),
  },
  INDEX_NOT_EXIST: {
    code: ERROR_CODES.INDEX_NOT_EXIST,
    messageTemplate: "Index '{0}' does not exist in table '{1}'",
    createDetails: (indexId: string, tableId: string) => ({ indexId, tableId }),
  },
  CONSTRAINT_NOT_EXIST: {
    code: ERROR_CODES.CONSTRAINT_NOT_EXIST,
    messageTemplate: "Constraint '{0}' does not exist in table '{1}'",
    createDetails: (constraintId: string, tableId: string) => ({ constraintId, tableId }),
  },
  CONSTRAINT_IN_USE: {
    code: ERROR_CODES.CONSTRAINT_IN_USE,
    messageTemplate: "Constraint '{0}' cannot be deleted because it is referenced by relationships: {1}",
    createDetails: (constraintId: string, referencingRelationships: string[]) => ({
      constraintId,
      referencingRelationships,
    }),
  },
  LAST_SCHEMA_CANNOT_DELETE: {
    code: ERROR_CODES.LAST_SCHEMA_CANNOT_DELETE,
    messageTemplate: "Cannot delete the last schema '{0}'. Database must have at least one schema",
    createDetails: (schemaId: string) => ({ schemaId }),
  },
  LAST_COLUMN_CANNOT_DELETE: {
    code: ERROR_CODES.LAST_COLUMN_CANNOT_DELETE,
    messageTemplate: "Cannot delete the last column '{0}' from table '{1}'. Table must have at least one column",
    createDetails: (columnId: string, tableId: string) => ({ columnId, tableId }),
  },
  PRIMARY_KEY_CANNOT_DELETE: {
    code: ERROR_CODES.PRIMARY_KEY_CANNOT_DELETE,
    messageTemplate: "Primary Key constraint '{0}' cannot be deleted from table '{1}'",
    createDetails: (constraintName: string, tableId: string) => ({ constraintName, tableId }),
  },
  COLUMN_TYPE_INCOMPATIBLE: {
    code: ERROR_CODES.COLUMN_TYPE_INCOMPATIBLE,
    messageTemplate: "Cannot change column '{0}' type from '{1}' to '{2}': {3}",
    createDetails: (columnId: string, currentType: string, newType: string, reason: string) => ({
      columnId,
      currentType,
      newType,
      reason,
    }),
  },
  UNIQUE_SAME_AS_PRIMARY_KEY: {
    code: ERROR_CODES.UNIQUE_SAME_AS_PRIMARY_KEY,
    messageTemplate: "Unique constraint '{0}' duplicates the Primary Key columns '{1}'",
    createDetails: (constraintName: string, primaryKeyName: string) => ({ constraintName, primaryKeyName }),
  },
};

export const DatabaseEmptySchemaError = createErrorClass(
  'DatabaseEmptySchema',
  ERROR_DEFINITIONS.DATABASE_EMPTY_SCHEMA
);

export const SchemaNotExistError = createErrorClass('SchemaNotExist', ERROR_DEFINITIONS.SCHEMA_NOT_EXIST);
export const SchemaNameInvalidError = createErrorClass('SchemaNameInvalid', ERROR_DEFINITIONS.SCHEMA_NAME_INVALID);
export const SchemaNameNotUniqueError = createErrorClass(
  'SchemaNameNotUnique',
  ERROR_DEFINITIONS.SCHEMA_NAME_NOT_UNIQUE
);
export const TableNameNotInvalidError = createErrorClass(
  'TableNameNotInvalid',
  ERROR_DEFINITIONS.TABLE_NAME_NOT_INVALID
);
export const TableEmptyColumnError = createErrorClass('TableEmptyColumn', ERROR_DEFINITIONS.TABLE_EMPTY_COLUMN);
export const TableNameNotUniqueError = createErrorClass('TableNameNotUnique', ERROR_DEFINITIONS.TABLE_NAME_NOT_UNIQUE);
export const ColumnNotExistError = createErrorClass('ColumnNotExist', ERROR_DEFINITIONS.COLUMN_NOT_EXIST);
export const ColumnNameNotUniqueError = createErrorClass(
  'ColumnNameNotUnique',
  ERROR_DEFINITIONS.COLUMN_NAME_NOT_UNIQUE
);
export const ColumnDataTypeRequiredError = createErrorClass(
  'ColumnDataTypeRequired',
  ERROR_DEFINITIONS.COLUMN_DATA_TYPE_REQUIRED
);
export const ColumnDataTypeInvalidError = createErrorClass(
  'ColumnDataTypeInvalid',
  ERROR_DEFINITIONS.COLUMN_DATA_TYPE_INVALID
);
export const ColumnLengthRequiredError = createErrorClass(
  'ColumnLengthRequired',
  ERROR_DEFINITIONS.COLUMN_LENGTH_REQUIRED
);
export const ColumnPrecisionRequiredError = createErrorClass(
  'ColumnPrecisionRequired',
  ERROR_DEFINITIONS.COLUMN_PRECISION_REQUIRED
);
export const ColumnNameInvalidError = createErrorClass('ColumnNameInvalid', ERROR_DEFINITIONS.COLUMN_NAME_INVALID);
export const ColumnNameInvalidFormatError = createErrorClass(
  'ColumnNameInvalidFormat',
  ERROR_DEFINITIONS.COLUMN_NAME_INVALID_FORMAT
);
export const ColumnNameIsReservedKeywordError = createErrorClass(
  'ColumnNameIsReservedKeyword',
  ERROR_DEFINITIONS.COLUMN_NAME_IS_RESERVED
);
export const MultipleAutoIncrementColumnsError = createErrorClass(
  'MultipleAutoIncrementColumns',
  ERROR_DEFINITIONS.MULTIPLE_AUTO_INCREMENT
);
export const AutoIncrementColumnError = createErrorClass(
  'AutoIncrementColumn',
  ERROR_DEFINITIONS.AUTO_INCREMENT_INVALID
);
export const DefaultValueIncompatibleError = createErrorClass(
  'DefaultValueIncompatible',
  ERROR_DEFINITIONS.DEFAULT_VALUE_INCOMPATIBLE
);

export const IndexNameNotUniqueError = createErrorClass('IndexNameNotUnique', ERROR_DEFINITIONS.INDEX_NAME_NOT_UNIQUE);
export const IndexTypeInvalidError = createErrorClass('IndexTypeInvalid', ERROR_DEFINITIONS.INDEX_TYPE_INVALID);
export const IndexColumnNotUniqueError = createErrorClass(
  'IndexColumnNotUnique',
  ERROR_DEFINITIONS.INDEX_COLUMN_NOT_UNIQUE
);
export const DuplicateIndexDefinitionError = createErrorClass(
  'DuplicateIndexDefinition',
  ERROR_DEFINITIONS.DUPLICATE_INDEX_DEFINITION
);
export const IndexColumnNotExistError = createErrorClass(
  'IndexColumnNotExist',
  ERROR_DEFINITIONS.INDEX_COLUMN_NOT_EXIST
);
export const IndexColumnSeqError = createErrorClass('IndexColumnSeq', ERROR_DEFINITIONS.INDEX_COLUMN_SEQ_ERROR);
export const IndexColumnSortDirInvalidError = createErrorClass(
  'IndexColumnSortDirInvalid',
  ERROR_DEFINITIONS.INDEX_COLUMN_SORT_DIR_INVALID
);

export const ConstraintNameNotUniqueError = createErrorClass(
  'ConstraintNameNotUnique',
  ERROR_DEFINITIONS.CONSTRAINT_NAME_NOT_UNIQUE
);
export const ConstraintColumnRequiredError = createErrorClass(
  'ConstraintColumnRequired',
  ERROR_DEFINITIONS.CONSTRAINT_COLUMN_REQUIRED
);
export const ConstraintColumnNotExistError = createErrorClass(
  'ConstraintColumnNotExist',
  ERROR_DEFINITIONS.CONSTRAINT_COLUMN_NOT_EXIST
);
export const ConstraintColumnNotUniqueError = createErrorClass(
  'ConstraintColumnNotUnique',
  ERROR_DEFINITIONS.CONSTRAINT_COLUMN_NOT_UNIQUE
);
export const PrimaryKeyRequiredError = createErrorClass('PrimaryKeyRequired', ERROR_DEFINITIONS.PRIMARY_KEY_REQUIRED);
export const PrimaryKeyNullableError = createErrorClass('PrimaryKeyNullable', ERROR_DEFINITIONS.PRIMARY_KEY_NULLABLE);
export const DuplicateKeyDefinitionError = createErrorClass(
  'DuplicateKeyDefinition',
  ERROR_DEFINITIONS.DUPLICATE_KEY_DEFINITION
);
export const CheckExpressionRequiredError = createErrorClass(
  'CheckExpressionRequired',
  ERROR_DEFINITIONS.CHECK_EXPRESSION_REQUIRED
);
export const DefaultConstraintError = createErrorClass('DefaultConstraint', ERROR_DEFINITIONS.DEFAULT_CONSTRAINT_ERROR);
export const ConstraintColumnSeqError = createErrorClass(
  'ConstraintColumnSeq',
  ERROR_DEFINITIONS.CONSTRAINT_COLUMN_SEQ_ERROR
);

export const RelationshipColumnSeqError = createErrorClass(
  'RelationshipColumnSeq',
  ERROR_DEFINITIONS.RELATIONSHIP_COLUMN_SEQ_ERROR
);
export const ColumnPositionSeqError = createErrorClass(
  'ColumnPositionSeq',
  ERROR_DEFINITIONS.COLUMN_POSITION_SEQ_ERROR
);
export const IdentifyingRelationshipOrderError = createErrorClass(
  'IdentifyingRelationshipOrder',
  ERROR_DEFINITIONS.IDENTIFYING_RELATIONSHIP_ORDER_ERROR
);
export const CompositeKeyOrderMismatchError = createErrorClass(
  'CompositeKeyOrderMismatch',
  ERROR_DEFINITIONS.COMPOSITE_KEY_ORDER_MISMATCH
);
export const RelationshipNotExistError = createErrorClass(
  'RelationshipNotExist',
  ERROR_DEFINITIONS.RELATIONSHIP_NOT_EXIST
);
export const RelationshipEmptyError = createErrorClass('RelationshipEmpty', ERROR_DEFINITIONS.RELATIONSHIP_EMPTY_ERROR);
export const RelationshipNameNotUniqueError = createErrorClass(
  'RelationshipNameNotUnique',
  ERROR_DEFINITIONS.RELATIONSHIP_NAME_NOT_UNIQUE
);
export const RelationshipColumnNotExistError = createErrorClass(
  'RelationshipColumnNotExist',
  ERROR_DEFINITIONS.RELATIONSHIP_COLUNN_NOT_EXIST
);
export const RelationshipColumnMappingDuplicateError = createErrorClass(
  'RelationshipColumnMappingDuplicate',
  ERROR_DEFINITIONS.RELATIONSHIP_COLUMN_MAPPING_DUPLICATE
);
export const RelationshipColumnTypeIncompatibleError = createErrorClass(
  'RelationshipColumnTypeIncompatible',
  ERROR_DEFINITIONS.RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE
);
export const RelationshipDeleteSetNullError = createErrorClass(
  'RelationshipDeleteSetNull',
  ERROR_DEFINITIONS.RELATIONSHIP_DELETE_SET_NULL_ERROR
);
export const RelationshipCyclicReferenceError = createErrorClass(
  'RelationshipCyclicReference',
  ERROR_DEFINITIONS.RELATIONSHIP_CYCLIC_REFERENCE
);
export const RelationshipTargetTableNotExistError = createErrorClass(
  'RelationshipTargetTableNotExist',
  ERROR_DEFINITIONS.RELATIONSHIP_TARGET_TABLE_NOT_EXIST
);

export const SchemaInUseError = createErrorClass('SchemaInUse', ERROR_DEFINITIONS.SCHEMA_IN_USE);
export const TableNotExistError = createErrorClass('TableNotExist', ERROR_DEFINITIONS.TABLE_NOT_EXIST);
export const TableInUseError = createErrorClass('TableInUse', ERROR_DEFINITIONS.TABLE_IN_USE);
export const ColumnInUseError = createErrorClass('ColumnInUse', ERROR_DEFINITIONS.COLUMN_IN_USE);
export const ColumnInPrimaryKeyError = createErrorClass('ColumnInPrimaryKey', ERROR_DEFINITIONS.COLUMN_IN_PRIMARY_KEY);
export const IndexNotExistError = createErrorClass('IndexNotExist', ERROR_DEFINITIONS.INDEX_NOT_EXIST);
export const ConstraintNotExistError = createErrorClass('ConstraintNotExist', ERROR_DEFINITIONS.CONSTRAINT_NOT_EXIST);
export const ConstraintInUseError = createErrorClass('ConstraintInUse', ERROR_DEFINITIONS.CONSTRAINT_IN_USE);
export const LastSchemaCannotDeleteError = createErrorClass(
  'LastSchemaCannotDelete',
  ERROR_DEFINITIONS.LAST_SCHEMA_CANNOT_DELETE
);
export const LastColumnCannotDeleteError = createErrorClass(
  'LastColumnCannotDelete',
  ERROR_DEFINITIONS.LAST_COLUMN_CANNOT_DELETE
);
export const PrimaryKeyCannotDeleteError = createErrorClass(
  'PrimaryKeyCannotDelete',
  ERROR_DEFINITIONS.PRIMARY_KEY_CANNOT_DELETE
);
export const ColumnTypeIncompatibleError = createErrorClass(
  'ColumnTypeIncompatible',
  ERROR_DEFINITIONS.COLUMN_TYPE_INCOMPATIBLE
);
export const UniqueSameAsPrimaryKeyError = createErrorClass(
  'UniqueSameAsPrimaryKey',
  ERROR_DEFINITIONS.UNIQUE_SAME_AS_PRIMARY_KEY
);
