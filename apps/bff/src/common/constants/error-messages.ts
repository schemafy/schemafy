import {
  ErrorCategory,
  ErrorCategoryType,
} from '../types/api-response.types.js';

type ErrorInfo = {
  category: ErrorCategoryType;
};

const SESSION_EXPIRED: ErrorInfo = {
  category: ErrorCategory.AUTO_HANDLE,
};

const SOMETHING_WENT_WRONG: ErrorInfo = {
  category: ErrorCategory.USER_FEEDBACK,
};

const CHECK_INPUT: ErrorInfo = {
  category: ErrorCategory.USER_FEEDBACK,
};

const SETTINGS_TOO_LARGE: ErrorInfo = {
  category: ErrorCategory.USER_FEEDBACK,
};

const notFound = (): ErrorInfo => ({
  category: ErrorCategory.USER_FEEDBACK,
});

const ErrorMessages: Record<string, ErrorInfo> = {
  COMMON_SYSTEM_ERROR: SOMETHING_WENT_WRONG,
  COMMON_INVALID_PARAMETER: {
    category: ErrorCategory.SILENT,
  },
  COMMON_API_VERSION_MISSING: {
    category: ErrorCategory.SILENT,
  },
  COMMON_API_VERSION_INVALID: {
    category: ErrorCategory.SILENT,
  },
  COMMON_INVALID_INPUT_VALUE: CHECK_INPUT,
  COMMON_NOT_FOUND: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  COMMON_INTERNAL_SERVER_ERROR: SOMETHING_WENT_WRONG,
  COMMON_ALREADY_DELETED: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  AUTH_AUTHENTICATION_REQUIRED: {
    category: ErrorCategory.AUTO_HANDLE,
  },
  AUTH_ACCESS_DENIED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  AUTH_INVALID_REFRESH_TOKEN: SESSION_EXPIRED,
  AUTH_INVALID_TOKEN_TYPE: SESSION_EXPIRED,
  AUTH_MISSING_REFRESH_TOKEN: SESSION_EXPIRED,
  AUTH_EXPIRED_TOKEN: SESSION_EXPIRED,
  AUTH_INVALID_TOKEN: SESSION_EXPIRED,
  AUTH_INVALID_ACCESS_TOKEN_TYPE: SESSION_EXPIRED,
  AUTH_MALFORMED_TOKEN: SESSION_EXPIRED,
  AUTH_TOKEN_VALIDATION_ERROR: SESSION_EXPIRED,

  HMAC_SIGNATURE_MISSING: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  HMAC_SIGNATURE_INVALID: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  HMAC_TIMESTAMP_EXPIRED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  HMAC_NONCE_DUPLICATE: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  USER_NOT_FOUND: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  USER_ALREADY_EXISTS: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  USER_LOGIN_FAILED: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  VALIDATION_FAILED: CHECK_INPUT,
  VALIDATION_SERVICE_UNAVAILABLE: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  VALIDATION_TIMEOUT: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  VALIDATION_ERROR: SOMETHING_WENT_WRONG,

  SCHEMA_NOT_FOUND: notFound(),
  TABLE_NOT_FOUND: notFound(),
  COLUMN_NOT_FOUND: notFound(),
  CONSTRAINT_NOT_FOUND: notFound(),
  INDEX_NOT_FOUND: notFound(),
  RELATIONSHIP_NOT_FOUND: notFound(),
  CONSTRAINT_COLUMN_NOT_FOUND: notFound(),
  INDEX_COLUMN_NOT_FOUND: notFound(),
  RELATIONSHIP_COLUMN_NOT_FOUND: notFound(),
  VENDOR_NOT_FOUND: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  MEMO_NOT_FOUND: notFound(),
  MEMO_COMMENT_NOT_FOUND: notFound(),

  WORKSPACE_NOT_FOUND: notFound(),
  WORKSPACE_ACCESS_DENIED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_SETTINGS_INVALID: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_SETTINGS_TOO_LARGE: SETTINGS_TOO_LARGE,
  WORKSPACE_ALREADY_DELETED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_ADMIN_REQUIRED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_ALREADY_EXISTS: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_NOT_FOUND: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_LIMIT_EXCEEDED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_LAST_ADMIN_CANNOT_LEAVE: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_LAST_ADMIN_CANNOT_CHANGE_ROLE: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  PROJECT_NOT_FOUND: notFound(),
  PROJECT_ACCESS_DENIED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_OWNER_ONLY: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_ADMIN_REQUIRED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_WORKSPACE_MISMATCH: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_SETTINGS_TOO_LARGE: SETTINGS_TOO_LARGE,
  PROJECT_ALREADY_DELETED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_CANNOT_ASSIGN_HIGHER_ROLE: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_CANNOT_MODIFY_HIGHER_ROLE_MEMBER: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  PROJECT_CANNOT_CHANGE_OWN_ROLE: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_LAST_OWNER_CANNOT_BE_REMOVED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_WORKSPACE_MEMBERSHIP_REQUIRED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_LIMIT_EXCEEDED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_NOT_FOUND: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_ALREADY_EXISTS: {
    category: ErrorCategory.USER_FEEDBACK,
  },

  SHARE_LINK_NOT_FOUND: notFound(),
  SHARE_LINK_EXPIRED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_REVOKED: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_INVALID: {
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_INVALID_PROJECT_ID: {
    category: ErrorCategory.SILENT,
  },
  SHARE_LINK_INVALID_TOKEN_HASH: {
    category: ErrorCategory.SILENT,
  },
  SHARE_LINK_INVALID_ROLE: {
    category: ErrorCategory.SILENT,
  },
  SHARE_LINK_INVALID_EXPIRATION: {
    category: ErrorCategory.USER_FEEDBACK,
  },
};

export function getErrorInfo(code: string): ErrorInfo {
  return (
    ErrorMessages[code] ?? {
      category: ErrorCategory.SILENT,
    }
  );
}
