import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import type { ValidateResult } from '../common';
import { IndexesService } from './indexes.service';
import type {
  Database,
  Schema,
  Table,
  Index,
  IndexColumn,
} from '@schemafy/validator';

@Controller()
export class IndexesController {
  constructor(private readonly service: IndexesService) {}

  @GrpcMethod('ValidationService', 'CreateIndex')
  createIndex(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    index: Omit<Index, 'tableId'>;
  }): ValidateResult {
    const { database, schemaId, tableId, index } = req;
    return this.service.createIndex(database, schemaId, tableId, index);
  }

  @GrpcMethod('ValidationService', 'DeleteIndex')
  deleteIndex(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
  }): ValidateResult {
    const { database, schemaId, tableId, indexId } = req;
    return this.service.deleteIndex(database, schemaId, tableId, indexId);
  }

  @GrpcMethod('ValidationService', 'ChangeIndexName')
  changeIndexName(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
    newName: Index['name'];
  }): ValidateResult {
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
  addColumnToIndex(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
    indexColumn: Omit<IndexColumn, 'indexId'>;
  }): ValidateResult {
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
  removeColumnFromIndex(req: {
    database: Database;
    schemaId: Schema['id'];
    tableId: Table['id'];
    indexId: Index['id'];
    indexColumnId: IndexColumn['id'];
  }): ValidateResult {
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
