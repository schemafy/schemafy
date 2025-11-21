import { Injectable } from '@nestjs/common';
import { ERD_VALIDATOR } from '@schemafy/validator';

import { toErrorDetails } from '../common/error-mapper';

import type { ValidateResult } from '../common';
import type { Database, Column, Schema, Table } from '@schemafy/validator';

@Injectable()
export class ColumnsService {
  createColumn(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>,
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.createColumn(
        database,
        schemaId,
        tableId,
        column,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  deleteColumn(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.deleteColumn(
        database,
        schemaId,
        tableId,
        columnId,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnName(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newName: Column['name'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnName(
        database,
        schemaId,
        tableId,
        columnId,
        newName,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnType(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    dataType: Column['dataType'],
    lengthScale?: Column['lengthScale'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnType(
        database,
        schemaId,
        tableId,
        columnId,
        dataType,
        lengthScale,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }

  changeColumnPosition(
    database: Database,
    schemaId: Schema['id'],
    tableId: Table['id'],
    columnId: Column['id'],
    newPosition: Column['ordinalPosition'],
  ): ValidateResult {
    try {
      const updated = ERD_VALIDATOR.changeColumnPosition(
        database,
        schemaId,
        tableId,
        columnId,
        newPosition,
      );
      return { success: { database: updated } };
    } catch (err) {
      return { failure: { errors: toErrorDetails(err) } };
    }
  }
}
