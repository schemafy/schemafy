// Error codes
export const ERROR_CODES = {
  SCHEMA_NOT_EXIST: 'SCHEMA_NOT_EXIST',
  SCHEMA_NAME_INVALID: 'SCHEMA_NAME_INVALID',
  SCHEMA_NAME_NOT_UNIQUE: 'SCHEMA_NAME_NOT_UNIQUE',
  SCHEMA_INVALID_STRUCTURE: 'SCHEMA_INVALID_STRUCTURE',
  TABLE_NOT_EXIST: 'TABLE_NOT_EXIST',
  TABLE_NAME_INVALID: 'TABLE_NAME_INVALID',
  TABLE_NOT_EXISTS: 'TABLE_NOT_EXISTS',
  COLUMN_NOT_EXIST: 'COLUMN_NOT_EXIST',
  COLUMN_NAME_INVALID: 'COLUMN_NAME_INVALID',
  COLUMN_POSITION_INVALID: 'COLUMN_POSITION_INVALID',
  INDEX_NOT_EXIST: 'INDEX_NOT_EXIST',
  INDEX_NAME_INVALID: 'INDEX_NAME_INVALID',
  INDEX_COLUMN_NOT_EXIST: 'INDEX_COLUMN_NOT_EXIST',
  CONSTRAINT_NOT_EXIST: 'CONSTRAINT_NOT_EXIST',
  CONSTRAINT_NAME_INVALID: 'CONSTRAINT_NAME_INVALID',
  CONSTRAINT_COLUMN_NOT_EXIST: 'CONSTRAINT_COLUMN_NOT_EXIST',
  RELATIONSHIP_NOT_EXIST: 'RELATIONSHIP_NOT_EXIST',
  RELATIONSHIP_NAME_INVALID: 'RELATIONSHIP_NAME_INVALID',
  RELATIONSHIP_COLUMN_NOT_EXIST: 'RELATIONSHIP_COLUMN_NOT_EXIST',
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

// Column errors
export class ColumnNotExistError extends ERDValidationError {
  constructor(columnId: string) {
    super(ERROR_CODES.COLUMN_NOT_EXIST, `Column with ID '${columnId}' does not exist`, { columnId });
    this.name = 'ColumnNotExistError';
  }
}

export class ColumnNameInvalidError extends ERDValidationError {
  constructor(name: string) {
    super(
      ERROR_CODES.COLUMN_NAME_INVALID,
      `Column name '${name}' is invalid. Name must be between 3 and 40 characters`,
      {
        name,
      }
    );
    this.name = 'ColumnNameInvalidError';
  }
}

export class ColumnPositionInvalidError extends ERDValidationError {
  constructor(position: number, maxPosition: number) {
    super(
      ERROR_CODES.COLUMN_POSITION_INVALID,
      `Column position '${position}' is invalid. Position must be between 1 and ${maxPosition}`,
      {
        position,
        maxPosition,
      }
    );
    this.name = 'ColumnPositionInvalidError';
  }
}

// Index errors
export class IndexNotExistError extends ERDValidationError {
  constructor(indexId: string) {
    super(ERROR_CODES.INDEX_NOT_EXIST, `Index with ID '${indexId}' does not exist`, { indexId });
    this.name = 'IndexNotExistError';
  }
}

export class IndexNameInvalidError extends ERDValidationError {
  constructor(name: string) {
    super(ERROR_CODES.INDEX_NAME_INVALID, `Index name '${name}' is invalid`, { name });
    this.name = 'IndexNameInvalidError';
  }
}

export class IndexColumnNotExistError extends ERDValidationError {
  constructor(indexColumnId: string) {
    super(ERROR_CODES.INDEX_COLUMN_NOT_EXIST, `Index column with ID '${indexColumnId}' does not exist`, {
      indexColumnId,
    });
    this.name = 'IndexColumnNotExistError';
  }
}

// Constraint errors
export class ConstraintNotExistError extends ERDValidationError {
  constructor(constraintId: string) {
    super(ERROR_CODES.CONSTRAINT_NOT_EXIST, `Constraint with ID '${constraintId}' does not exist`, { constraintId });
    this.name = 'ConstraintNotExistError';
  }
}

export class ConstraintNameInvalidError extends ERDValidationError {
  constructor(name: string) {
    super(ERROR_CODES.CONSTRAINT_NAME_INVALID, `Constraint name '${name}' is invalid`, { name });
    this.name = 'ConstraintNameInvalidError';
  }
}

export class ConstraintColumnNotExistError extends ERDValidationError {
  constructor(constraintColumnId: string) {
    super(ERROR_CODES.CONSTRAINT_COLUMN_NOT_EXIST, `Constraint column with ID '${constraintColumnId}' does not exist`, {
      constraintColumnId,
    });
    this.name = 'ConstraintColumnNotExistError';
  }
}

// Relationship errors
export class RelationshipNotExistError extends ERDValidationError {
  constructor(relationshipId: string) {
    super(ERROR_CODES.RELATIONSHIP_NOT_EXIST, `Relationship with ID '${relationshipId}' does not exist`, {
      relationshipId,
    });
    this.name = 'RelationshipNotExistError';
  }
}

export class RelationshipNameInvalidError extends ERDValidationError {
  constructor(name: string) {
    super(ERROR_CODES.RELATIONSHIP_NAME_INVALID, `Relationship name '${name}' is invalid`, { name });
    this.name = 'RelationshipNameInvalidError';
  }
}

export class RelationshipColumnNotExistError extends ERDValidationError {
  constructor(relationshipColumnId: string) {
    super(
      ERROR_CODES.RELATIONSHIP_COLUMN_NOT_EXIST,
      `Relationship column with ID '${relationshipColumnId}' does not exist`,
      { relationshipColumnId }
    );
    this.name = 'RelationshipColumnNotExistError';
  }
}
