export type ZodDbVendor = 'mysql';
export type ZodOnAction = 'NO_ACTION' | 'RESTRICT' | 'CASCADE' | 'SET_NULL' | 'SET_DEFAULT';
export type ZodRelationshipCardinality = '1:1' | '1:N';
export type ZodIndexType = 'BTREE' | 'HASH' | 'FULLTEXT' | 'SPATIAL' | 'OTHER';
export type ZodIndexSortDir = 'ASC' | 'DESC';
export type ZodConstraintKind = 'PRIMARY_KEY' | 'UNIQUE' | 'CHECK' | 'DEFAULT' | 'NOT_NULL';
export type ZodRelationshipKind = 'IDENTIFYING' | 'NON_IDENTIFYING';

const createValidator = <T extends string>(
  allowedValues: readonly T[],
  typeName: string,
  options?: {
    is?: (value: string, allowedSet: Set<T>) => value is T;
    to?: (value: string, allowedSet: Set<T>) => T;
  },
) => {
  const allowedSet = new Set(allowedValues);

  const defaultIs = (value: string): value is T => allowedSet.has(value as T);
  const defaultTo = (value: string): T => {
    if (allowedSet.has(value as T)) return value as T;
    throw new Error(`Unsupported ${typeName}: ${value}`);
  };

  const isValidator = options?.is ?? defaultIs;
  const toValidator = options?.to ?? defaultTo;

  return {
    is: (value: string): value is T => isValidator(value, allowedSet),
    to: (value: string): T => toValidator(value, allowedSet),
  };
};

const ZodOnActionValidator = createValidator(
  ['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT'] as const,
  'OnAction',
);

const ZodOnUpdateValidator = createValidator(
  ['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT'] as const,
  'OnAction',
  {
    to: (value, allowedSet) => {
      const withoutSuffix = value.replace(/_UPDATE$/, '') as ZodOnAction;
      if (allowedSet.has(withoutSuffix)) return withoutSuffix;
      throw new Error(`Unsupported OnAction: ${value}`);
    },
  },
);

const ZodRelationshipCardinalityValidator = createValidator(['1:1', '1:N'] as const, 'RelationshipCardinality', {
  to: (value, allowedSet) => {
    if (/^ONE_TO_(ONE|MANY)$/.test(value)) {
      const rhs = value.endsWith('ONE') ? '1' : 'N';
      const result = `1:${rhs}` as ZodRelationshipCardinality;
      if (allowedSet.has(result)) return result;
    }
    throw new Error(`Unsupported RelationshipCardinality: ${value}`);
  },
});

const ZodIndexTypeValidator = createValidator(['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL', 'OTHER'] as const, 'IndexType');

const ZodIndexSortDirValidator = createValidator(['ASC', 'DESC'] as const, 'IndexSortDir');

const ZodConstraintKindValidator = createValidator(
  ['PRIMARY_KEY', 'UNIQUE', 'CHECK', 'DEFAULT', 'NOT_NULL'] as const,
  'ConstraintKind',
);

const ZodRelationshipKindValidator = createValidator(['IDENTIFYING', 'NON_IDENTIFYING'] as const, 'RelationshipKind');

const ZodDbVendorValidator = createValidator(['mysql'] as const, 'DbVendor', {
  to: (value, allowedSet) => {
    const normalized = value.toLowerCase() as ZodDbVendor;
    if (allowedSet.has(normalized)) return normalized;
    throw new Error(`Unsupported DbVendor: ${value}`);
  },
});

export const toZodDbVendor = ZodDbVendorValidator.to;

export const toZodOnDelete = ZodOnActionValidator.to;

export const normalizeOnUpdate = ZodOnUpdateValidator.to;

export const toZodRelationshipCardinality = ZodRelationshipCardinalityValidator.to;

export const toZodIndexType = ZodIndexTypeValidator.to;

export const toZodIndexSortDir = ZodIndexSortDirValidator.to;

export const toZodConstraintKind = ZodConstraintKindValidator.to;

export const toZodRelationshipKind = ZodRelationshipKindValidator.to;
