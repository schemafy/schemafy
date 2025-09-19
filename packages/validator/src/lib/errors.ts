// Error codes
export const ERROR_CODES = {
  SCHEMA_NOT_EXIST: 'SCHEMA_NOT_EXIST',
  SCHEMA_NAME_INVALID: 'SCHEMA_NAME_INVALID',
  SCHEMA_NAME_NOT_UNIQUE: 'SCHEMA_NAME_NOT_UNIQUE',
  SCHEMA_INVALID_STRUCTURE: 'SCHEMA_INVALID_STRUCTURE',
  TABLE_NOT_EXIST: 'TABLE_NOT_EXIST',
  TABLE_NAME_INVALID: 'TABLE_NAME_INVALID',
  TABLE_NOT_EXISTS: 'TABLE_NOT_EXISTS',
} as const;

export type ErrorCode = (typeof ERROR_CODES)[keyof typeof ERROR_CODES];

// Base error class for ERD validation
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

// Specific error classes
export class SchemaNotExistError extends ERDValidationError {
  constructor(schemaId: string) {
    super(ERROR_CODES.SCHEMA_NOT_EXIST, `Schema with ID '${schemaId}' does not exist`, { schemaId });
    this.name = 'SchemaNotExistError';
  }
}

export class SchemaNameInvalidError extends ERDValidationError {
  constructor(name: string, minLength = 3, maxLength = 20) {
    super(
      ERROR_CODES.SCHEMA_NAME_INVALID,
      `Schema name '${name}' is invalid. Name must be between ${minLength} and ${maxLength} characters`,
      { name, minLength, maxLength, actualLength: name.length }
    );
    this.name = 'SchemaNameInvalidError';
  }
}

export class SchemaNameNotUniqueError extends ERDValidationError {
  constructor(name: string, existingSchemaId: string) {
    super(ERROR_CODES.SCHEMA_NAME_NOT_UNIQUE, `Schema name '${name}' already exists. Schema names must be unique`, {
      name,
      existingSchemaId,
    });
    this.name = 'SchemaNameNotUniqueError';
  }
}

export class SchemaInvalidStructureError extends ERDValidationError {
  constructor(schemaId: string) {
    super(ERROR_CODES.SCHEMA_INVALID_STRUCTURE, `Schema with ID '${schemaId}' has invalid structure`, { schemaId });
    this.name = 'SchemaInvalidStructureError';
  }
}

//table errors
export class TableNotExistError extends ERDValidationError {
  constructor(tableId: string) {
    super(ERROR_CODES.TABLE_NOT_EXIST, `Table with ID '${tableId}' does not exist`, { tableId });
    this.name = 'TableNotExistError';
  }
}

export class TableNameInvalidError extends ERDValidationError {
  constructor(name: string) {
    super(ERROR_CODES.TABLE_NAME_INVALID, `Table name '${name}' is invalid. Name must be between 3 and 20 characters`, {
      name,
    });
    this.name = 'TableNameInvalidError';
  }
}

export class TableNotExistsError extends ERDValidationError {
  constructor(name: string) {
    super(ERROR_CODES.TABLE_NOT_EXISTS, `Table name '${name}' is not exists`, { name });
    this.name = 'TableNotExistsError';
  }
}

// Legacy constants for backward compatibility
export const SCHEMA_NOT_EXIST = 'Schema does not exist';
export const SCHEMA_NAME_INVALID = 'Schema name must be between 3 and 20 characters';
export const SCHEMA_NAME_NOT_UNIQUE = 'Schema name must be unique';
export const SCHEMA_INVALID_STRUCTURE = 'Schema has invalid structure';
export const TABLE_NOT_EXIST = 'Table does not exist';
export const TABLE_NAME_INVALID = 'Table name must be between 3 and 20 characters';
export const TABLE_NOT_EXISTS = 'Table does not exist';
