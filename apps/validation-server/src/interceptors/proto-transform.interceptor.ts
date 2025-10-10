import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Observable } from 'rxjs';

import {
  normalizeOnUpdate,
  toZodDbVendor,
  toZodOnDelete,
  toZodRelationshipCardinality,
} from '../transformers/enum-transformers';
import {
  emptyStringToNull,
  parseIsoDate,
} from '../transformers/value-normalizers';

@Injectable()
export class ProtoTransformInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const rpc = context.switchToRpc();
    const data: unknown = rpc.getData();
    this.transformInPlace(data);
    return next.handle() as Observable<unknown>;
  }

  private transformInPlace(input: unknown): void {
    if (Array.isArray(input)) {
      const arr = input as unknown[];
      for (let i = 0; i < arr.length; i += 1) {
        const nextValue = this.transformScalar('', arr[i]);
        if (nextValue !== arr[i]) arr[i] = nextValue;
        this.transformInPlace(arr[i]);
      }
      return;
    }
    if (this.isPlainObject(input)) {
      const obj = input;
      for (const key of Object.keys(obj)) {
        const original = obj[key];
        const nextValue = this.transformScalar(key, original);
        if (nextValue !== original) obj[key] = nextValue;
        this.transformInPlace(obj[key]);
      }
    }
  }

  private transformScalar(key: string, value: unknown): unknown {
    if (key === 'createdAt' || key === 'updatedAt') {
      return parseIsoDate(value as string | null | undefined);
    }
    if (key === 'deletedAt') {
      return parseIsoDate(value as string | null | undefined);
    }

    if (key === 'dataType' || key === 'comment') {
      return emptyStringToNull(value as string | null | undefined);
    }

    if (key === 'dbVendorId' && typeof value === 'string') {
      return toZodDbVendor(value);
    }
    if (key === 'onDelete' && typeof value === 'string') {
      return toZodOnDelete(value);
    }
    if (key === 'onUpdate' && typeof value === 'string') {
      return normalizeOnUpdate(value);
    }
    if (key === 'cardinality' && typeof value === 'string') {
      return toZodRelationshipCardinality(value);
    }

    if (key === 'fkEnforced' && typeof value === 'boolean') {
      if (value !== false) {
        throw new Error('fkEnforced must be false');
      }
      return false;
    }

    return value;
  }

  private isPlainObject(value: unknown): value is Record<string, unknown> {
    if (typeof value !== 'object' || value === null) return false;
    const proto = Reflect.getPrototypeOf(value);
    return proto === Object.prototype || proto === null;
  }
}
