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
import { SchemaService } from './schema.service';
import type { ChangeSchemaNameRequest, CreateSchemaRequest } from './erd.types';

@Controller('api/v1.0')
export class SchemaController {
  constructor(private readonly schemaService: SchemaService) {}

  @Post('schemas')
  async createSchema(
    @Body() data: CreateSchemaRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.schemaService.createSchema(data, authHeader);
  }

  @Get('projects/:projectId/schemas')
  async getSchemasByProjectId(
    @Param('projectId') projectId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.schemaService.getSchemasByProjectId(projectId, authHeader);
  }

  @Get('schemas/:schemaId')
  async getSchema(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.schemaService.getSchema(schemaId, authHeader);
  }

  @Patch('schemas/:schemaId/name')
  async changeSchemaName(
    @Param('schemaId') schemaId: string,
    @Body() data: ChangeSchemaNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.schemaService.changeSchemaName(schemaId, data, authHeader);
  }

  @Delete('schemas/:schemaId')
  async deleteSchema(
    @Param('schemaId') schemaId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.schemaService.deleteSchema(schemaId, authHeader);
  }
}
