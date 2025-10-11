import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { TablesService } from './tables.service';

import type { ValidateResult } from '../common';
import type {
  ChangeTableNameRequest,
  CreateTableRequest,
  DeleteTableRequest,
} from '../../types/validation.types';

@Controller()
export class TablesController {
  constructor(private readonly service: TablesService) {}

  @GrpcMethod('ValidationService', 'CreateTable')
  createTable(req: CreateTableRequest): ValidateResult {
    const { database, schemaId, table } = req;
    return this.service.createTable(database, schemaId, table);
  }

  @GrpcMethod('ValidationService', 'DeleteTable')
  deleteTable(req: DeleteTableRequest): ValidateResult {
    const { database, schemaId, tableId } = req;
    return this.service.deleteTable(database, schemaId, tableId);
  }

  @GrpcMethod('ValidationService', 'ChangeTableName')
  changeTableName(req: ChangeTableNameRequest): ValidateResult {
    const { database, schemaId, tableId, newName } = req;
    return this.service.changeTableName(database, schemaId, tableId, newName);
  }
}
