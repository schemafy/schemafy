import { Injectable } from '@nestjs/common';
import type { ValidateResult } from '../common';
import { toErrorDetails } from '../common/error-mapper';
import type { Database, Table, Schema } from '@schemafy/validator';
import { ERD_VALIDATOR } from '@schemafy/validator';

@Injectable()
export class TablesService {
  createTable(
    database: Database,
    schemaId: Schema['id'],
    table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createTable(database, schemaId, table);
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteTable(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteTable(database, schemaId, tableId);
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeTableName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    newName: Table['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeTableName(
        database,
        schemaId,
        tableId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
