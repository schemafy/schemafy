import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ColumnsService } from './columns.service';

import type { ValidateResult } from '../common';
import type {
  ChangeColumnNameRequest,
  ChangeColumnNullableRequest,
  ChangeColumnPositionRequest,
  ChangeColumnTypeRequest,
  CreateColumnRequest,
  DeleteColumnRequest,
} from '../../types/validation.types';

@Controller()
export class ColumnsController {
  constructor(private readonly service: ColumnsService) {}

  @GrpcMethod('ValidationService', 'CreateColumn')
  createColumn(req: CreateColumnRequest): ValidateResult {
    const { database, schemaId, tableId, column } = req;
    return this.service.createColumn(database, schemaId, tableId, column);
  }

  @GrpcMethod('ValidationService', 'DeleteColumn')
  deleteColumn(req: DeleteColumnRequest): ValidateResult {
    const { database, schemaId, tableId, columnId } = req;
    return this.service.deleteColumn(database, schemaId, tableId, columnId);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnName')
  changeColumnName(req: ChangeColumnNameRequest): ValidateResult {
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
  changeColumnType(req: ChangeColumnTypeRequest): ValidateResult {
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
  changeColumnPosition(req: ChangeColumnPositionRequest): ValidateResult {
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
  changeColumnNullable(req: ChangeColumnNullableRequest): ValidateResult {
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
