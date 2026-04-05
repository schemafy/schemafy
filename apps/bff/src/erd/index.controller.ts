import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { IndexService } from './index.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { CollaborationHeaders } from '../common/decorators/collaboration-headers.decorator';
import type { CollaborationRequestHeaders } from '../common/backend-client/backend-client.service';
import type {
  AddIndexColumnRequest,
  ChangeIndexColumnPositionRequest,
  ChangeIndexColumnSortDirectionRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  CreateIndexRequest,
} from './erd.types';

@Controller('api/v1.0')
export class IndexController {
  constructor(private readonly indexService: IndexService) {}

  @Post('indexes')
  async createIndex(
    @Body() data: CreateIndexRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.createIndex(
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('indexes/:indexId')
  async getIndex(
    @Param('indexId') indexId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.indexService.getIndex(indexId, authHeader);
  }

  @Get('tables/:tableId/indexes')
  async getIndexesByTableId(
    @Param('tableId') tableId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.indexService.getIndexesByTableId(tableId, authHeader);
  }

  @Patch('indexes/:indexId/name')
  async changeIndexName(
    @Param('indexId') indexId: string,
    @Body() data: ChangeIndexNameRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.changeIndexName(
      indexId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Patch('indexes/:indexId/type')
  async changeIndexType(
    @Param('indexId') indexId: string,
    @Body() data: ChangeIndexTypeRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.changeIndexType(
      indexId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Delete('indexes/:indexId')
  async deleteIndex(
    @Param('indexId') indexId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.deleteIndex(
      indexId,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('indexes/:indexId/columns')
  async getIndexColumns(
    @Param('indexId') indexId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.indexService.getIndexColumns(indexId, authHeader);
  }

  @Post('indexes/:indexId/columns')
  async addIndexColumn(
    @Param('indexId') indexId: string,
    @Body() data: AddIndexColumnRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.addIndexColumn(
      indexId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Delete('index-columns/:indexColumnId')
  async removeIndexColumn(
    @Param('indexColumnId') indexColumnId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.removeIndexColumn(
      indexColumnId,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('index-columns/:indexColumnId')
  async getIndexColumn(
    @Param('indexColumnId') indexColumnId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.indexService.getIndexColumn(indexColumnId, authHeader);
  }

  @Patch('index-columns/:indexColumnId/position')
  async changeIndexColumnPosition(
    @Param('indexColumnId') indexColumnId: string,
    @Body() data: ChangeIndexColumnPositionRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.changeIndexColumnPosition(
      indexColumnId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Patch('index-columns/:indexColumnId/sort-direction')
  async changeIndexColumnSortDirection(
    @Param('indexColumnId') indexColumnId: string,
    @Body() data: ChangeIndexColumnSortDirectionRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.indexService.changeIndexColumnSortDirection(
      indexColumnId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }
}
