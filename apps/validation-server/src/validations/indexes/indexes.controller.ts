import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { IndexesService } from './indexes.service';

import type { ValidateResult } from '../common';
import type {
  AddColumnToIndexRequest,
  ChangeIndexNameRequest,
  CreateIndexRequest,
  DeleteIndexRequest,
  RemoveColumnFromIndexRequest,
} from '../../types/validation.types';

@Controller()
export class IndexesController {
  constructor(private readonly service: IndexesService) {}

  @GrpcMethod('ValidationService', 'CreateIndex')
  createIndex(req: CreateIndexRequest): ValidateResult {
    const { database, schemaId, tableId, index } = req;
    return this.service.createIndex(database, schemaId, tableId, index);
  }

  @GrpcMethod('ValidationService', 'DeleteIndex')
  deleteIndex(req: DeleteIndexRequest): ValidateResult {
    const { database, schemaId, tableId, indexId } = req;
    return this.service.deleteIndex(database, schemaId, tableId, indexId);
  }

  @GrpcMethod('ValidationService', 'ChangeIndexName')
  changeIndexName(req: ChangeIndexNameRequest): ValidateResult {
    const { database, schemaId, tableId, indexId, newName } = req;
    return this.service.changeIndexName(
      database,
      schemaId,
      tableId,
      indexId,
      newName,
    );
  }

  @GrpcMethod('ValidationService', 'AddColumnToIndex')
  addColumnToIndex(req: AddColumnToIndexRequest): ValidateResult {
    const { database, schemaId, tableId, indexId, indexColumn } = req;
    return this.service.addColumnToIndex(
      database,
      schemaId,
      tableId,
      indexId,
      indexColumn,
    );
  }

  @GrpcMethod('ValidationService', 'RemoveColumnFromIndex')
  removeColumnFromIndex(req: RemoveColumnFromIndexRequest): ValidateResult {
    const { database, schemaId, tableId, indexId, indexColumnId } = req;
    return this.service.removeColumnFromIndex(
      database,
      schemaId,
      tableId,
      indexId,
      indexColumnId,
    );
  }
}
