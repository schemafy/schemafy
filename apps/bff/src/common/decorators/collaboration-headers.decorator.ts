import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import type { Request } from 'express';

import type { CollaborationRequestHeaders } from '../backend-client/backend-client.service';

function firstHeaderValue(
  raw: string | string[] | undefined,
): string | undefined {
  return Array.isArray(raw) ? raw[0] : raw;
}

export const CollaborationHeaders = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): CollaborationRequestHeaders => {
    const request = ctx.switchToHttp().getRequest<Request>();

    return {
      sessionId: firstHeaderValue(request.headers['x-session-id']),
      clientOperationId: firstHeaderValue(request.headers['x-client-op-id']),
      baseSchemaRevision: firstHeaderValue(
        request.headers['x-base-schema-revision'],
      ),
    };
  },
);
