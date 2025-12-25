import { ulid } from 'ulid';
import type { Relationship, RelationshipColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import {
  createRelationshipAPI,
  getRelationshipAPI,
  updateRelationshipNameAPI,
  updateRelationshipCardinalityAPI,
  updateRelationshipExtraAPI,
  addColumnToRelationshipAPI,
  removeColumnFromRelationshipAPI,
  deleteRelationshipAPI,
} from '../api/relationship.api';
import { withOptimisticUpdate } from '../utils/optimisticUpdate';
import {
  convertCardinality,
  convertOnUpdate,
} from '../utils/relationshipHelpers';
import {
  validateAndGetSchema,
  validateAndGetRelationship,
  ERROR_MESSAGES,
} from '../utils/entityValidators';
import {
  handleRelationshipIdRemapping,
  handleRelationshipColumnIdRemapping,
  handleBatchRelationshipColumnRemapping,
} from '../utils/idRemapping';
import { handlePropagatedData } from '../utils/propagatedDataHandler';

const getErdStore = () => ErdStore.getInstance();

export async function getRelationship(relationshipId: string) {
  const response = await getRelationshipAPI(relationshipId);
  return response.result;
}

export async function createRelationship(
  schemaId: string,
  srcTableId: string,
  tgtTableId: string,
  name: string,
  kind: 'IDENTIFYING' | 'NON_IDENTIFYING',
  cardinality: '1:1' | '1:N',
  onDelete: 'NO_ACTION' | 'RESTRICT' | 'CASCADE' | 'SET_NULL' | 'SET_DEFAULT',
  onUpdate: 'NO_ACTION' | 'RESTRICT' | 'CASCADE' | 'SET_NULL' | 'SET_DEFAULT',
  fkEnforced: false,
  columns: Array<{
    fkColumnId: string;
    refColumnId: string;
    seqNo: number;
  }>,
  extra?: string,
) {
  const erdStore = getErdStore();
  const { database } = validateAndGetSchema(schemaId);

  const relationshipId = ulid();

  const relationshipColumns: RelationshipColumn[] = columns.map((col) => ({
    id: ulid(),
    relationshipId,
    fkColumnId: col.fkColumnId,
    refColumnId: col.refColumnId,
    seqNo: col.seqNo,
    isAffected: false,
  }));

  const newRelationship: Relationship = {
    id: relationshipId,
    srcTableId,
    tgtTableId,
    name,
    kind,
    cardinality,
    onDelete,
    onUpdate,
    fkEnforced,
    columns: relationshipColumns,
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () => erdStore.createRelationship(schemaId, newRelationship),
    () =>
      createRelationshipAPI(
        {
          database,
          schemaId,
          relationship: {
            id: relationshipId,
            srcTableId,
            tgtTableId,
            name,
            kind,
            cardinality: convertCardinality(cardinality),
            onDelete,
            onUpdate: convertOnUpdate(onUpdate),
            columns: relationshipColumns.map((col) => ({
              id: col.id,
              relationshipId,
              fkColumnId: col.fkColumnId,
              refColumnId: col.refColumnId,
              seqNo: col.seqNo,
            })),
          },
        },
        extra,
      ),
    () => erdStore.deleteRelationship(schemaId, relationshipId),
  );

  const finalRelationshipId = handleRelationshipIdRemapping(
    response.result ?? {},
    schemaId,
    srcTableId,
    relationshipId,
  );

  handleBatchRelationshipColumnRemapping(
    response.result ?? {},
    schemaId,
    finalRelationshipId,
  );

  handlePropagatedData(
    response.result?.propagated,
    schemaId,
    srcTableId,
    finalRelationshipId,
  );

  return finalRelationshipId;
}

export async function updateRelationshipName(
  schemaId: string,
  relationshipId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  const { database, relationship } = validateAndGetRelationship(
    schemaId,
    relationshipId,
  );

  await withOptimisticUpdate(
    () => {
      const oldName = relationship.name;
      erdStore.changeRelationshipName(schemaId, relationshipId, newName);
      return oldName;
    },
    () =>
      updateRelationshipNameAPI(relationshipId, {
        database,
        schemaId,
        relationshipId,
        newName,
      }),
    (oldName) =>
      erdStore.changeRelationshipName(schemaId, relationshipId, oldName),
  );
}

export async function updateRelationshipCardinality(
  schemaId: string,
  relationshipId: string,
  cardinality: '1:1' | '1:N',
) {
  const erdStore = getErdStore();
  const { database, relationship } = validateAndGetRelationship(
    schemaId,
    relationshipId,
  );

  await withOptimisticUpdate(
    () => {
      const oldCardinality = relationship.cardinality;
      erdStore.changeRelationshipCardinality(
        schemaId,
        relationshipId,
        cardinality,
      );
      return oldCardinality;
    },
    () =>
      updateRelationshipCardinalityAPI(relationshipId, {
        database,
        schemaId,
        relationshipId,
        cardinality: convertCardinality(cardinality),
      }),
    (oldCardinality) =>
      erdStore.changeRelationshipCardinality(
        schemaId,
        relationshipId,
        oldCardinality,
      ),
  );
}

export async function updateRelationshipExtra(
  schemaId: string,
  relationshipId: string,
  extra: unknown,
) {
  const erdStore = getErdStore();
  const { relationship } = validateAndGetRelationship(schemaId, relationshipId);

  await withOptimisticUpdate(
    () => {
      const oldExtra = relationship.extra;
      erdStore.updateRelationshipExtra(schemaId, relationshipId, extra);
      return oldExtra;
    },
    () =>
      updateRelationshipExtraAPI(relationshipId, {
        extra: JSON.stringify(extra),
      }),
    (oldExtra) =>
      erdStore.updateRelationshipExtra(schemaId, relationshipId, oldExtra),
  );
}

export async function addColumnToRelationship(
  schemaId: string,
  relationshipId: string,
  fkColumnId: string,
  refColumnId: string,
  seqNo: number,
) {
  const erdStore = getErdStore();
  const { database } = validateAndGetRelationship(schemaId, relationshipId);

  const relationshipColumnId = ulid();

  const newRelationshipColumn: RelationshipColumn = {
    id: relationshipColumnId,
    relationshipId,
    fkColumnId,
    refColumnId,
    seqNo,
    isAffected: false,
  };

  const response = await withOptimisticUpdate(
    () =>
      erdStore.addColumnToRelationship(
        schemaId,
        relationshipId,
        newRelationshipColumn,
      ),
    () =>
      addColumnToRelationshipAPI(relationshipId, {
        database,
        schemaId,
        relationshipId,
        relationshipColumn: {
          id: relationshipColumnId,
          relationshipId,
          fkColumnId,
          refColumnId,
          seqNo,
        },
      }),
    () =>
      erdStore.removeColumnFromRelationship(
        schemaId,
        relationshipId,
        relationshipColumnId,
      ),
  );

  return handleRelationshipColumnIdRemapping(
    response.result ?? {},
    schemaId,
    relationshipId,
    relationshipColumnId,
  );
}

export async function removeColumnFromRelationship(
  schemaId: string,
  relationshipId: string,
  relationshipColumnId: string,
) {
  const erdStore = getErdStore();
  const { database, relationship } = validateAndGetRelationship(
    schemaId,
    relationshipId,
  );

  const relationshipColumn = relationship.columns.find(
    (c) => c.id === relationshipColumnId,
  );

  if (!relationshipColumn) {
    throw new Error(
      ERROR_MESSAGES.RELATIONSHIP_COLUMN_NOT_FOUND(relationshipColumnId),
    );
  }

  await withOptimisticUpdate(
    () => {
      const columnSnapshot = structuredClone(relationshipColumn);
      erdStore.removeColumnFromRelationship(
        schemaId,
        relationshipId,
        relationshipColumnId,
      );
      return columnSnapshot;
    },
    () =>
      removeColumnFromRelationshipAPI(relationshipId, relationshipColumnId, {
        database,
        schemaId,
        relationshipId,
        relationshipColumnId,
      }),
    (columnSnapshot) =>
      erdStore.addColumnToRelationship(
        schemaId,
        relationshipId,
        columnSnapshot,
      ),
  );
}

export async function deleteRelationship(
  schemaId: string,
  relationshipId: string,
) {
  const erdStore = getErdStore();
  const { database, relationship } = validateAndGetRelationship(
    schemaId,
    relationshipId,
  );

  await withOptimisticUpdate(
    () => {
      const relationshipSnapshot = structuredClone(relationship);
      erdStore.deleteRelationship(schemaId, relationshipId);
      return relationshipSnapshot;
    },
    () =>
      deleteRelationshipAPI(relationshipId, {
        database,
        schemaId,
        relationshipId,
      }),
    (relationshipSnapshot) =>
      erdStore.createRelationship(schemaId, relationshipSnapshot),
  );
}
