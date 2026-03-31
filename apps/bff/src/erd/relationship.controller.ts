import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
} from '@nestjs/common';
import { RelationshipService } from './relationship.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { CollaborationHeaders } from '../common/decorators/collaboration-headers.decorator';
import type { CollaborationRequestHeaders } from '../common/backend-client/backend-client.service';
import type {
  AddRelationshipColumnRequest,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipColumnPositionRequest,
  ChangeRelationshipExtraRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipNameRequest,
  CreateRelationshipRequest,
} from './erd.types';

@Controller('api/v1.0')
export class RelationshipController {
  constructor(private readonly relationshipService: RelationshipService) {}

  @Post('relationships')
  async createRelationship(
    @Body() data: CreateRelationshipRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.createRelationship(
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('relationships/:relationshipId')
  async getRelationship(
    @Param('relationshipId') relationshipId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.relationshipService.getRelationship(relationshipId, authHeader);
  }

  @Get('tables/:tableId/relationships')
  async getRelationshipsByTableId(
    @Param('tableId') tableId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.relationshipService.getRelationshipsByTableId(
      tableId,
      authHeader,
    );
  }

  @Patch('relationships/:relationshipId/name')
  async changeRelationshipName(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipNameRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.changeRelationshipName(
      relationshipId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Patch('relationships/:relationshipId/kind')
  async changeRelationshipKind(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipKindRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.changeRelationshipKind(
      relationshipId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Patch('relationships/:relationshipId/cardinality')
  async changeRelationshipCardinality(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipCardinalityRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.changeRelationshipCardinality(
      relationshipId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Patch('relationships/:relationshipId/extra')
  async changeRelationshipExtra(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipExtraRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.changeRelationshipExtra(
      relationshipId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Delete('relationships/:relationshipId')
  async deleteRelationship(
    @Param('relationshipId') relationshipId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.deleteRelationship(
      relationshipId,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('relationships/:relationshipId/columns')
  async getRelationshipColumns(
    @Param('relationshipId') relationshipId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.relationshipService.getRelationshipColumns(
      relationshipId,
      authHeader,
    );
  }

  @Post('relationships/:relationshipId/columns')
  async addRelationshipColumn(
    @Param('relationshipId') relationshipId: string,
    @Body() data: AddRelationshipColumnRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.addRelationshipColumn(
      relationshipId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }

  @Delete('relationship-columns/:relationshipColumnId')
  async removeRelationshipColumn(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.removeRelationshipColumn(
      relationshipColumnId,
      authHeader,
      collaborationHeaders,
    );
  }

  @Get('relationship-columns/:relationshipColumnId')
  async getRelationshipColumn(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.relationshipService.getRelationshipColumn(
      relationshipColumnId,
      authHeader,
    );
  }

  @Patch('relationship-columns/:relationshipColumnId/position')
  async changeRelationshipColumnPosition(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @Body() data: ChangeRelationshipColumnPositionRequest,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.relationshipService.changeRelationshipColumnPosition(
      relationshipColumnId,
      data,
      authHeader,
      collaborationHeaders,
    );
  }
}
