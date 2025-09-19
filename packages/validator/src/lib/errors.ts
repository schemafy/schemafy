// Error codes
export const ERROR_CODES = {
  SCHEMA_NOT_EXIST: 'SCHEMA_NOT_EXIST',
  SCHEMA_NAME_INVALID: 'SCHEMA_NAME_INVALID',
  SCHEMA_NAME_NOT_UNIQUE: 'SCHEMA_NAME_NOT_UNIQUE',
  SCHEMA_ALREADY_DELETED: 'SCHEMA_ALREADY_DELETED',
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

export class SchemaAlreadyDeletedError extends ERDValidationError {
  constructor(schemaId: string) {
    super(ERROR_CODES.SCHEMA_ALREADY_DELETED, `Schema with ID '${schemaId}' has already been deleted`, { schemaId });
    this.name = 'SchemaAlreadyDeletedError';
  }
}

// Legacy constants for backward compatibility
export const SCHEMA_NOT_EXIST = 'Schema does not exist';
export const SCHEMA_NAME_INVALID = 'Schema name must be between 3 and 20 characters';
export const SCHEMA_NAME_NOT_UNIQUE = 'Schema name must be unique';
export const SCHEMA_ALREADY_DELETED = 'Schema has already been deleted';
