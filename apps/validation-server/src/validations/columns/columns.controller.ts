import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ColumnsService } from './columns.service';

import type { ValidateResult } from '../common';
import type { Database, Column, Schema, Table } from '@schemafy/validator';

@Controller()
export class ColumnsController {
  constructor(private readonly service: ColumnsService) {}

  @GrpcMethod('ValidationService', 'CreateColumn')
  createColumn(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    column: Omit<Column, 'tableId' | 'createdAt' | 'updatedAt'>;
  }): ValidateResult {
    const { database, schemaId, tableId, column } = req;
    return this.service.createColumn(database, schemaId, tableId, column);
  }

  @GrpcMethod('ValidationService', 'DeleteColumn')
  deleteColumn(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
  }): ValidateResult {
    const { database, schemaId, tableId, columnId } = req;
    return this.service.deleteColumn(database, schemaId, tableId, columnId);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnName')
  changeColumnName(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
    newName: Column['name'];
  }): ValidateResult {
    const { database, schemaId, tableId, columnId, newName } = req;
    return this.service.changeColumnName(
      database,
      schemaId,
      tableId,
      columnId,
      newName,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeColumnType')
  changeColumnType(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
    dataType: Column['dataType'];
    lengthScale?: Column['lengthScale'];
  }): ValidateResult {
    const { database, schemaId, tableId, columnId, dataType, lengthScale } =
      req;
    return this.service.changeColumnType(
      database,
      schemaId,
      tableId,
      columnId,
      dataType,
      lengthScale,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeColumnPosition')
  changeColumnPosition(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
    newPosition: Column['ordinalPosition'];
  }): ValidateResult {
    const { database, schemaId, tableId, columnId, newPosition } = req;
    return this.service.changeColumnPosition(
      database,
      schemaId,
      tableId,
      columnId,
      newPosition,
    );
  }

  @GrpcMethod('ValidationService', 'ChangeColumnNullable')
  changeColumnNullable(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    columnId: Column['id'];
    nullable: boolean;
  }): ValidateResult {
    const { database, schemaId, tableId, columnId, nullable } = req;
    return this.service.changeColumnNullable(
      database,
      schemaId,
      tableId,
      columnId,
      nullable,
    );
  }
}
