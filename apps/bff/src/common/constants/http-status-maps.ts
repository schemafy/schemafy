import { HttpStatus } from '@nestjs/common';
import { ErrorCode, ErrorCodeType } from '../types/api-response.types.js';

const STATUS_CODE_MAP: Record<number, ErrorCodeType> = {
  [HttpStatus.BAD_REQUEST]: ErrorCode.BAD_REQUEST,
  [HttpStatus.UNAUTHORIZED]: ErrorCode.UNAUTHORIZED,
  [HttpStatus.FORBIDDEN]: ErrorCode.FORBIDDEN,
  [HttpStatus.NOT_FOUND]: ErrorCode.NOT_FOUND,
  [HttpStatus.CONFLICT]: ErrorCode.CONFLICT,
  [HttpStatus.INTERNAL_SERVER_ERROR]: ErrorCode.INTERNAL_SERVER_ERROR,
  [HttpStatus.SERVICE_UNAVAILABLE]: ErrorCode.SERVICE_UNAVAILABLE,
  [HttpStatus.GATEWAY_TIMEOUT]: ErrorCode.GATEWAY_TIMEOUT,
};

export function getErrorCodeFromStatus(status: number): ErrorCodeType {
  return STATUS_CODE_MAP[status] ?? ErrorCode.INTERNAL_SERVER_ERROR;
}
