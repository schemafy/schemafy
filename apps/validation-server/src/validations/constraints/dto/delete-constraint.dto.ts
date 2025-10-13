import type { Database, Constraint, Schema, Table } from '@schemafy/validator';

export interface DeleteConstraintDto {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    constraintId: Constraint['id'];
}
