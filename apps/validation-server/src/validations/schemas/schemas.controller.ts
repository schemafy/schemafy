import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { SchemasService } from './schemas.service';

import type { ChangeSchemaNameDto, CreateSchemaDto, DeleteSchemaDto } from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class SchemasController {
  constructor(private readonly service: SchemasService) {}

  @GrpcMethod('ValidationService', 'CreateSchema')
  createSchema(req: CreateSchemaDto): ValidateResult {
    const { database, schema } = req;
    return this.service.createSchema(database, schema);
  }

  @GrpcMethod('ValidationService', 'DeleteSchema')
  deleteSchema(req: DeleteSchemaDto): ValidateResult {
    const { database, schemaId } = req;
    return this.service.deleteSchema(database, schemaId);
  }

  @GrpcMethod('ValidationService', 'ChangeSchemaName')
  changeSchemaName(req: ChangeSchemaNameDto): ValidateResult {
    const { database, schemaId, newName } = req;
    return this.service.changeSchemaName(database, schemaId, newName);
  }
}
