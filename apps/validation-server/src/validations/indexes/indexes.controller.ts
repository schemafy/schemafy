import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { IndexesService } from './indexes.service';

import type {
  AddColumnToIndexDto,
  ChangeIndexNameDto,
  CreateIndexDto,
  DeleteIndexDto,
  RemoveColumnFromIndexDto,
} from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class IndexesController {
  constructor(private readonly service: IndexesService) {}

  @GrpcMethod('ValidationService', 'CreateIndex')
  createIndex(req: CreateIndexDto): ValidateResult {
    const { database, schemaId, tableId, index } = req;
    return this.service.createIndex(database, schemaId, tableId, index);
  }

  @GrpcMethod('ValidationService', 'DeleteIndex')
  deleteIndex(req: DeleteIndexDto): ValidateResult {
    const { database, schemaId, tableId, indexId } = req;
    return this.service.deleteIndex(database, schemaId, tableId, indexId);
  }

  @GrpcMethod('ValidationService', 'ChangeIndexName')
  changeIndexName(req: ChangeIndexNameDto): ValidateResult {
    const { database, schemaId, tableId, indexId, newName } = req;
    return this.service.changeIndexName(database, schemaId, tableId, indexId, newName);
  }

  @GrpcMethod('ValidationService', 'AddColumnToIndex')
  addColumnToIndex(req: AddColumnToIndexDto): ValidateResult {
    const { database, schemaId, tableId, indexId, indexColumn } = req;
    return this.service.addColumnToIndex(database, schemaId, tableId, indexId, indexColumn);
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromIndex')
  removeColumnFromIndex(req: RemoveColumnFromIndexDto): ValidateResult {
    const { database, schemaId, tableId, indexId, indexColumnId } = req;
    return this.service.removeColumnFromIndex(database, schemaId, tableId, indexId, indexColumnId);
  }
}
