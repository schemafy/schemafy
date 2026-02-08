import { ERROR_CODES } from './codes';
import { createErrorClass } from './base';

export const RelationshipNotExistError = createErrorClass(
  'RelationshipNotExist',
  {
    code: ERROR_CODES.RELATIONSHIP_NOT_EXIST,
    messageTemplate: "Relationship with ID '{0}' does not exist",
    createDetails: (relationshipId: string) => ({ relationshipId }),
  },
);

export const RelationshipEmptyError = createErrorClass('RelationshipEmpty', {
  code: ERROR_CODES.RELATIONSHIP_EMPTY_ERROR,
  messageTemplate: "Relationship '{0}' must have at least one column mapping",
  createDetails: (relationshipName: string) => ({ relationshipName }),
});

export const RelationshipNameNotUniqueError = createErrorClass(
  'RelationshipNameNotUnique',
  {
    code: ERROR_CODES.RELATIONSHIP_NAME_NOT_UNIQUE,
    messageTemplate:
      "Relationship name '{0}' must be unique within table '{1}'",
    createDetails: (relationshipName: string, tableId: string) => ({
      relationshipName,
      tableId,
    }),
  },
);

export const RelationshipColumnNotExistError = createErrorClass(
  'RelationshipColumnNotExist',
  {
    code: ERROR_CODES.RELATIONSHIP_COLUNN_NOT_EXIST,
    messageTemplate:
      "Relationship column with ID '{0}' does not exist in relationship '{1}'",
    createDetails: (relationshipColumnId: string, relationshipId: string) => ({
      relationshipColumnId,
      relationshipId,
    }),
  },
);

export const RelationshipCyclicReferenceError = createErrorClass(
  'RelationshipCyclicReference',
  {
    code: ERROR_CODES.RELATIONSHIP_CYCLIC_REFERENCE,
    messageTemplate:
      'Direct cyclic reference detected between tables: {0} <-> {1}',
    createDetails: (table1: string, table2: string) => ({ table1, table2 }),
  },
);

export const RelationshipTargetTableNotExistError = createErrorClass(
  'RelationshipTargetTableNotExist',
  {
    code: ERROR_CODES.RELATIONSHIP_TARGET_TABLE_NOT_EXIST,
    messageTemplate: "Target table '{1}' does not exist for relationship '{0}'",
    createDetails: (relationshipName: string, targetTableId: string) => ({
      relationshipName,
      targetTableId,
    }),
  },
);
