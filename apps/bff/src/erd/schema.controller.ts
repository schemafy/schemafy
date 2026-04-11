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
import { TableService } from './table.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { CollaborationHeaders } from '../common/decorators/collaboration-headers.decorator';
import type { CollaborationRequestHeaders } from '../common/backend-client/backend-client.service';
import type {
  ChangeSchemaNameRequest,
  CreateSchemaRequest,
  SchemaSnapshotsResponse,
} from './erd.types';

@Controller('api/v1.0')
export class SchemaController {
  constructor(
    private readonly schemaService: SchemaService,
    private readonly tableService: TableService,
  ) {}

  @Post('schemas')
  async createSchema(
    @Body() data: CreateSchemaRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.schemaService.createSchema(
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('projects/:projectId/schemas')
  async getSchemasByProjectId(
    @Param('projectId') projectId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.schemaService.getSchemasByProjectId(projectId, authHeader);
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
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.schemaService.changeSchemaName(
      schemaId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Delete('schemas/:schemaId')
  async deleteSchema(
    @Param('schemaId') schemaId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.schemaService.deleteSchema(
      schemaId,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('schemas/:schemaId/snapshots')
  async getSchemaWithSnapshots(
    @Param('schemaId') schemaId: string,
    @AuthHeader() authHeader: string,
  ): Promise<SchemaSnapshotsResponse> {
    const [schema, snapshots] = await Promise.all([
      this.schemaService.getSchema(schemaId, authHeader),
      this.tableService.getSchemaWithSnapshots(schemaId, authHeader),
    ]);

    return {
      currentRevision: schema.currentRevision ?? 0,
      snapshots,
    };
  }
}
