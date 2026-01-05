import type {
  Database,
  Relationship,
  RelationshipColumn,
} from '@schemafy/validator';
import { BaseCommand, type IdMapping } from '../Command';
import type { ErdStore } from '@/store/erd.store';
import {
  createRelationshipAPI,
  updateRelationshipNameAPI,
  updateRelationshipCardinalityAPI,
  updateRelationshipExtraAPI,
  addColumnToRelationshipAPI,
  removeColumnFromRelationshipAPI,
  deleteRelationshipAPI,
} from '../../api/relationship.api';
import {
  convertCardinality,
  convertOnUpdate,
} from '../../utils/relationshipHelpers';

export class CreateRelationshipCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipData: Relationship,
    private extra?: string,
  ) {
    super('CREATE_RELATIONSHIP', 'relationship', relationshipData.id);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.createRelationship(this.schemaId, this.relationshipData);
  }

  async executeAPI(db: Database) {
    const response = await createRelationshipAPI(
      {
        database: db,
        schemaId: this.schemaId,
        relationship: {
          id: this.relationshipData.id,
          srcTableId: this.relationshipData.srcTableId,
          tgtTableId: this.relationshipData.tgtTableId,
          name: this.relationshipData.name,
          kind: this.relationshipData.kind,
          cardinality: convertCardinality(this.relationshipData.cardinality),
          onDelete: this.relationshipData.onDelete,
          onUpdate: convertOnUpdate(this.relationshipData.onUpdate),
          columns: this.relationshipData.columns.map((col) => ({
            id: col.id,
            relationshipId: this.relationshipData.id,
            fkColumnId: col.fkColumnId,
            refColumnId: col.refColumnId,
            seqNo: col.seqNo,
          })),
        },
      },
      this.extra,
    );

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipData.id] || this.relationshipData.id;
    const mappedSrcTableId =
      mapping[this.relationshipData.srcTableId] ||
      this.relationshipData.srcTableId;
    const mappedTgtTableId =
      mapping[this.relationshipData.tgtTableId] ||
      this.relationshipData.tgtTableId;

    return new CreateRelationshipCommand(
      mappedSchemaId,
      {
        ...this.relationshipData,
        id: mappedRelationshipId,
        srcTableId: mappedSrcTableId,
        tgtTableId: mappedTgtTableId,
        columns: this.relationshipData.columns.map((col) => ({
          ...col,
          id: mapping[col.id] || col.id,
          relationshipId: mappedRelationshipId,
          fkColumnId: mapping[col.fkColumnId] || col.fkColumnId,
          refColumnId: mapping[col.refColumnId] || col.refColumnId,
        })),
      },
      this.extra,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      tableId: this.relationshipData.srcTableId,
      relationshipId: this.relationshipData.id,
    };
  }
}

