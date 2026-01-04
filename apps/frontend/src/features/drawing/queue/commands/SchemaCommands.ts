import type { Database, Schema } from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { ErdStore } from '@/store/erd.store';
import {
  createSchemaAPI,
  updateSchemaNameAPI,
  deleteSchemaAPI,
} from '../../api/schema.api';

export class CreateSchemaCommand extends BaseCommand {
  constructor(private schemaData: Schema) {
    super('CREATE_SCHEMA', 'schema', schemaData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.createSchema(this.schemaData);
  }

  async executeAPI(db: Database) {
    const response = await createSchemaAPI({
      database: db,
      schema: {
        id: this.schemaData.id,
        projectId: this.schemaData.projectId,
        dbVendorId: this.schemaData.dbVendorId,
        name: this.schemaData.name,
        charset: this.schemaData.charset ?? '',
        collation: this.schemaData.collation ?? '',
        vendorOption: this.schemaData.vendorOption ?? '',
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaData.id] || this.schemaData.id;

    return new CreateSchemaCommand({
      ...this.schemaData,
      id: mappedSchemaId,
    });
  }

  getContext() {
    return {
      schemaId: this.schemaData.id,
    };
  }
}

export class UpdateSchemaNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private newName: string,
  ) {
    super('UPDATE_SCHEMA_NAME', 'schema', schemaId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeSchemaName(this.schemaId, this.newName);
  }

  async executeAPI(db: Database) {
    await updateSchemaNameAPI(this.schemaId, {
      database: db,
      schemaId: this.schemaId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;

    return new UpdateSchemaNameCommand(mappedSchemaId, this.newName);
  }

  getContext() {
    return {
      schemaId: this.schemaId,
    };
  }
}

export class DeleteSchemaCommand extends BaseCommand {
  constructor(private schemaId: string) {
    super('DELETE_SCHEMA', 'schema', schemaId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.deleteSchema(this.schemaId);
  }

  async executeAPI(db: Database) {
    await deleteSchemaAPI(this.schemaId, {
      database: db,
      schemaId: this.schemaId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;

    return new DeleteSchemaCommand(mappedSchemaId);
  }

  getContext() {
    return {
      schemaId: this.schemaId,
    };
  }
}
