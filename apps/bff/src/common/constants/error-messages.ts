import { ErrorCategory, ErrorCategoryType } from '../types/index.js';

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
  C001: SOMETHING_WENT_WRONG,
  C002: { message: 'Invalid parameter.', category: ErrorCategory.SILENT },
  C003: { message: 'API version is missing.', category: ErrorCategory.SILENT },
  C004: {
    message: 'Invalid API version format. (e.g., v1.0, v2.1)',
    category: ErrorCategory.SILENT,
  },
  C005: CHECK_INPUT,
  C006: {
    message: "The requested item doesn't exist or may have been moved.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  C007: SOMETHING_WENT_WRONG,
  C008: {
    message: 'This item has already been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  A001: {
    message: 'Please sign in to continue.',
    category: ErrorCategory.AUTO_HANDLE,
  },
  A002: {
    message: "You don't have permission to perform this action.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  A003: SESSION_EXPIRED,
  A004: SESSION_EXPIRED,
  A005: SESSION_EXPIRED,
  A006: SESSION_EXPIRED,
  A007: SESSION_EXPIRED,
  A008: SESSION_EXPIRED,
  A009: SESSION_EXPIRED,
  A010: SESSION_EXPIRED,

  U001: {
    message: 'User not found. Please check and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  U002: {
    message: 'This email is already registered.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  U003: {
    message: 'Invalid email or password. Please try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  V001: CHECK_INPUT,
  V002: {
    message: 'Service is temporarily unavailable. Please try again later.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  V003: {
    message: 'Request timed out. Please try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  V004: SOMETHING_WENT_WRONG,

  E001: notFound('Schema'),
  E002: notFound('Table'),
  E003: notFound('Column'),
  E004: notFound('Constraint'),
  E005: notFound('Index'),
  E006: notFound('Relationship'),
  E007: notFound('Constraint column'),
  E008: notFound('Index column'),
  E009: notFound('Relationship column'),
  E010: {
    message: 'Unsupported database type.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  E011: notFound('Memo'),
  E012: notFound('Comment'),

  W001: notFound('Workspace'),
  W002: {
    message: "You don't have access to this workspace.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  W003: {
    message: 'Failed to save settings. Please check the format and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W004: SETTINGS_TOO_LARGE,
  W005: {
    message: 'This workspace has been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W006: {
    message: 'Only admins can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W007: {
    message: 'This user is already a member of this workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W008: {
    message: 'Member not found in this workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W009: {
    message: 'Workspace has reached the member limit (max 30).',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W010: {
    message:
      'You are the last admin. Please assign another admin before leaving.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  W011: {
    message:
      "Cannot change the last admin's role. Please assign another admin first.",
    category: ErrorCategory.USER_FEEDBACK,
  },

  P001: notFound('Project'),
  P002: {
    message: "You don't have access to this project.",
    category: ErrorCategory.USER_FEEDBACK,
  },
  P003: {
    message: 'Only the project owner can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  P004: {
    message: 'Only project admins can perform this action.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  P005: {
    message: 'This project is not part of the current workspace.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  P006: SETTINGS_TOO_LARGE,
  P007: {
    message: 'This project has been deleted.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  P008: {
    message: 'You cannot assign a role higher than your own.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  P009: {
    message: 'You cannot modify members with higher roles than yours.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  PM001: {
    message: 'You cannot change your own role.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PM002: {
    message: 'Cannot remove the last owner. Please assign another owner first.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PM003: {
    message: 'You must be a workspace member to access this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PM004: {
    message: 'Project has reached the member limit (max 30).',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PM005: {
    message: 'Member not found in this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  PM006: {
    message: 'This user is already a member of this project.',
    category: ErrorCategory.USER_FEEDBACK,
  },

  S001: notFound('Share link'),
  S002: {
    message: 'This share link has expired. Please request a new one.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  S003: {
    message: 'This share link has been deactivated.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  S004: {
    message: 'Invalid share link. Please check and try again.',
    category: ErrorCategory.USER_FEEDBACK,
  },
  S005: { message: 'Invalid project ID.', category: ErrorCategory.SILENT },
  S006: { message: 'Invalid token hash.', category: ErrorCategory.SILENT },
  S007: { message: 'Invalid role.', category: ErrorCategory.SILENT },
  S008: {
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
