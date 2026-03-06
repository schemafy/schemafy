import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import type { Request } from 'express';

export const SessionId = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): string | undefined => {
    const request = ctx.switchToHttp().getRequest<Request>();
    const raw = request.headers['x-session-id'];
    return Array.isArray(raw) ? raw[0] : raw;
  },
);
