import { ERROR_CODES } from './codes';
import { createErrorClass } from './base';

export const RelationshipColumnSeqError = createErrorClass('RelationshipColumnSeq', {
  code: ERROR_CODES.RELATIONSHIP_COLUMN_SEQ_ERROR,
  messageTemplate: "Column sequence in relationship '{0}' must be consecutive starting from 1.",
  createDetails: (relationshipName: string) => ({ relationshipName }),
});

export const IdentifyingRelationshipOrderError = createErrorClass('IdentifyingRelationshipOrder', {
  code: ERROR_CODES.IDENTIFYING_RELATIONSHIP_ORDER_ERROR,
  messageTemplate: "Column order mismatch in identifying relationship '{0}': {1}",
  createDetails: (relationshipName: string, reason: string) => ({
    relationshipName,
    reason,
  }),
});

export const CompositeKeyOrderMismatchError = createErrorClass('CompositeKeyOrderMismatch', {
  code: ERROR_CODES.COMPOSITE_KEY_ORDER_MISMATCH,
  messageTemplate: "Column order issue in {0} '{1}': {2}",
  createDetails: (keyType: string, keyName: string, reason: string) => ({
    keyType,
    keyName,
    reason,
  }),
});

export const RelationshipNotExistError = createErrorClass('RelationshipNotExist', {
  code: ERROR_CODES.RELATIONSHIP_NOT_EXIST,
  messageTemplate: "Relationship with ID '{0}' does not exist",
  createDetails: (relationshipId: string) => ({ relationshipId }),
});

export const RelationshipEmptyError = createErrorClass('RelationshipEmpty', {
  code: ERROR_CODES.RELATIONSHIP_EMPTY_ERROR,
  messageTemplate: "Relationship '{0}' must have at least one column mapping",
  createDetails: (relationshipName: string) => ({ relationshipName }),
});

export const RelationshipNameNotUniqueError = createErrorClass('RelationshipNameNotUnique', {
  code: ERROR_CODES.RELATIONSHIP_NAME_NOT_UNIQUE,
  messageTemplate: "Relationship name '{0}' must be unique within table '{1}'",
  createDetails: (relationshipName: string, tableId: string) => ({
    relationshipName,
    tableId,
  }),
});

export const RelationshipColumnNotExistError = createErrorClass('RelationshipColumnNotExist', {
  code: ERROR_CODES.RELATIONSHIP_COLUNN_NOT_EXIST,
  messageTemplate: "Relationship column with ID '{0}' does not exist in relationship '{1}'",
  createDetails: (relationshipColumnId: string, relationshipId: string) => ({
    relationshipColumnId,
    relationshipId,
  }),
});

export const RelationshipColumnMappingDuplicateError = createErrorClass('RelationshipColumnMappingDuplicate', {
  code: ERROR_CODES.RELATIONSHIP_COLUMN_MAPPING_DUPLICATE,
  messageTemplate: "Column mapping already exists in relationship '{0}': {1} -> {2}",
  createDetails: (relationshipName: string, srcColumnId: string, tgtColumnId: string) => ({
    relationshipName,
    srcColumnId,
    tgtColumnId,
  }),
});

export const RelationshipColumnTypeIncompatibleError = createErrorClass('RelationshipColumnTypeIncompatible', {
  code: ERROR_CODES.RELATIONSHIP_COLUMN_TYPE_INCOMPATIBLE,
  messageTemplate: "Related columns must have compatible data types in relationship '{0}': {1} <-> {2}",
  createDetails: (relationshipName: string, srcColumn: string, tgtColumn: string) => ({
    relationshipName,
    srcColumn,
    tgtColumn,
  }),
});

export const RelationshipDeleteSetNullError = createErrorClass('RelationshipDeleteSetNull', {
  code: ERROR_CODES.RELATIONSHIP_DELETE_SET_NULL_ERROR,
  messageTemplate: "ON DELETE SET NULL requires nullable foreign key columns in relationship '{0}': {1}",
  createDetails: (relationshipName: string, nonNullableColumns: string[]) => ({
    relationshipName,
    nonNullableColumns,
  }),
});

export const RelationshipCyclicReferenceError = createErrorClass('RelationshipCyclicReference', {
  code: ERROR_CODES.RELATIONSHIP_CYCLIC_REFERENCE,
  messageTemplate: 'Direct cyclic reference detected between tables: {0} <-> {1}',
  createDetails: (table1: string, table2: string) => ({ table1, table2 }),
});

export const RelationshipTargetTableNotExistError = createErrorClass('RelationshipTargetTableNotExist', {
  code: ERROR_CODES.RELATIONSHIP_TARGET_TABLE_NOT_EXIST,
  messageTemplate: "Target table '{1}' does not exist for relationship '{0}'",
  createDetails: (relationshipName: string, targetTableId: string) => ({
    relationshipName,
    targetTableId,
  }),
});
