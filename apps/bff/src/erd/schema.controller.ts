import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  Res,
} from '@nestjs/common';
import type { Response } from 'express';
import { SchemaService } from './schema.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { CollaborationHeaders } from '../common/decorators/collaboration-headers.decorator';
import type { CollaborationRequestHeaders } from '../common/backend-client/backend-client.service';
import type {
  ChangeSchemaNameRequest,
  CreateSchemaRequest,
  SchemaDdlExportResponse,
  SchemaMermaidExportResponse,
  SchemaSnapshotsResponse,
} from './erd.types';

@Controller('api/v1.0')
export class SchemaController {
  constructor(private readonly schemaService: SchemaService) {}

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
    return this.schemaService.getSchemaWithSnapshots(schemaId, authHeader);
  }

  @Get('schemas/:schemaId/exports/ddl')
  async exportSchemaDdl(
    @Param('schemaId') schemaId: string,
    @Query('targetDbVendor') targetDbVendor: string,
    @AuthHeader() authHeader: string,
    @Res({ passthrough: true }) response: Response,
  ): Promise<SchemaDdlExportResponse> {
    return this.withDisconnectSignal(response, (signal) =>
      this.schemaService.exportSchemaDdl(
        schemaId,
        targetDbVendor,
        authHeader,
        signal,
      ),
    );
  }

  @Get('schemas/:schemaId/exports/mermaid')
  async exportSchemaMermaid(
    @Param('schemaId') schemaId: string,
    @AuthHeader() authHeader: string,
    @Res({ passthrough: true }) response: Response,
  ): Promise<SchemaMermaidExportResponse> {
    return this.withDisconnectSignal(response, (signal) =>
      this.schemaService.exportSchemaMermaid(schemaId, authHeader, signal),
    );
  }

  private async withDisconnectSignal<T>(
    response: Response,
    run: (signal: AbortSignal) => Promise<T>,
  ): Promise<T> {
    const controller = new AbortController();
    const onClose = () => {
      if (!response.writableFinished) controller.abort();
    };
    response.once('close', onClose);
    if (response.destroyed) onClose();
    try {
      return await run(controller.signal);
    } finally {
      response.off('close', onClose);
    }
  }
}
