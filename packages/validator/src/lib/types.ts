import { z } from 'zod';

const ULID = z.string().ulid();
const DB_VENDOR = z.enum(['mysql']);
const INDEX_TYPE = z.enum(['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL', 'OTHER']);
const INDEX_SORT_DIR = z.enum(['ASC', 'DESC']);
const CONSTRAINT_KIND = z.enum(['PRIMARY_KEY', 'UNIQUE', 'CHECK', 'DEFAULT', 'NOT_NULL']);
const RELATIONSHIP_KIND = z.enum(['IDENTIFYING', 'NON_IDENTIFYING']);
const RELATIONSHIP_CARDINALITY = z.enum(['1:1', '1:N']);
const RELATIONSHIP_ON_DELETE = z.enum(['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT']);
const RELATIONSHIP_ON_UPDATE = z.enum(['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT']);

export const COLUMN = z.object({
  id: ULID,
  tableId: ULID,
  name: z.string().min(1).max(40),
  ordinalPosition: z.number().positive(),
  dataType: z.string().nullable().optional(),
  lengthScale: z.string(),
  isAutoIncrement: z.boolean(),
  charset: z.string(),
  collation: z.string(),
  comment: z.string().nullable().optional(),
  isAffected: z.boolean().default(false),
});

export const INDEX_COLUMN = z.object({
  id: ULID,
  indexId: ULID,
  columnId: ULID,
  seqNo: z.number().positive(),
  sortDir: INDEX_SORT_DIR,
  isAffected: z.boolean().default(false),
});

export const INDEX = z.object({
  id: ULID,
  tableId: ULID,
  name: z.string(),
  type: INDEX_TYPE,
  comment: z.string().nullable().optional(),
  columns: z.array(INDEX_COLUMN),
  isAffected: z.boolean().default(false),
});

export const CONSTRAINT_COLUMN = z.object({
  id: ULID,
  constraintId: ULID,
  columnId: ULID,
  seqNo: z.number().positive(),
  isAffected: z.boolean().default(false),
});

export const CONSTRAINT = z
  .object({
    id: ULID,
    tableId: ULID,
    name: z.string(),
    kind: CONSTRAINT_KIND,
    checkExpr: z.string().nullable().optional(),
    defaultExpr: z.string().nullable().optional(),
    columns: z.array(CONSTRAINT_COLUMN),
    isAffected: z.boolean().default(false),
  })
  .refine((data) => {
    if (data.kind == 'CHECK') {
      return typeof data.checkExpr === 'string';
    } else if (data.kind == 'DEFAULT') {
      return typeof data.defaultExpr === 'string';
    }

    return true;
  });

export const RELATIONSHIP_COLUMN = z.object({
  id: ULID,
  relationshipId: ULID,
  fkColumnId: ULID,
  refColumnId: ULID,
  seqNo: z.number().positive(),
  isAffected: z.boolean().default(false),
});

export const RELATIONSHIP = z.object({
  id: ULID,
  srcTableId: ULID,
  tgtTableId: ULID,
  name: z.string(),
  kind: RELATIONSHIP_KIND,
  cardinality: RELATIONSHIP_CARDINALITY,
  onDelete: RELATIONSHIP_ON_DELETE,
  onUpdate: RELATIONSHIP_ON_UPDATE,
  fkEnforced: z.literal(false),
  columns: z.array(RELATIONSHIP_COLUMN),
  isAffected: z.boolean().default(false),
});

export const TABLE = z.object({
  id: ULID,
  schemaId: ULID,
  name: z.string().min(1).max(20),
  comment: z.string().nullable().optional(),
  tableOptions: z.string(),
  columns: z.array(COLUMN),
  indexes: z.array(INDEX),
  constraints: z.array(CONSTRAINT),
  relationships: z.array(RELATIONSHIP),
  isAffected: z.boolean().default(false),
});

export const SCHEMA = z.object({
  id: ULID,
  projectId: ULID,
  dbVendorId: DB_VENDOR,
  name: z.string().min(1).max(20),
  charset: z.string(),
  collation: z.string(),
  vendorOption: z.string(),
  tables: z.array(TABLE),
  isAffected: z.boolean().default(false),
});

export const DATABASE = z.object({
  id: ULID,
  schemas: z.array(SCHEMA),
  isAffected: z.boolean().default(false),
});

export type Schema = z.infer<typeof SCHEMA>;
export type Database = z.infer<typeof DATABASE>;
export type Table = z.infer<typeof TABLE>;
export type Column = z.infer<typeof COLUMN>;
export type Index = z.infer<typeof INDEX>;
export type IndexColumn = z.infer<typeof INDEX_COLUMN>;
export type Constraint = z.infer<typeof CONSTRAINT>;
export type ConstraintColumn = z.infer<typeof CONSTRAINT_COLUMN>;
export type Relationship = z.infer<typeof RELATIONSHIP>;
export type RelationshipColumn = z.infer<typeof RELATIONSHIP_COLUMN>;
