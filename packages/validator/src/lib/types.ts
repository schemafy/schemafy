import { z } from 'zod';

const ULID = z.string().ulid();
const DB_VENDOR = z.enum(['mysql']);
const INDEX_TYPE = z.enum(['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL', 'OTHER']);
const INDEX_SORT_DIR = z.enum(['ASC', 'DESC']);
const CONSTRAINT_KIND = z.enum(['PRIMARYKEY', 'UNIQUE', 'CHECK', 'DEFAULT', 'NOT_NULL']);
const RELATIONSHIP_KIND = z.enum(['IDENTIFYING', 'NONIDENTIFYING']);
const RELATIONSHIP_CARDINALITY = z.enum(['1:1', '1:N']);
const REALTIONSHIP_ON_DELETE = z.enum(['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT']);
const REALTIONSHIP_ON_UPDATE = z.enum(['NO_ACTION', 'RESTRICT', 'CASCADE', 'SET_NULL', 'SET_DEFAULT']);

export const SCHEMA = z.object({
  id: ULID,
  projectId: ULID,
  dbVendorId: DB_VENDOR,
  name: z.string().min(3).max(20),
  charset: z.string(),
  collation: z.string(),
  vendorOption: z.string(),
  createdAt: z.date(),
  updatedAt: z.date(),
  deletedAt: z.date().nullable().optional(),
});

export const DATABASE = z.object({
  id: ULID,
  projects: z.array(SCHEMA),
});

export const TABLE = z.object({
  id: ULID,
  schemaId: ULID,
  name: z.string().min(3).max(20),
  comment: z.string().nullable().optional(),
  tableOptions: z.string(),
  createdAt: z.date(),
  updatedAt: z.date(),
  deletedAt: z.date().nullable().optional(),
});

export const COLUMN = z.object({
  id: ULID,
  tableId: ULID,
  name: z.string().min(3).max(40),
  ordinalPosition: z.number().positive(),
  dataType: z.string(),
  lengthScale: z.string(),
  isAutoIncrement: z.boolean(),
  charset: z.string(),
  collation: z.string(),
  comment: z.string().nullable().optional(),
  createdAt: z.date(),
  updatedAt: z.date(),
  deletedAt: z.date().nullable().optional(),
});

export const INDEX = z.object({
  id: ULID,
  tableId: ULID,
  name: z.string(),
  type: INDEX_TYPE,
  comment: z.string().nullable().optional(),
});

export const INDEX_COLUMN = z.object({
  id: ULID,
  indexId: ULID,
  columnId: ULID,
  seqNo: z.number().positive(),
  sortDir: INDEX_SORT_DIR,
});

export const CONSTRAINT = z
  .object({
    id: ULID,
    tableId: ULID,
    name: z.string(),
    kind: CONSTRAINT_KIND,
    checkExpr: z.string().nullable().optional(),
    defaultExpr: z.string().nullable().optional(),
  })
  .refine((data) => {
    if (data.kind == 'CHECK') {
      return typeof data.checkExpr === 'string';
    } else if (data.kind == 'DEFAULT') {
      return typeof data.defaultExpr === 'string';
    }

    return true;
  });

export const CONSTRAINT_COLUMN = z.object({
  id: ULID,
  constraintId: ULID,
  columnId: ULID,
  seqNo: z.number().positive(),
});

export const RELATIONSHIP = z.object({
  id: ULID,
  srcTableId: ULID,
  tgtTableId: ULID,
  name: z.string(),
  kind: RELATIONSHIP_KIND,
  cardinality: RELATIONSHIP_CARDINALITY,
  onDelete: REALTIONSHIP_ON_DELETE,
  onUpdate: REALTIONSHIP_ON_UPDATE,
  fkEnforced: z.literal(false),
});

export const RELATIONSHIP_COLUMN = z.object({
  id: ULID,
  relationshipId: ULID,
  srcColumnId: ULID,
  tgtColumnId: ULID,
  seqNo: z.number().positive(),
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
