package com.schemafy.core.validation.exception;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;

public class GrpcConnectionException extends BusinessException {

    public GrpcConnectionException(Throwable cause) {
        super(ErrorCode.VALIDATION_SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
