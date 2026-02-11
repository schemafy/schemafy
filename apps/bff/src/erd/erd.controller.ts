import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { ErdService } from './erd.service';
import type {
  ChangeSchemaNameRequest,
  CreateSchemaRequest,
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
}
