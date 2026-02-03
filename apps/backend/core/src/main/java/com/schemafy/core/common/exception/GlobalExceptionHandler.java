package com.schemafy.core.common.exception;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameDuplicateException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameReservedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.ForeignKeyColumnProtectedException;
import com.schemafy.domain.erd.column.domain.exception.MultipleAutoIncrementColumnException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintDefinitionDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.MultiplePrimaryKeyConstraintException;
import com.schemafy.domain.erd.constraint.domain.exception.UniqueSameAsPrimaryKeyException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnSortDirectionInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexDefinitionDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNameDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexTypeInvalidException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipEmptyException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Map<Class<? extends DomainException>, ErrorCode> DOMAIN_ERROR_CODE_MAP =
      Map.ofEntries(
          Map.entry(SchemaNotExistException.class, ErrorCode.ERD_SCHEMA_NOT_FOUND),
          Map.entry(SchemaNameDuplicateException.class, ErrorCode.ERD_SCHEMA_NAME_DUPLICATE),
          Map.entry(TableNotExistException.class, ErrorCode.ERD_TABLE_NOT_FOUND),
          Map.entry(TableNameDuplicateException.class, ErrorCode.ERD_TABLE_NAME_DUPLICATE),
          Map.entry(ColumnNotExistException.class, ErrorCode.ERD_COLUMN_NOT_FOUND),
          Map.entry(ConstraintNotExistException.class, ErrorCode.ERD_CONSTRAINT_NOT_FOUND),
          Map.entry(ConstraintColumnNotExistException.class, ErrorCode.ERD_CONSTRAINT_COLUMN_NOT_FOUND),
          Map.entry(RelationshipNotExistException.class, ErrorCode.ERD_RELATIONSHIP_NOT_FOUND),
          Map.entry(RelationshipColumnNotExistException.class, ErrorCode.ERD_RELATIONSHIP_COLUMN_NOT_FOUND),
          Map.entry(RelationshipTargetTableNotExistException.class, ErrorCode.ERD_TABLE_NOT_FOUND),
          Map.entry(IndexNotExistException.class, ErrorCode.ERD_INDEX_NOT_FOUND),
          Map.entry(IndexColumnNotExistException.class, ErrorCode.ERD_INDEX_COLUMN_NOT_FOUND));

  private static final Set<Class<? extends DomainException>> INVALID_PARAMETER_EXCEPTIONS = Set.of(
      ColumnNameDuplicateException.class,
      ColumnNameReservedException.class,
      MultipleAutoIncrementColumnException.class,
      ForeignKeyColumnProtectedException.class,
      ConstraintNameDuplicateException.class,
      ConstraintColumnDuplicateException.class,
      ConstraintDefinitionDuplicateException.class,
      UniqueSameAsPrimaryKeyException.class,
      MultiplePrimaryKeyConstraintException.class,
      RelationshipColumnDuplicateException.class,
      RelationshipCyclicReferenceException.class,
      RelationshipEmptyException.class,
      RelationshipNameDuplicateException.class,
      IndexNameDuplicateException.class,
      IndexColumnDuplicateException.class,
      IndexDefinitionDuplicateException.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<BaseResponse<Object>> handleBusinessException(
      BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn("[BusinessException] BusinessException occurred: {}",
        errorCode.getMessage(), e);
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<BaseResponse<Object>> handleAuthorizationDeniedException(
      AuthorizationDeniedException e) {
    ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
    log.warn("AuthorizationDeniedException occurred: {}", e.getMessage());
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<BaseResponse<Object>> handleValidationException(
      WebExchangeBindException e) {
    ErrorCode errorCode = ErrorCode.COMMON_INVALID_PARAMETER;
    log.warn("[WebExchangeBindException] Validation failed: {}",
        e.getMessage());
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(), e.getReason()));
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<BaseResponse<Object>> handleDomainException(
      DomainException e) {
    ErrorCode errorCode = mapDomainExceptionToErrorCode(e);
    log.warn("[DomainException] Domain exception occurred: {}",
        e.getMessage(), e);
    return ResponseEntity
        .status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

  private ErrorCode mapDomainExceptionToErrorCode(DomainException e) {
    for (Map.Entry<Class<? extends DomainException>, ErrorCode> entry
        : DOMAIN_ERROR_CODE_MAP.entrySet()) {
      if (entry.getKey().isInstance(e)) {
        return entry.getValue();
      }
    }
    if (matchesInvalidParameter(e)) {
      return ErrorCode.COMMON_INVALID_PARAMETER;
    }
    return ErrorCode.COMMON_SYSTEM_ERROR;
  }

  private boolean matchesInvalidParameter(DomainException e) {
    if (e instanceof InvalidValueException) {
      return true;
    }
    for (Class<? extends DomainException> exceptionClass : INVALID_PARAMETER_EXCEPTIONS) {
      if (exceptionClass.isInstance(e)) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
    ErrorCode errorCode = ErrorCode.COMMON_SYSTEM_ERROR;
    log.error("[Exception] Unhandled exception occurred: {}",
        e.getMessage(), e);
    return ResponseEntity.status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getCode(),
            errorCode.getMessage()));
  }

}
