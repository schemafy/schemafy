import type { Database } from '@schemafy/validator';

export interface ErrorDetail {
    code: string;
    message: string;
    meta?: Record<string, string>;
}

export interface ValidateSuccess {
    database: Database;
}

export interface ValidateFailure {
    errors: ErrorDetail[];
}

export type ValidateResult = {
    success?: ValidateSuccess;
    failure?: ValidateFailure;
};
