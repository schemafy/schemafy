import type { Database } from '@schemafy/validator';
import type { Command } from './Command';
import type { AffectedMappingResponse } from '../api/types/common';
import { ErdStore } from '@/store/erd.store';
import {
  handleServerResponse,
  applyLocalIdMapping,
  type IdMappingWithType,
} from '../utils/sync';
import type { SyncContext } from '../types';

type IdMappingCallback = (tempId: string, realId: string, type: string) => void;

export class CommandQueue {
  private static instance: CommandQueue;
  private queue: Command[] = [];
  private pendingEntityIds = new Set<string>();
  private idMapping = new Map<string, IdMappingWithType>();
  private syncedStore: ErdStore;
  private isProcessing = false;
  private idMappingCallbacks: IdMappingCallback[] = [];

  private constructor() {
    this.syncedStore = ErdStore.getSyncedInstance();
  }

  static getInstance() {
    if (!CommandQueue.instance) {
      CommandQueue.instance = new CommandQueue();
    }
    return CommandQueue.instance;
  }

  initialize(initialDb: Database) {
    this.syncedStore.load(initialDb);
  }

  getSyncedDb() {
    return this.syncedStore.database;
  }

  hasPendingEntity(entityId: string) {
    return this.pendingEntityIds.has(entityId);
  }

  enqueue(command: Command) {
    if (!this.syncedStore.database) {
      throw new Error('CommandQueue not initialized. Call initialize() first.');
    }

    this.queue.push(command);
    this.pendingEntityIds.add(command.entityId);

    if (!this.isProcessing) {
      this.processQueue();
    }
  }

  private async processQueue() {
    if (this.isProcessing || this.queue.length === 0) {
      return;
    }

    this.isProcessing = true;

    while (this.queue.length > 0) {
      const command = this.queue[0];

      try {
        await this.executeCommand(command);
        this.queue.shift();
      } catch (error) {
        console.error('Command execution failed:', error);
        this.queue.shift();
      }
    }

    this.isProcessing = false;
  }

  private async executeCommand(command: Command) {
    const syncedDb = this.syncedStore.database;
    if (!syncedDb) {
      throw new Error('Synced DB is not initialized');
    }

    const simplifiedMapping = Object.fromEntries(
      Array.from(this.idMapping.entries()).map(([tempId, mapping]) => [
        tempId,
        mapping.realId,
      ]),
    );

    const mappedCommand = command.withMappedIds(simplifiedMapping);

    try {
      const response = await mappedCommand.executeAPI(syncedDb);

      mappedCommand.applyToSyncedStore(this.syncedStore);

      const context = this.buildSyncContext(mappedCommand);

      const idMap = handleServerResponse(
        response as AffectedMappingResponse,
        context,
        this.syncedStore,
      );

      idMap.forEach((realId, tempId) => {
        this.idMapping.set(tempId, realId);
      });

      const localStore = ErdStore.getInstance();
      applyLocalIdMapping(idMap, localStore, this.syncedStore);

      if (this.idMappingCallbacks.length > 0) {
        idMap.forEach((realId, tempId) => {
          this.notifyIdMapping(tempId, realId.realId, realId.type);
        });
      }

      this.pendingEntityIds.delete(command.entityId);
    } catch (error) {
      console.error(
        '[CommandQueue] API request failed, rolling back to synced state:',
        error,
      );
      const localStore = ErdStore.getInstance();
      localStore.load(syncedDb);

      this.pendingEntityIds.delete(command.entityId);

      throw error;
    }
  }

  private buildSyncContext(command: Command): SyncContext {
    return command.getContext();
  }

  getRealId(tempId: string) {
    return this.idMapping.get(tempId);
  }

  onIdMapping(callback: IdMappingCallback) {
    this.idMappingCallbacks.push(callback);
    return () => {
      const index = this.idMappingCallbacks.indexOf(callback);
      if (index > -1) {
        this.idMappingCallbacks.splice(index, 1);
      }
    };
  }

  private notifyIdMapping(tempId: string, realId: string, type: string) {
    this.idMappingCallbacks.forEach((callback) => {
      try {
        callback(tempId, realId, type);
      } catch (error) {
        console.error('Error in ID mapping callback:', error);
      }
    });
  }

  clear() {
    this.queue = [];
    this.pendingEntityIds.clear();
    this.idMapping.clear();
    this.isProcessing = false;
    this.idMappingCallbacks = [];
  }
}
