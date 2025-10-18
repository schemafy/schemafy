import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';

import { ColumnsService } from './columns.service';

import type {
  ChangeColumnNameDto,
  ChangeColumnNullableDto,
  ChangeColumnPositionDto,
  ChangeColumnTypeDto,
  CreateColumnDto,
  DeleteColumnDto,
} from './dto';
import type { ValidateResult } from '../common';

@Controller()
export class ColumnsController {
  constructor(private readonly service: ColumnsService) {}

  @GrpcMethod('ValidationService', 'CreateColumn')
  createColumn(req: CreateColumnDto): ValidateResult {
    const { database, schemaId, tableId, column } = req;
    return this.service.createColumn(database, schemaId, tableId, column);
  }

  @GrpcMethod('ValidationService', 'DeleteColumn')
  deleteColumn(req: DeleteColumnDto): ValidateResult {
    const { database, schemaId, tableId, columnId } = req;
    return this.service.deleteColumn(database, schemaId, tableId, columnId);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnName')
  changeColumnName(req: ChangeColumnNameDto): ValidateResult {
    const { database, schemaId, tableId, columnId, newName } = req;
    return this.service.changeColumnName(database, schemaId, tableId, columnId, newName);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnType')
  changeColumnType(req: ChangeColumnTypeDto): ValidateResult {
    const { database, schemaId, tableId, columnId, dataType, lengthScale } = req;
    return this.service.changeColumnType(database, schemaId, tableId, columnId, dataType, lengthScale);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnPosition')
  changeColumnPosition(req: ChangeColumnPositionDto): ValidateResult {
    const { database, schemaId, tableId, columnId, newPosition } = req;
    return this.service.changeColumnPosition(database, schemaId, tableId, columnId, newPosition);
  }

  @GrpcMethod('ValidationService', 'ChangeColumnNullable')
  changeColumnNullable(req: ChangeColumnNullableDto): ValidateResult {
    const { database, schemaId, tableId, columnId, nullable } = req;
    return this.service.changeColumnNullable(database, schemaId, tableId, columnId, nullable);
  }
}
