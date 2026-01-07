import type { Database } from '@schemafy/validator';
import { ulid } from 'ulid';
import type { AffectedMappingResponse } from '../api/types/common';
import type { EntityType, SyncContext } from '../types';
import type { ErdStore } from '@/store/erd.store';

export interface IdMapping {
  [tempId: string]: string;
}

export interface Command {
  readonly id: string;
  readonly type: string;
  readonly entityType: EntityType;
  readonly entityId: string;
  readonly timestamp: number;

  applyToSyncedStore(syncedStore: ErdStore): void;
  executeAPI(db: Database): Promise<Partial<AffectedMappingResponse>>;
  withMappedIds(mapping: IdMapping): Command;
  getContext(): SyncContext;
}

export abstract class BaseCommand implements Command {
  readonly id: string;
  readonly timestamp: number;

  constructor(
    public readonly type: string,
    public readonly entityType: EntityType,
    public readonly entityId: string,
  ) {
    this.id = ulid();
    this.timestamp = Date.now();
  }

  abstract applyToSyncedStore(syncedStore: ErdStore): void;
  abstract executeAPI(db: Database): Promise<Partial<AffectedMappingResponse>>;
  abstract withMappedIds(mapping: IdMapping): Command;
  abstract getContext(): SyncContext;
}
