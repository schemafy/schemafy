import { Injectable, Logger } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Index, IndexColumn, Table, Schema } from '@schemafy/validator';

@Injectable()
export class IndexesService {
  private readonly logger = new Logger(IndexesService.name);
  createIndex(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    index: Omit<Index, 'tableId'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createIndex(database, schemaId, tableId, index);
      this.logger.log(
        `CreateIndex request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, index: ${JSON.stringify(
          index,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `CreateIndex request failed, schemaId: ${schemaId}, tableId: ${tableId}, index: ${JSON.stringify(index)}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteIndex(database: Database, schemaId: Schema['id'], tableId: Table['id'], indexId: Index['id']): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteIndex(database, schemaId, tableId, indexId);
      this.logger.log(
        `DeleteIndex request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `DeleteIndex request failed, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}`,
        err,
      );
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
      const updated = ERD_VALIDATOR.changeIndexName(database, schemaId, tableId, indexId, newName);
      this.logger.log(
        `ChangeIndexName request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, newName: ${newName}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `ChangeIndexName request failed, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, newName: ${newName}`,
        err,
      );
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
      const updated = ERD_VALIDATOR.addColumnToIndex(database, schemaId, tableId, indexId, indexColumn);
      this.logger.log(
        `AddColumnToIndex request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, indexColumn: ${JSON.stringify(
          indexColumn,
        )}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `AddColumnToIndex request failed, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, indexColumn: ${JSON.stringify(
          indexColumn,
        )}`,
        err,
      );
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
      const updated = ERD_VALIDATOR.removeColumnFromIndex(database, schemaId, tableId, indexId, indexColumnId);
      this.logger.log(
        `RemoveColumnFromIndex request successfully validated, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, indexColumnId: ${indexColumnId}`,
      );
      return { success: { database: updated } };
    } catch (err) {
      this.logger.error(
        `RemoveColumnFromIndex request failed, schemaId: ${schemaId}, tableId: ${tableId}, indexId: ${indexId}, indexColumnId: ${indexColumnId}`,
        err,
      );
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
