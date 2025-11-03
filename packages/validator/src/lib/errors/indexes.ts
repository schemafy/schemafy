import { ERROR_CODES } from "./codes";
import { createErrorClass } from "./base";

export const IndexParseInvalidError = createErrorClass("IndexParseInvalid", {
  code: ERROR_CODES.INDEX_PARSE_INVALID,
  messageTemplate: "Invalid index: {0}",
  createDetails: (reason: string) => ({ reason }),
});

export const IndexNameNotUniqueError = createErrorClass("IndexNameNotUnique", {
  code: ERROR_CODES.INDEX_NAME_NOT_UNIQUE,
  messageTemplate:
    "Index name '{0}' already exists in the table. Index names must be unique within a table.",
  createDetails: (name: string, tableId: string) => ({ name, tableId }),
});

export const IndexTypeInvalidError = createErrorClass("IndexTypeInvalid", {
  code: ERROR_CODES.INDEX_TYPE_INVALID,
  messageTemplate:
    "Index type '{0}' is not valid for the database vendor '{1}'.",
  createDetails: (type: string, dbVendor: string) => ({ type, dbVendor }),
});

export const IndexColumnNotUniqueError = createErrorClass(
  "IndexColumnNotUnique",
  {
    code: ERROR_CODES.INDEX_COLUMN_NOT_UNIQUE,
    messageTemplate: "Column '{0}' is already included in the index '{1}'.",
    createDetails: (columnName: string, indexName: string) => ({
      columnName,
      indexName,
    }),
  },
);

export const DuplicateIndexDefinitionError = createErrorClass(
  "DuplicateIndexDefinition",
  {
    code: ERROR_CODES.DUPLICATE_INDEX_DEFINITION,
    messageTemplate:
      "Index '{0}' has the same definition as existing index '{1}'.",
    createDetails: (indexName: string, existingIndexName: string) => ({
      indexName,
      existingIndexName,
    }),
  },
);

export const IndexColumnNotExistError = createErrorClass(
  "IndexColumnNotExist",
  {
    code: ERROR_CODES.INDEX_COLUMN_NOT_EXIST,
    messageTemplate:
      "Column '{0}' specified in index '{1}' does not exist in the table.",
    createDetails: (columnName: string, indexName: string) => ({
      columnName,
      indexName,
    }),
  },
);

export const IndexColumnSeqError = createErrorClass("IndexColumnSeq", {
  code: ERROR_CODES.INDEX_COLUMN_SEQ_ERROR,
  messageTemplate:
    "Column sequence in index '{0}' must be consecutive starting from 1.",
  createDetails: (indexName: string) => ({ indexName }),
});

export const IndexColumnSortDirInvalidError = createErrorClass(
  "IndexColumnSortDirInvalid",
  {
    code: ERROR_CODES.INDEX_COLUMN_SORT_DIR_INVALID,
    messageTemplate:
      "Sort direction '{0}' is invalid for index column in index '{1}'. Use 'ASC' or 'DESC'.",
    createDetails: (sortDir: string, indexName: string) => ({
      sortDir,
      indexName,
    }),
  },
);

export const IndexNotExistError = createErrorClass("IndexNotExist", {
  code: ERROR_CODES.INDEX_NOT_EXIST,
  messageTemplate: "Index '{0}' does not exist in table '{1}'",
  createDetails: (indexId: string, tableId: string) => ({ indexId, tableId }),
});
