import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';
import { TablesService } from './tables.service';
import type { Database, Table, Schema } from '@schemafy/validator';

@Controller()
export class TablesController {
  constructor(private readonly service: TablesService) {}

  @GrpcMethod('ValidationService', 'CreateTable')
  createTable(req: {
    database: Database;
    schemaId: Schema['id'];
    table: Omit<Table, 'schemaId' | 'createdAt' | 'updatedAt'>;
  }): ValidateResult {
    const { database, schemaId, table } = req;
    return this.service.createTable(database, schemaId, table);
  }

  @GrpcMethod('ValidationService', 'DeleteTable')
  deleteTable(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
  }): ValidateResult {
    const { database, schemaId, tableId } = req;
    return this.service.deleteTable(database, schemaId, tableId);
  }

  @GrpcMethod('ValidationService', 'ChangeTableName')
  changeTableName(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    newName: Table['name'];
  }): ValidateResult {
    const { database, schemaId, tableId, newName } = req;
    return this.service.changeTableName(database, schemaId, tableId, newName);
  }
}
