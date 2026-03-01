import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Injectable,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request, Response } from 'express';
import axios, { AxiosError } from 'axios';
import {
  ApiError,
  ErrorCode,
  ErrorCategory,
  ErrorCategoryType,
} from '../types/api-response.types.js';
import { getErrorInfo } from '../constants/error-messages.js';
import { getErrorCodeFromStatus } from '../constants/http-status-maps.js';

type BackendProblemDetails = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  reason?: string;
  [key: string]: unknown;
};

@Injectable()
@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);
  private readonly isProduction: boolean;

  constructor(private readonly configService: ConfigService) {
    this.isProduction =
      this.configService.get<string>('NODE_ENV') === 'production';
  }

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const request = ctx.getRequest<Request>();
    const response = ctx.getResponse<Response>();
    const context = `[${request.method}] ${request.url}`;

    const { status, errorResponse } = this.handleException(exception, context);
    response.status(status).json(errorResponse);
  }

  private handleException(exception: unknown, context: string) {
    if (axios.isAxiosError(exception)) {
      return this.handleAxiosError(exception, context);
    }
    if (exception instanceof HttpException) {
      return this.handleHttpException(exception, context);
    }
    return this.handleUnknownError(exception, context);
  }

  private handleAxiosError(error: AxiosError, context: string) {
    if (error.response) {
      return this.handleBackendError(error, context);
    }
    return this.handleNetworkError(error, context);
  }

  private handleBackendError(error: AxiosError, context: string) {
    const status = error.response!.status;
    const rawData = error.response!.data;

    this.logger.warn(
      `${context} - Backend error: ${status} - ${JSON.stringify(rawData)}`,
    );

    if (this.isProblemDetailsResponse(rawData)) {
      const reason =
        typeof rawData.reason === 'string' ? rawData.reason : undefined;
      const errorCode = reason ?? getErrorCodeFromStatus(status);
      const errorInfo = getErrorInfo(errorCode);

      return {
        status,
        errorResponse: this.createErrorResponse(
          errorCode,
          errorInfo.category,
          this.extractProblemDetails(rawData),
        ),
      };
    }

    return this.buildErrorResult(status, getErrorCodeFromStatus(status));
  }

  private handleNetworkError(error: AxiosError, context: string) {
    if (error.code === 'ECONNREFUSED' || error.code === 'ENOTFOUND') {
      this.logger.error(`${context} - Backend unreachable: ${error.message}`);
      return this.buildErrorResult(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.BACKEND_UNREACHABLE,
        ErrorCategory.USER_FEEDBACK,
      );
    }

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      this.logger.error(`${context} - Backend timeout: ${error.message}`);
      return this.buildErrorResult(
        HttpStatus.GATEWAY_TIMEOUT,
        ErrorCode.BACKEND_TIMEOUT,
        ErrorCategory.USER_FEEDBACK,
      );
    }

    this.logger.error(`${context} - Axios error: ${error.message}`);
    return this.buildErrorResult(
      HttpStatus.INTERNAL_SERVER_ERROR,
      ErrorCode.INTERNAL_SERVER_ERROR,
      ErrorCategory.USER_FEEDBACK,
    );
  }

  private handleHttpException(exception: HttpException, context: string) {
    const status = exception.getStatus();

    this.logger.warn(
      `${context} - HttpException: ${status} - ${exception.message}`,
    );

    return {
      status,
      errorResponse: this.createErrorResponse(
        getErrorCodeFromStatus(status),
        ErrorCategory.USER_FEEDBACK,
      ),
    };
  }

  private handleUnknownError(exception: unknown, context: string) {
    const errorMessage =
      exception instanceof Error ? exception.message : 'Unknown error';

    this.logger.error(
      `${context} - Unknown error: ${errorMessage}`,
      exception instanceof Error ? exception.stack : undefined,
    );

    return this.buildErrorResult(
      HttpStatus.INTERNAL_SERVER_ERROR,
      ErrorCode.INTERNAL_SERVER_ERROR,
      ErrorCategory.USER_FEEDBACK,
    );
  }

  private buildErrorResult(
    status: number,
    code: string,
    category: ErrorCategoryType = ErrorCategory.SILENT,
  ) {
    return {
      status,
      errorResponse: this.createErrorResponse(code, category),
    };
  }

  private createErrorResponse(
    code: string,
    category: ErrorCategoryType,
    details?: Record<string, unknown>,
  ) {
    const error: ApiError = { code, category };
    if (!this.isProduction && details) {
      error.details = details;
    }
    return error;
  }

  private isProblemDetailsResponse(
    data: unknown,
  ): data is BackendProblemDetails {
    if (typeof data !== 'object' || data === null) {
      return false;
    }

    const problem = data as Record<string, unknown>;
    return 'reason' in problem || ('title' in problem && 'status' in problem);
  }

  private extractProblemDetails(
    problem: BackendProblemDetails,
  ): Record<string, unknown> | undefined {
    const details: Record<string, unknown> = {};

    if (typeof problem.type === 'string') {
      details.type = problem.type;
    }
    if (typeof problem.instance === 'string') {
      details.instance = problem.instance;
    }
    if (typeof problem.detail === 'string') {
      details.originalDetail = problem.detail;
    }

    const extensionEntries = Object.entries(problem).filter(
      ([key]) =>
        !['type', 'title', 'status', 'detail', 'instance', 'reason'].includes(
          key,
        ),
    );

    if (extensionEntries.length > 0) {
      details.extensions = Object.fromEntries(extensionEntries);
    }

    return Object.keys(details).length > 0 ? details : undefined;
  }
}
