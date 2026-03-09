import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { ColumnService } from './column.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { SessionId } from '../common/decorators/session-id.decorator';
import type {
  ChangeColumnMetaRequest,
  ChangeColumnNameRequest,
  ChangeColumnPositionRequest,
  ChangeColumnTypeRequest,
  CreateColumnRequest,
} from './erd.types';

@Controller('api/v1.0')
export class ColumnController {
  constructor(private readonly columnService: ColumnService) {}

  @Post('columns')
  async createColumn(
    @Body() data: CreateColumnRequest,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.createColumn(data, authHeader, sessionId);
  }

  @Get('columns/:columnId')
  async getColumn(
    @Param('columnId') columnId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.columnService.getColumn(columnId, authHeader);
  }

  @Get('tables/:tableId/columns')
  async getColumnsByTableId(
    @Param('tableId') tableId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.columnService.getColumnsByTableId(tableId, authHeader);
  }

  @Patch('columns/:columnId/name')
  async changeColumnName(
    @Param('columnId') columnId: string,
    @Body() data: ChangeColumnNameRequest,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.changeColumnName(
      columnId,
      data,
      authHeader,
      sessionId,
    );
  }

  @Patch('columns/:columnId/type')
  async changeColumnType(
    @Param('columnId') columnId: string,
    @Body() data: ChangeColumnTypeRequest,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.changeColumnType(
      columnId,
      data,
      authHeader,
      sessionId,
    );
  }

  @Patch('columns/:columnId/meta')
  async changeColumnMeta(
    @Param('columnId') columnId: string,
    @Body() data: ChangeColumnMetaRequest,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.changeColumnMeta(
      columnId,
      data,
      authHeader,
      sessionId,
    );
  }

  @Patch('columns/:columnId/position')
  async changeColumnPosition(
    @Param('columnId') columnId: string,
    @Body() data: ChangeColumnPositionRequest,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.changeColumnPosition(
      columnId,
      data,
      authHeader,
      sessionId,
    );
  }

  @Delete('columns/:columnId')
  async deleteColumn(
    @Param('columnId') columnId: string,
    @AuthHeader() authHeader: string,
    @SessionId() sessionId?: string,
  ) {
    return this.columnService.deleteColumn(columnId, authHeader, sessionId);
  }
}
