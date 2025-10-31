import type { ErrorCode } from "./codes";

export class ERDValidationError extends Error {
  public readonly code: ErrorCode;
  public readonly details?: Record<string, unknown>;

  constructor(
    code: ErrorCode,
    message: string,
    details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "ERDValidationError";
    this.code = code;
    this.details = details;
  }
}

interface ErrorDefinition {
  code: ErrorCode;
  messageTemplate: string;
  createDetails: (...args: any[]) => Record<string, unknown>;
}

export function createErrorClass(name: string, definition: ErrorDefinition) {
  return class extends ERDValidationError {
    constructor(...args: any[]) {
      const message = definition.messageTemplate.replace(
        /\{(\d+)\}/g,
        (match, index) => {
          return (args as any[])[parseInt(index)] || match;
        },
      );
      const details = definition.createDetails(...args);
      super(definition.code, message, details);
      this.name = `${name}Error`;
    }
  };
}

export type { ErrorDefinition };
