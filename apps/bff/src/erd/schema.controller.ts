import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { SchemaService } from './schema.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import type { ChangeSchemaNameRequest, CreateSchemaRequest } from './erd.types';

@Controller('api/v1.0')
export class SchemaController {
  constructor(private readonly schemaService: SchemaService) {}

  @Post('schemas')
  async createSchema(
    @Body() data: CreateSchemaRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.schemaService.createSchema(data, authHeader);
  }

  @Get('schemas/:schemaId')
  async getSchema(
    @Param('schemaId') schemaId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.schemaService.getSchema(schemaId, authHeader);
  }

  @Patch('schemas/:schemaId/name')
  async changeSchemaName(
    @Param('schemaId') schemaId: string,
    @Body() data: ChangeSchemaNameRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.schemaService.changeSchemaName(schemaId, data, authHeader);
  }

  @Delete('schemas/:schemaId')
  async deleteSchema(
    @Param('schemaId') schemaId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.schemaService.deleteSchema(schemaId, authHeader);
  }
}
