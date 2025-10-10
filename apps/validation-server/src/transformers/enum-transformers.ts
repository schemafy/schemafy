export type ZodDbVendor = 'mysql';
export type ZodOnAction =
  | 'NO_ACTION'
  | 'RESTRICT'
  | 'CASCADE'
  | 'SET_NULL'
  | 'SET_DEFAULT';
export type ZodRelationshipCardinality = '1:1' | '1:N';

export const toZodDbVendor = (value: string): ZodDbVendor => {
  const normalized = value.toLowerCase();
  if (normalized === 'mysql') return 'mysql';
  throw new Error(`Unsupported DbVendor: ${value}`);
};

export const normalizeOnUpdate = (value: string): ZodOnAction => {
  const withoutSuffix = value.replace(/_UPDATE$/, '');
  if (isZodOnAction(withoutSuffix)) return withoutSuffix;
  throw new Error(`Unsupported OnUpdate: ${value}`);
};

export const toZodOnDelete = (value: string): ZodOnAction => {
  if (isZodOnAction(value)) return value;
  throw new Error(`Unsupported OnDelete: ${value}`);
};

export const toZodRelationshipCardinality = (
  value: string,
): ZodRelationshipCardinality => {
  if (/^ONE_TO_(ONE|MANY)$/.test(value)) {
    const rhs = value.endsWith('ONE') ? '1' : 'N';
    return `1:${rhs}` as ZodRelationshipCardinality;
  }
  throw new Error(`Unsupported RelationshipCardinality: ${value}`);
};

const isZodOnAction = (value: string): value is ZodOnAction => {
  return (
    value === 'NO_ACTION' ||
    value === 'RESTRICT' ||
    value === 'CASCADE' ||
    value === 'SET_NULL' ||
    value === 'SET_DEFAULT'
  );
};
