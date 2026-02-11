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
import { ErdService } from './erd.service';
import type {
  ChangeSchemaNameRequest,
  ChangeTableExtraRequest,
  ChangeTableMetaRequest,
  ChangeTableNameRequest,
  CreateSchemaRequest,
  CreateTableRequest,
} from './erd.types';

@Controller('api/v1.0')
export class ErdController {
  constructor(private readonly erdService: ErdService) {}

  @Post('schemas')
  async createSchema(
    @Body() data: CreateSchemaRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.createSchema(data, authHeader);
  }

  @Get('schemas/:schemaId')
  async getSchema(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getSchema(schemaId, authHeader);
  }

  @Patch('schemas/:schemaId/name')
  async changeSchemaName(
    @Param('schemaId') schemaId: string,
    @Body() data: ChangeSchemaNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.changeSchemaName(schemaId, data, authHeader);
  }

  @Delete('schemas/:schemaId')
  async deleteSchema(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.deleteSchema(schemaId, authHeader);
  }

  @Post('tables')
  async createTable(
    @Body() data: CreateTableRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.createTable(data, authHeader);
  }

  @Get('tables/:tableId')
  async getTable(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getTable(tableId, authHeader);
  }

  @Get('schemas/:schemaId/tables')
  async getTablesBySchemaId(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getTablesBySchemaId(schemaId, authHeader);
  }

  @Get('tables/:tableId/snapshot')
  async getTableSnapshot(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getTableSnapshot(tableId, authHeader);
  }

  @Get('tables/snapshots')
  async getTableSnapshots(
    @Query('tableIds') tableIds: string[],
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getTableSnapshots(tableIds, authHeader);
  }

  @Patch('tables/:tableId/name')
  async changeTableName(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.changeTableName(tableId, data, authHeader);
  }

  @Patch('tables/:tableId/meta')
  async changeTableMeta(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableMetaRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.changeTableMeta(tableId, data, authHeader);
  }

  @Patch('tables/:tableId/extra')
  async changeTableExtra(
    @Param('tableId') tableId: string,
    @Body() data: ChangeTableExtraRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.changeTableExtra(tableId, data, authHeader);
  }

  @Delete('tables/:tableId')
  async deleteTable(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.deleteTable(tableId, authHeader);
  }

  @Get('schemas/:schemaId/snapshots')
  async getSchemaWithSnapshots(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.erdService.getSchemaWithSnapshots(schemaId, authHeader);
  }
}
