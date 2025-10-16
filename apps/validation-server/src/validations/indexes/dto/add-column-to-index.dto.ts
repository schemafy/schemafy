import type {
  Database,
  Index,
  IndexColumn,
  Schema,
  Table,
} from '@schemafy/validator';

export interface AddColumnToIndexDto {
  database: Database;
  schemaId: Schema['id'];
  tableId: Table['id'];
  indexId: Index['id'];
  indexColumn: IndexColumn;
}
