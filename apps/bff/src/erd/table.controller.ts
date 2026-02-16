import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { TableService } from './table.service';
import type {
  ChangeTableExtraRequest,
  ChangeTableMetaRequest,
  ChangeTableNameRequest,
  CreateTableRequest,
} from './erd.types';

@Controller('api/v1.0')
export class TableController {
  constructor(private readonly tableService: TableService) {}

  @Post('tables')
  async createTable(
    @Body() data: CreateTableRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.createTable(data, authHeader);
  }

  @Get('tables/:tableId')
  async getTable(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.getTable(tableId, authHeader);
  }

  @Get('schemas/:schemaId/tables')
  async getTablesBySchemaId(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.getTablesBySchemaId(schemaId, authHeader);
  }

  @Get('tables/:tableId/snapshot')
  async getTableSnapshot(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.getTableSnapshot(tableId, authHeader);
  }

  @Get('tables/snapshots')
  async getTableSnapshots(
    @Query('tableIds') tableIds: string[],
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.getTableSnapshots(tableIds, authHeader);
  }

  @Patch('tables/:tableId/name')
  async changeTableName(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.changeTableName(tableId, data, authHeader);
  }

  @Patch('tables/:tableId/meta')
  async changeTableMeta(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableMetaRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.changeTableMeta(tableId, data, authHeader);
  }

  @Patch('tables/:tableId/extra')
  async changeTableExtra(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableExtraRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.changeTableExtra(tableId, data, authHeader);
  }

  @Delete('tables/:tableId')
  async deleteTable(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.deleteTable(tableId, authHeader);
  }

  @Get('schemas/:schemaId/snapshots')
  async getSchemaWithSnapshots(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.tableService.getSchemaWithSnapshots(schemaId, authHeader);
  }
}
