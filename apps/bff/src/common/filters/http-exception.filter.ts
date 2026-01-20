import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import axios, { AxiosError } from 'axios';
import {
  ApiError,
  ErrorCode,
  ErrorCategory,
  ErrorCategoryType,
  getErrorInfo,
  getErrorCodeFromStatus,
  getMessageFromStatus,
} from '@/common/index.js';

@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);
  private readonly isProduction = process.env.NODE_ENV === 'production';

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

    if (this.isValidBackendResponse(rawData)) {
      const { code, message, details } = rawData.error;
      const errorCode = code ?? getErrorCodeFromStatus(status);
      const errorInfo = getErrorInfo(
        errorCode,
        message ?? getMessageFromStatus(status),
      );

      return {
        status,
        errorResponse: this.createErrorResponse(
          errorCode,
          errorInfo.message,
          errorInfo.category,
          details,
        ),
      };
    }

    return this.buildErrorResult(
      status,
      getErrorCodeFromStatus(status),
      getMessageFromStatus(status),
    );
  }

  private handleNetworkError(error: AxiosError, context: string) {
    if (error.code === 'ECONNREFUSED' || error.code === 'ENOTFOUND') {
      this.logger.error(`${context} - Backend unreachable: ${error.message}`);
      return this.buildErrorResult(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.BACKEND_UNREACHABLE,
        'Service is temporarily unavailable. Please try again later.',
        ErrorCategory.USER_FEEDBACK,
      );
    }

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      this.logger.error(`${context} - Backend timeout: ${error.message}`);
      return this.buildErrorResult(
        HttpStatus.GATEWAY_TIMEOUT,
        ErrorCode.BACKEND_TIMEOUT,
        'Request timed out. Please try again.',
        ErrorCategory.USER_FEEDBACK,
      );
    }

    this.logger.error(`${context} - Axios error: ${error.message}`);
    return this.buildErrorResult(
      HttpStatus.INTERNAL_SERVER_ERROR,
      ErrorCode.INTERNAL_SERVER_ERROR,
      'Something went wrong. Please try again later.',
      ErrorCategory.USER_FEEDBACK,
    );
  }

  private handleHttpException(exception: HttpException, context: string) {
    const status = exception.getStatus();

    this.logger.warn(
      `${context} - HttpException: ${status} - ${exception.message}`,
    );

    const { message, details } = this.extractValidationInfo(
      exception.message,
      exception.getResponse(),
    );

    return {
      status,
      errorResponse: this.createErrorResponse(
        getErrorCodeFromStatus(status),
        message,
        ErrorCategory.USER_FEEDBACK,
        details,
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
      this.isProduction
        ? 'Something went wrong. Please try again later.'
        : errorMessage,
      ErrorCategory.USER_FEEDBACK,
    );
  }

  private buildErrorResult(
    status: number,
    code: string,
    message: string,
    category: ErrorCategoryType = ErrorCategory.SILENT,
  ) {
    return {
      status,
      errorResponse: this.createErrorResponse(code, message, category),
    };
  }

  private createErrorResponse(
    code: string,
    message: string,
    category: ErrorCategoryType,
    details?: Record<string, unknown>,
  ) {
    const error: ApiError = { code, message, category };
    if (!this.isProduction && details) {
      error.details = details;
    }
    return { success: false, result: null, error };
  }

  private isValidBackendResponse(
    data: unknown,
  ): data is {
    error: {
      code?: string;
      message?: string;
      details?: Record<string, unknown>;
    };
  } {
    return (
      typeof data === 'object' &&
      data !== null &&
      'error' in data &&
      typeof (data as { error: unknown }).error === 'object' &&
      (data as { error: unknown }).error !== null
    );
  }

  private extractValidationInfo(defaultMessage: string, response: unknown) {
    if (
      typeof response !== 'object' ||
      response === null ||
      !('message' in response)
    ) {
      return { message: defaultMessage };
    }

    const responseMessage = (response as { message: unknown }).message;

    if (Array.isArray(responseMessage)) {
      return {
        message: responseMessage.join(', '),
        details: { validationErrors: responseMessage },
      };
    }

    return { message: String(responseMessage) };
  }
}
