import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
  Logger,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Request, Response } from 'express';

@Injectable()
export class LoggingInterceptor implements NestInterceptor {
  private readonly logger = new Logger('HTTP');

  private readonly excludedPaths = ['/health', '/favicon.ico'];

  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const ctx = context.switchToHttp();
    const request = ctx.getRequest<Request>();
    const response = ctx.getResponse<Response>();

    const { method, originalUrl } = request;

    if (this.shouldSkipLogging(originalUrl)) {
      return next.handle();
    }

    const startTime = Date.now();
    const requestSize = this.getContentLength(
      request.headers['content-length'],
    );

    return next.handle().pipe(
      tap({
        next: () => {
          const duration = Date.now() - startTime;
          const statusCode = response.statusCode;
          const responseSize = this.getContentLength(
            response.getHeader('content-length'),
          );

          this.logger.log(
            this.formatLog(
              method,
              originalUrl,
              statusCode,
              duration,
              requestSize,
              responseSize,
            ),
          );
        },
        error: (error: Error) => {
          const duration = Date.now() - startTime;
          const statusCode = this.getErrorStatus(error);

          this.logger.warn(
            this.formatLog(
              method,
              originalUrl,
              statusCode,
              duration,
              requestSize,
            ),
          );
        },
      }),
    );
  }

  private shouldSkipLogging(url: string): boolean {
    return this.excludedPaths.some((path) => url.startsWith(path));
  }

  private getContentLength(
    value: string | number | string[] | undefined,
  ): string {
    if (!value) return '-';
    const size =
      typeof value === 'string' ? parseInt(value, 10) : Number(value);
    if (isNaN(size)) return '-';
    return this.formatBytes(size);
  }

  private formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }

  private getErrorStatus(error: Error): number {
    if (error instanceof HttpException) {
      return error.getStatus();
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private formatLog(
    method: string,
    url: string,
    status: number,
    duration: number,
    reqSize: string,
    resSize?: string,
  ): string {
    const sizeInfo = resSize ? `${reqSize}/${resSize}` : reqSize;
    return `[${method}] ${url} - ${status} - ${duration}ms - ${sizeInfo}`;
  }
}