export class UpdateRelationshipNameCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
    private newName: string,
  ) {
    super('UPDATE_RELATIONSHIP_NAME', 'relationship', relationshipId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeRelationshipName(
      this.schemaId,
      this.relationshipId,
      this.newName,
    );
  }

  async executeAPI(db: Database) {
    await updateRelationshipNameAPI(this.relationshipId, {
      database: db,
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
      newName: this.newName,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;

    return new UpdateRelationshipNameCommand(
      mappedSchemaId,
      mappedRelationshipId,
      this.newName,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}

export class UpdateRelationshipCardinalityCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
    private cardinality: '1:1' | '1:N',
  ) {
    super('UPDATE_RELATIONSHIP_CARDINALITY', 'relationship', relationshipId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.changeRelationshipCardinality(
      this.schemaId,
      this.relationshipId,
      this.cardinality,
    );
  }

  async executeAPI(db: Database) {
    await updateRelationshipCardinalityAPI(this.relationshipId, {
      database: db,
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
      cardinality: convertCardinality(this.cardinality),
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;

    return new UpdateRelationshipCardinalityCommand(
      mappedSchemaId,
      mappedRelationshipId,
      this.cardinality,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}

export class UpdateRelationshipExtraCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
    private newExtra: unknown,
  ) {
    super('UPDATE_RELATIONSHIP_EXTRA', 'relationship', relationshipId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.updateRelationshipExtra(
      this.schemaId,
      this.relationshipId,
      this.newExtra,
    );
  }

  async executeAPI() {
    await updateRelationshipExtraAPI(this.relationshipId, {
      extra: JSON.stringify(this.newExtra),
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;

    return new UpdateRelationshipExtraCommand(
      mappedSchemaId,
      mappedRelationshipId,
      this.newExtra,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}

export class AddColumnToRelationshipCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
    private relationshipColumnData: RelationshipColumn,
  ) {
    super(
      'ADD_COLUMN_TO_RELATIONSHIP',
      'relationshipColumn',
      relationshipColumnData.id,
    );
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.addColumnToRelationship(
      this.schemaId,
      this.relationshipId,
      this.relationshipColumnData,
    );
  }

  async executeAPI(db: Database) {
    const response = await addColumnToRelationshipAPI(this.relationshipId, {
      database: db,
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
      relationshipColumn: {
        id: this.relationshipColumnData.id,
        relationshipId: this.relationshipId,
        fkColumnId: this.relationshipColumnData.fkColumnId,
        refColumnId: this.relationshipColumnData.refColumnId,
        seqNo: this.relationshipColumnData.seqNo,
      },
    });

    return response.result ?? {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;
    const mappedRelationshipColumnId =
      mapping[this.relationshipColumnData.id] || this.relationshipColumnData.id;
    const mappedFkColumnId =
      mapping[this.relationshipColumnData.fkColumnId] ||
      this.relationshipColumnData.fkColumnId;
    const mappedRefColumnId =
      mapping[this.relationshipColumnData.refColumnId] ||
      this.relationshipColumnData.refColumnId;

    return new AddColumnToRelationshipCommand(
      mappedSchemaId,
      mappedRelationshipId,
      {
        ...this.relationshipColumnData,
        id: mappedRelationshipColumnId,
        relationshipId: mappedRelationshipId,
        fkColumnId: mappedFkColumnId,
        refColumnId: mappedRefColumnId,
      },
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}

export class RemoveColumnFromRelationshipCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
    private relationshipColumnId: string,
  ) {
    super(
      'REMOVE_COLUMN_FROM_RELATIONSHIP',
      'relationshipColumn',
      relationshipColumnId,
    );
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.removeColumnFromRelationship(
      this.schemaId,
      this.relationshipId,
      this.relationshipColumnId,
    );
  }

  async executeAPI(db: Database) {
    await removeColumnFromRelationshipAPI(
      this.relationshipId,
      this.relationshipColumnId,
      {
        database: db,
        schemaId: this.schemaId,
        relationshipId: this.relationshipId,
        relationshipColumnId: this.relationshipColumnId,
      },
    );
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;
    const mappedRelationshipColumnId =
      mapping[this.relationshipColumnId] || this.relationshipColumnId;

    return new RemoveColumnFromRelationshipCommand(
      mappedSchemaId,
      mappedRelationshipId,
      mappedRelationshipColumnId,
    );
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}

export class DeleteRelationshipCommand extends BaseCommand {
  constructor(
    private schemaId: string,
    private relationshipId: string,
  ) {
    super('DELETE_RELATIONSHIP', 'relationship', relationshipId);
  }

  applyToSyncedStore(syncedStore: ErdStore) {
    syncedStore.deleteRelationship(this.schemaId, this.relationshipId);
  }

  async executeAPI(db: Database) {
    await deleteRelationshipAPI(this.relationshipId, {
      database: db,
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    });
    return {};
  }

  withMappedIds(mapping: IdMapping) {
    const mappedSchemaId = mapping[this.schemaId] || this.schemaId;
    const mappedRelationshipId =
      mapping[this.relationshipId] || this.relationshipId;

    return new DeleteRelationshipCommand(mappedSchemaId, mappedRelationshipId);
  }

  getContext() {
    return {
      schemaId: this.schemaId,
      relationshipId: this.relationshipId,
    };
  }
}
