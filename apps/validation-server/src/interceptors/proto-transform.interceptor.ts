import { CallHandler, ExecutionContext, Injectable, NestInterceptor } from '@nestjs/common';
import { Observable } from 'rxjs';

import {
    normalizeOnUpdate,
    toZodConstraintKind,
    toZodDbVendor,
    toZodIndexSortDir,
    toZodIndexType,
    toZodOnDelete,
    toZodRelationshipCardinality,
    toZodRelationshipKind,
} from '../transformers/enum-transformers';
import { emptyStringToNull, parseIsoDate } from '../transformers/value-normalizers';

@Injectable()
export class ProtoTransformInterceptor implements NestInterceptor {
    private readonly DATE_KEYS = new Set(['createdAt', 'updatedAt', 'deletedAt']);
    private readonly NULLABLE_STRING_KEYS = new Set(['dataType', 'comment', 'checkExpr', 'defaultExpr']);

    intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
        const rpc = context.switchToRpc();
        const data: unknown = rpc.getData();
        this.transformInPlace(data, '');
        return next.handle() as Observable<unknown>;
    }

    private transformInPlace(input: unknown, path: string): void {
        if (Array.isArray(input)) {
            const arr = input as unknown[];
            for (let i = 0; i < arr.length; i += 1) {
                const itemPath = `${path}[${i}]`;
                const nextValue = this.transformScalar('', arr[i], itemPath);
                if (nextValue !== arr[i]) arr[i] = nextValue;
                this.transformInPlace(arr[i], itemPath);
            }
            return;
        }
        if (this.isPlainObject(input)) {
            const obj = input;
            for (const key of Object.keys(obj)) {
                const original = obj[key];
                const fieldPath = path ? `${path}.${key}` : key;
                const nextValue = this.transformScalar(key, original, fieldPath);
                if (nextValue !== original) obj[key] = nextValue;
                this.transformInPlace(obj[key], fieldPath);
            }
        }
    }

    private transformScalar(key: string, value: unknown, path: string): unknown {
        try {
            if (this.DATE_KEYS.has(key)) {
                return parseIsoDate(value as string | null | undefined);
            }

            if (this.NULLABLE_STRING_KEYS.has(key)) {
                return emptyStringToNull(value as string | null | undefined);
            }

            if (typeof value === 'string') {
                switch (key) {
                    case 'dbVendorId':
                        return toZodDbVendor(value);
                    case 'type':
                        return toZodIndexType(value);
                    case 'sortDir':
                        return toZodIndexSortDir(value);
                    case 'kind':
                        return this.transformKind(value);
                    case 'onDelete':
                        return toZodOnDelete(value);
                    case 'onUpdate':
                        return normalizeOnUpdate(value);
                    case 'cardinality':
                        return toZodRelationshipCardinality(value);
                }
            }

            if (key === 'fkEnforced' && typeof value === 'boolean') {
                if (value !== false) {
                    throw new Error('fkEnforced must be false');
                }
                return false;
            }

            return value;
        } catch (error) {
            if (error instanceof Error) {
                throw new Error(`Error transforming field '${path}': ${error.message}`);
            }
            throw error;
        }
    }

    private transformKind(value: string): string {
        try {
            return toZodConstraintKind(value);
        } catch {
            return toZodRelationshipKind(value);
        }
    }

    private isPlainObject(value: unknown): value is Record<string, unknown> {
        if (typeof value !== 'object' || value === null) return false;
        const proto = Reflect.getPrototypeOf(value);
        return proto === Object.prototype || proto === null;
    }
}
