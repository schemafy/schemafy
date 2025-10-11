import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { SchemasService } from './schemas.service';

import type { ValidateResult } from '../common';
import type {
  CreateSchemaRequest,
  DeleteSchemaRequest,
} from '../../types/validation.types';
import type { Database, Schema } from '@schemafy/validator';

@Controller()
export class SchemasController {
  constructor(private readonly service: SchemasService) {}

  @GrpcMethod('ValidationService', 'CreateSchema')
  createSchema(req: CreateSchemaRequest): ValidateResult {
    const { database, schema } = req;
    return this.service.createSchema(database, schema);
  }

  @GrpcMethod('ValidationService', 'DeleteSchema')
  deleteSchema(req: DeleteSchemaRequest): ValidateResult {
    const { database, schemaId } = req;
    return this.service.deleteSchema(database, schemaId);
  }

  @GrpcMethod('ValidationService', 'ChangeSchemaName')
  changeSchemaName(req: {
    database: Database;
    schemaId: Schema['id'];
    newName: Schema['name'];
  }): ValidateResult {
    const { database, schemaId, newName } = req;
    return this.service.changeSchemaName(database, schemaId, newName);
  }
}
