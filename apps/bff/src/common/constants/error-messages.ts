import {
  ErrorCategory,
  ErrorCategoryType,
} from '../types/api-response.types.js';

type ErrorInfo = {
  message: string;
  category: ErrorCategoryType;
};

const SESSION_EXPIRED: ErrorInfo = {
  message: 'Your session has expired. Please sign in again.',
  category: ErrorCategory.AUTO_HANDLE,
};

const SOMETHING_WENT_WRONG: ErrorInfo = {
  message: 'Something went wrong. Please try again later.',
  category: ErrorCategory.USER_FEEDBACK,
};

const CHECK_INPUT: ErrorInfo = {
  message: 'Please check your input and try again.',
  category: ErrorCategory.USER_FEEDBACK,
};

const SETTINGS_TOO_LARGE: ErrorInfo = {
  message: 'Settings are too large. Please reduce the size and try again.',
  category: ErrorCategory.USER_FEEDBACK,
};

const notFound = (item: string): ErrorInfo => ({
  message: `${item} not found. Please check and try again.`,
  category: ErrorCategory.USER_FEEDBACK,
});

export const ErrorMessages: Record<string, ErrorInfo> = {
  COMMON_SYSTEM_ERROR: SOMETHING_WENT_WRONG,
  COMMON_INVALID_PARAMETER: { message: 'Invalid parameter.', category: ErrorCategory.SILENT },
  COMMON_API_VERSION_MISSING: { message: 'API version is missing.', category: ErrorCategory.SILENT },
  COMMON_API_VERSION_INVALID: {
    message: 'Invalid API version format. (e.g., v1.0, v2.1)',
    category: ErrorCategory.SILENT,
  },
  COMMON_INVALID_INPUT_VALUE: CHECK_INPUT,
  COMMON_NOT_FOUND: {
    message: "The requested item doesn't exist or may have been moved.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  COMMON_INTERNAL_SERVER_ERROR: SOMETHING_WENT_WRONG,
  COMMON_ALREADY_DELETED: {
    message: 'This item has already been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  AUTH_AUTHENTICATION_REQUIRED: {
    message: 'Please sign in to continue.',
    category: ErrorCategory.AUTO_HANDLE,
  },
  AUTH_ACCESS_DENIED: {
    message: "You don't have permission to perform this action.",
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

  USER_NOT_FOUND: {
    message: 'User not found. Please check and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  USER_ALREADY_EXISTS: {
    message: 'This email is already registered.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  USER_LOGIN_FAILED: {
    message: 'Invalid email or password. Please try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  VALIDATION_FAILED: CHECK_INPUT,
  VALIDATION_SERVICE_UNAVAILABLE: {
    message: 'Service is temporarily unavailable. Please try again later.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  VALIDATION_TIMEOUT: {
    message: 'Request timed out. Please try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  VALIDATION_ERROR: SOMETHING_WENT_WRONG,

  SCHEMA_NOT_FOUND: notFound('Schema'),
  TABLE_NOT_FOUND: notFound('Table'),
  COLUMN_NOT_FOUND: notFound('Column'),
  CONSTRAINT_NOT_FOUND: notFound('Constraint'),
  INDEX_NOT_FOUND: notFound('Index'),
  RELATIONSHIP_NOT_FOUND: notFound('Relationship'),
  CONSTRAINT_COLUMN_NOT_FOUND: notFound('Constraint column'),
  INDEX_COLUMN_NOT_FOUND: notFound('Index column'),
  RELATIONSHIP_COLUMN_NOT_FOUND: notFound('Relationship column'),
  VENDOR_NOT_FOUND: {
    message: 'Unsupported database type.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  MEMO_NOT_FOUND: notFound('Memo'),
  MEMO_COMMENT_NOT_FOUND: notFound('Comment'),

  WORKSPACE_NOT_FOUND: notFound('Workspace'),
  WORKSPACE_ACCESS_DENIED: {
    message: "You don't have access to this workspace.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_SETTINGS_INVALID: {
    message: 'Failed to save settings. Please check the format and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_SETTINGS_TOO_LARGE: SETTINGS_TOO_LARGE,
  WORKSPACE_ALREADY_DELETED: {
    message: 'This workspace has been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_ADMIN_REQUIRED: {
    message: 'Only admins can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_ALREADY_EXISTS: {
    message: 'This user is already a member of this workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_NOT_FOUND: {
    message: 'Member not found in this workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_MEMBER_LIMIT_EXCEEDED: {
    message: 'Workspace has reached the member limit (max 30).',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_LAST_ADMIN_CANNOT_LEAVE: {
    message:
      'You are the last admin. Please assign another admin before leaving.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  WORKSPACE_LAST_ADMIN_CANNOT_CHANGE_ROLE: {
    message:
      "Cannot change the last admin's role. Please assign another admin first.",
    category: ErrorCategory.USER_FEEDBACK,
  },

  PROJECT_NOT_FOUND: notFound('Project'),
  PROJECT_ACCESS_DENIED: {
    message: "You don't have access to this project.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_OWNER_ONLY: {
    message: 'Only the project owner can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_ADMIN_REQUIRED: {
    message: 'Only project admins can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_WORKSPACE_MISMATCH: {
    message: 'This project is not part of the current workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_SETTINGS_TOO_LARGE: SETTINGS_TOO_LARGE,
  PROJECT_ALREADY_DELETED: {
    message: 'This project has been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_CANNOT_ASSIGN_HIGHER_ROLE: {
    message: 'You cannot assign a role higher than your own.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_CANNOT_MODIFY_HIGHER_ROLE_MEMBER: {
    message: 'You cannot modify members with higher roles than yours.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  PROJECT_CANNOT_CHANGE_OWN_ROLE: {
    message: 'You cannot change your own role.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_LAST_OWNER_CANNOT_BE_REMOVED: {
    message: 'Cannot remove the last owner. Please assign another owner first.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_WORKSPACE_MEMBERSHIP_REQUIRED: {
    message: 'You must be a workspace member to access this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_LIMIT_EXCEEDED: {
    message: 'Project has reached the member limit (max 30).',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_NOT_FOUND: {
    message: 'Member not found in this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PROJECT_MEMBER_ALREADY_EXISTS: {
    message: 'This user is already a member of this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  SHARE_LINK_NOT_FOUND: notFound('Share link'),
  SHARE_LINK_EXPIRED: {
    message: 'This share link has expired. Please request a new one.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_REVOKED: {
    message: 'This share link has been deactivated.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_INVALID: {
    message: 'Invalid share link. Please check and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  SHARE_LINK_INVALID_PROJECT_ID: { message: 'Invalid project ID.', category: ErrorCategory.SILENT },
  SHARE_LINK_INVALID_TOKEN_HASH: { message: 'Invalid token hash.', category: ErrorCategory.SILENT },
  SHARE_LINK_INVALID_ROLE: { message: 'Invalid role.', category: ErrorCategory.SILENT },
  SHARE_LINK_INVALID_EXPIRATION: {
    message: 'Expiration date must be in the future.',
    category: ErrorCategory.USER_FEEDBACK,
  },
};

export function getErrorInfo(code: string, fallbackMessage: string): ErrorInfo {
  return (
    ErrorMessages[code] ?? {
      message: fallbackMessage,
      category: ErrorCategory.SILENT,
    }
  );
}
