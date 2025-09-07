import { z } from 'zod';

const ULID = z.string().ulid();
const DB_VENDOR = z.enum(['mysql']);

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

export type Schema = z.infer<typeof SCHEMA>;

export const DATABASE = z.object({
  id: ULID,
  projects: z.array(SCHEMA),
});

export type Database = z.infer<typeof DATABASE>;
