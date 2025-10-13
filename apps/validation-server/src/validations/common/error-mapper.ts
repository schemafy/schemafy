import type { ErrorDetail } from './index';

export function toErrorDetails(error: unknown): ErrorDetail[] {
    if (Array.isArray(error)) {
        return error.map((e) => toErrorDetails(e)).flat();
    }

    if (error && typeof error === 'object') {
        const anyErr = error as { name?: string; message?: string; code?: string };
        const code = anyErr.code || anyErr.name || 'VALIDATION_ERROR';
        const message = anyErr.message || 'Unknown error';
        return [{ code, message }];
    }

    return [{ code: 'VALIDATION_ERROR', message: String(error) }];
}
