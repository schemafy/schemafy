import type { Database, Constraint, Schema, Table } from '@schemafy/validator';

export interface CreateConstraintDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraint: Constraint;
}
