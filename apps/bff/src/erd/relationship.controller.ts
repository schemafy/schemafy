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
import { RelationshipService } from './relationship.service';
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
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.createRelationship(data, authHeader);
  }

  @Get('relationships/:relationshipId')
  async getRelationship(
    @Param('relationshipId') relationshipId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.getRelationship(relationshipId, authHeader);
  }

  @Get('tables/:tableId/relationships')
  async getRelationshipsByTableId(
    @Param('tableId') tableId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.getRelationshipsByTableId(tableId, authHeader);
  }

  @Patch('relationships/:relationshipId/name')
  async changeRelationshipName(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipNameRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.changeRelationshipName(relationshipId, data, authHeader);
  }

  @Patch('relationships/:relationshipId/kind')
  async changeRelationshipKind(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipKindRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.changeRelationshipKind(relationshipId, data, authHeader);
  }

  @Patch('relationships/:relationshipId/cardinality')
  async changeRelationshipCardinality(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipCardinalityRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.changeRelationshipCardinality(relationshipId, data, authHeader);
  }

  @Patch('relationships/:relationshipId/extra')
  async changeRelationshipExtra(
    @Param('relationshipId') relationshipId: string,
    @Body() data: ChangeRelationshipExtraRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.changeRelationshipExtra(relationshipId, data, authHeader);
  }

  @Delete('relationships/:relationshipId')
  async deleteRelationship(
    @Param('relationshipId') relationshipId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.deleteRelationship(relationshipId, authHeader);
  }

  @Get('relationships/:relationshipId/columns')
  async getRelationshipColumns(
    @Param('relationshipId') relationshipId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.getRelationshipColumns(relationshipId, authHeader);
  }

  @Post('relationships/:relationshipId/columns')
  async addRelationshipColumn(
    @Param('relationshipId') relationshipId: string,
    @Body() data: AddRelationshipColumnRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.addRelationshipColumn(relationshipId, data, authHeader);
  }

  @Delete('relationship-columns/:relationshipColumnId')
  async removeRelationshipColumn(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.removeRelationshipColumn(relationshipColumnId, authHeader);
  }

  @Get('relationship-columns/:relationshipColumnId')
  async getRelationshipColumn(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.getRelationshipColumn(relationshipColumnId, authHeader);
  }

  @Patch('relationship-columns/:relationshipColumnId/position')
  async changeRelationshipColumnPosition(
    @Param('relationshipColumnId') relationshipColumnId: string,
    @Body() data: ChangeRelationshipColumnPositionRequest,
    @Headers('authorization') authHeader: string,
  ) {
    return this.relationshipService.changeRelationshipColumnPosition(relationshipColumnId, data, authHeader);
  }
}
