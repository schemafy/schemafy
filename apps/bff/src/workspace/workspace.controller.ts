import { Body, Controller, Delete, Get, Param, Patch, Post, Put, Query } from '@nestjs/common';
import { WorkspaceService } from './workspace.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import type {
  CreateWorkspaceInvitationRequest,
  CreateWorkspaceRequest,
  UpdateMemberRoleRequest,
  UpdateWorkspaceRequest,
} from './workspace.types';

@Controller('api/v1.0')
export class WorkspaceController {
  constructor(private readonly workspaceService: WorkspaceService) {
  }

  @Post('workspaces')
  async createWorkspace(
    @Body() data: CreateWorkspaceRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.createWorkspace(data, authHeader);
  }

  @Get('workspaces')
  async getWorkspaces(
    @Query('page') page = 0,
    @Query('size') size = 5,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.getWorkspaces(+page, +size, authHeader);
  }

  @Get('workspaces/:id')
  async getWorkspace(
    @Param('id') id: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.getWorkspace(id, authHeader);
  }

  @Put('workspaces/:id')
  async updateWorkspace(
    @Param('id') id: string,
    @Body() data: UpdateWorkspaceRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.updateWorkspace(id, data, authHeader);
  }

  @Delete('workspaces/:id')
  async deleteWorkspace(
    @Param('id') id: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.deleteWorkspace(id, authHeader);
  }

  @Get('workspaces/:id/members')
  async getMembers(
    @Param('id') id: string,
    @Query('page') page = 0,
    @Query('size') size = 5,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.getMembers(id, +page, +size, authHeader);
  }

  @Delete('workspaces/:workspaceId/members/me')
  async leaveWorkspace(
    @Param('workspaceId') workspaceId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.leaveWorkspace(workspaceId, authHeader);
  }

  @Delete('workspaces/:workspaceId/members/:userId')
  async removeMember(
    @Param('workspaceId') workspaceId: string,
    @Param('userId') userId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.removeMember(workspaceId, userId, authHeader);
  }

  @Patch('workspaces/:workspaceId/members/:userId/role')
  async updateMemberRole(
    @Param('workspaceId') workspaceId: string,
    @Param('userId') userId: string,
    @Body() data: UpdateMemberRoleRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.updateMemberRole(
      workspaceId,
      userId,
      data,
      authHeader,
    );
  }

  @Post('workspaces/:workspaceId/invitations')
  async createInvitation(
    @Param('workspaceId') workspaceId: string,
    @Body() data: CreateWorkspaceInvitationRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.createInvitation(workspaceId, data, authHeader);
  }

  @Get('workspaces/:workspaceId/invitations')
  async getInvitations(
    @Param('workspaceId') workspaceId: string,
    @Query('page') page = 0,
    @Query('size') size = 10,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.getInvitations(
      workspaceId,
      +page,
      +size,
      authHeader,
    );
  }

  @Get('users/me/invitations/workspaces')
  async getMyInvitations(
    @Query('page') page = 0,
    @Query('size') size = 10,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.getMyInvitations(+page, +size, authHeader);
  }

  @Patch('workspaces/invitations/:invitationId/accept')
  async acceptInvitation(
    @Param('invitationId') invitationId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.acceptInvitation(invitationId, authHeader);
  }

  @Patch('workspaces/invitations/:invitationId/reject')
  async rejectInvitation(
    @Param('invitationId') invitationId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.workspaceService.rejectInvitation(invitationId, authHeader);
  }
}