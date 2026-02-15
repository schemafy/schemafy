import { HttpStatus } from '@nestjs/common';
import { ErrorCode, ErrorCodeType } from '../types/api-response.types.js';

export const STATUS_CODE_MAP: Record<number, ErrorCodeType> = {
  [HttpStatus.BAD_REQUEST]: ErrorCode.BAD_REQUEST,
  [HttpStatus.UNAUTHORIZED]: ErrorCode.UNAUTHORIZED,
  [HttpStatus.FORBIDDEN]: ErrorCode.FORBIDDEN,
  [HttpStatus.NOT_FOUND]: ErrorCode.NOT_FOUND,
  [HttpStatus.CONFLICT]: ErrorCode.CONFLICT,
  [HttpStatus.INTERNAL_SERVER_ERROR]: ErrorCode.INTERNAL_SERVER_ERROR,
  [HttpStatus.SERVICE_UNAVAILABLE]: ErrorCode.SERVICE_UNAVAILABLE,
  [HttpStatus.GATEWAY_TIMEOUT]: ErrorCode.GATEWAY_TIMEOUT,
};

export const STATUS_MESSAGE_MAP: Record<number, string> = {
  [HttpStatus.BAD_REQUEST]: 'Please check your input and try again.',
  [HttpStatus.UNAUTHORIZED]: 'Please sign in to continue.',
  [HttpStatus.FORBIDDEN]: "You don't have permission to perform this action.",
  [HttpStatus.NOT_FOUND]:
    "The requested item doesn't exist or may have been moved.",
  [HttpStatus.CONFLICT]:
    'This action conflicts with existing data. Please refresh and try again.',
  [HttpStatus.INTERNAL_SERVER_ERROR]:
    'Something went wrong. Please try again later.',
  [HttpStatus.SERVICE_UNAVAILABLE]:
    'Service is temporarily unavailable. Please try again later.',
  [HttpStatus.GATEWAY_TIMEOUT]: 'Request timed out. Please try again.',
};

export function getErrorCodeFromStatus(status: number): ErrorCodeType {
  return STATUS_CODE_MAP[status] ?? ErrorCode.INTERNAL_SERVER_ERROR;
}

export function getMessageFromStatus(status: number): string {
  return (
    STATUS_MESSAGE_MAP[status] ??
    'Something went wrong. Please try again later.'
  );
}
