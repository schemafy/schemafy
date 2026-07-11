import { Controller, Param, Post } from '@nestjs/common';
import { OperationService } from './operation.service';
import { AuthHeader } from '../common/decorators/auth-header.decorator';
import { CollaborationHeaders } from '../common/decorators/collaboration-headers.decorator';
import type { CollaborationRequestHeaders } from '../common/backend-client/backend-client.service';

@Controller('api/v1.0')
export class OperationController {
  constructor(private readonly operationService: OperationService) {}

  @Post('operations/:opId/undo')
  async undo(
    @Param('opId') opId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.operationService.undo(opId, authHeader, collaborationHeaders);
  }

  @Post('operations/:opId/redo')
  async redo(
    @Param('opId') opId: string,
    @AuthHeader() authHeader: string,
    @CollaborationHeaders()
    collaborationHeaders?: CollaborationRequestHeaders,
  ) {
    return this.operationService.redo(opId, authHeader, collaborationHeaders);
  }
}
