import { Body, Controller, Delete, Get, Param, Patch, Post, Put, Query, } from '@nestjs/common';
import { ProjectService } from './project.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import type {
  CreateProjectInvitationRequest,
  CreateProjectRequest,
  UpdateProjectMemberRoleRequest,
  UpdateProjectRequest,
} from './project.types';

@Controller('api/v1.0')
export class ProjectController {
  constructor(private readonly projectService: ProjectService) {
  }

  @Post('workspaces/:workspaceId/projects')
  async createProject(
    @Param('workspaceId') workspaceId: string,
    @Body() data: CreateProjectRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.createProject(workspaceId, data, authHeader);
  }

  @Get('workspaces/:workspaceId/projects')
  async getProjects(
    @Param('workspaceId') workspaceId: string,
    @Query('page') page = 0,
    @Query('size') size = 5,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.getProjects(
      workspaceId,
      +page,
      +size,
      authHeader,
    );
  }

  @Get('projects/:projectId')
  async getProject(
    @Param('projectId') projectId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.getProject(projectId, authHeader);
  }

  @Put('projects/:projectId')
  async updateProject(
    @Param('projectId') projectId: string,
    @Body() data: UpdateProjectRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.updateProject(projectId, data, authHeader);
  }

  @Delete('projects/:projectId')
  async deleteProject(
    @Param('projectId') projectId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.deleteProject(projectId, authHeader);
  }

  @Get('projects/:projectId/members')
  async getMembers(
    @Param('projectId') projectId: string,
    @Query('page') page = 0,
    @Query('size') size = 5,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.getMembers(projectId, +page, +size, authHeader);
  }

  @Patch('projects/:projectId/members/:userId/role')
  async updateMemberRole(
    @Param('projectId') projectId: string,
    @Param('userId') userId: string,
    @Body() data: UpdateProjectMemberRoleRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.updateMemberRole(
      projectId,
      userId,
      data,
      authHeader,
    );
  }

  @Delete('projects/:projectId/members/me')
  async leaveProject(
    @Param('projectId') projectId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.leaveProject(projectId, authHeader);
  }

  @Delete('projects/:projectId/members/:userId')
  async removeMember(
    @Param('projectId') projectId: string,
    @Param('userId') userId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.removeMember(projectId, userId, authHeader);
  }

  @Post('projects/:projectId/invitations')
  async createInvitation(
    @Param('projectId') projectId: string,
    @Body() data: CreateProjectInvitationRequest,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.createInvitation(projectId, data, authHeader);
  }

  @Get('projects/:projectId/invitations')
  async getInvitations(
    @Param('projectId') projectId: string,
    @Query('page') page = 0,
    @Query('size') size = 10,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.getInvitations(
      projectId,
      +page,
      +size,
      authHeader,
    );
  }

  @Get('users/me/invitations/projects')
  async getMyInvitations(
    @Query('page') page = 0,
    @Query('size') size = 10,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.getMyInvitations(+page, +size, authHeader);
  }

  @Patch('projects/invitations/:invitationId/accept')
  async acceptInvitation(
    @Param('invitationId') invitationId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.acceptInvitation(invitationId, authHeader);
  }

  @Patch('projects/invitations/:invitationId/reject')
  async rejectInvitation(
    @Param('invitationId') invitationId: string,
    @AuthHeader() authHeader: string,
  ) {
    return this.projectService.rejectInvitation(invitationId, authHeader);
  }
}
