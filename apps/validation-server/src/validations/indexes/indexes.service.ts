import { Injectable } from '@nestjs/common';
import type { ValidateResult } from '../common';
import { toErrorDetails } from '../common/error-mapper';
import type {
  Database,
  Index,
  IndexColumn,
  Table,
  Schema,
} from '@schemafy/validator';
import { ERD_VALIDATOR } from '@schemafy/validator';

@Injectable()
export class IndexesService {
  createIndex(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    index: Omit<Index, 'tableId'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createIndex(
        database,
        schemaId,
        tableId,
        index,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteIndex(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteIndex(
        database,
        schemaId,
        tableId,
        indexId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeIndexName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    newName: Index['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeIndexName(
        database,
        schemaId,
        tableId,
        indexId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  addColumnToIndex(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumn: Omit<IndexColumn, 'indexId'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.addColumnToIndex(
        database,
        schemaId,
        tableId,
        indexId,
        indexColumn,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  removeColumnFromIndex(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    indexId: Index['id'],
    indexColumnId: IndexColumn['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.removeColumnFromIndex(
        database,
        schemaId,
        tableId,
        indexId,
        indexColumnId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
