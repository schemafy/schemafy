import { ulid } from 'ulid';
import type { Relationship, RelationshipColumn } from '@schemafy/validator';
import { ErdStore } from '@/store/erd.store';
import { getRelationshipAPI } from '../api/relationship.api';
import {
  validateAndGetSchema,
  validateAndGetRelationship,
  ERROR_MESSAGES,
} from '../utils/entityValidators';
import { executeCommandWithValidation } from '../utils/commandQueueHelper';
import {
  CreateRelationshipCommand,
  UpdateRelationshipNameCommand,
  UpdateRelationshipCardinalityCommand,
  UpdateRelationshipExtraCommand,
  AddColumnToRelationshipCommand,
  RemoveColumnFromRelationshipCommand,
  DeleteRelationshipCommand,
} from '../queue/commands/RelationshipCommands';

const getErdStore = () => ErdStore.getInstance();

export async function getRelationship(relationshipId: string) {
  const response = await getRelationshipAPI(relationshipId);
  return response.result;
}

export function createRelationship(
  schemaId: string,
  fkTableId: string,
  pkTableId: string,
  name: string,
  kind: 'IDENTIFYING' | 'NON_IDENTIFYING',
  cardinality: '1:1' | '1:N',
  onDelete: 'NO_ACTION' | 'RESTRICT' | 'CASCADE' | 'SET_NULL' | 'SET_DEFAULT',
  onUpdate: 'NO_ACTION' | 'RESTRICT' | 'CASCADE' | 'SET_NULL' | 'SET_DEFAULT',
  fkEnforced: false,
  columns: Array<{
    fkColumnId: string;
    pkColumnId: string;
    seqNo: number;
  }>,
  extra?: string,
) {
  const erdStore = getErdStore();
  validateAndGetSchema(schemaId);

  const relationshipId = ulid();

  const relationshipColumns: RelationshipColumn[] = columns.map((col) => ({
    id: ulid(),
    relationshipId,
    fkColumnId: col.fkColumnId,
    pkColumnId: col.pkColumnId,
    seqNo: col.seqNo,
    isAffected: false,
  }));

  const newRelationship: Relationship = {
    id: relationshipId,
    fkTableId,
    pkTableId,
    name,
    kind,
    cardinality,
    onDelete,
    onUpdate,
    fkEnforced,
    columns: relationshipColumns,
    isAffected: false,
    extra,
  };

  const command = new CreateRelationshipCommand(
    schemaId,
    newRelationship,
    extra,
  );

  executeCommandWithValidation(command, () => {
    erdStore.createRelationship(schemaId, newRelationship);
  });

  return relationshipId;
}

export async function updateRelationshipName(
  schemaId: string,
  relationshipId: string,
  newName: string,
) {
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);

  const command = new UpdateRelationshipNameCommand(
    schemaId,
    relationshipId,
    newName,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeRelationshipName(schemaId, relationshipId, newName);
  });
}

export async function updateRelationshipCardinality(
  schemaId: string,
  relationshipId: string,
  cardinality: '1:1' | '1:N',
) {
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);

  const command = new UpdateRelationshipCardinalityCommand(
    schemaId,
    relationshipId,
    cardinality,
  );

  executeCommandWithValidation(command, () => {
    erdStore.changeRelationshipCardinality(
      schemaId,
      relationshipId,
      cardinality,
    );
  });
}

export async function updateRelationshipKind(
  schemaId: string,
  relationshipId: string,
  kind: 'IDENTIFYING' | 'NON_IDENTIFYING',
) {
  // TODO: relationship kind 업데이트 API 연결
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);
}

export async function updateRelationshipExtra(
  schemaId: string,
  relationshipId: string,
  extra: unknown,
) {
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);

  const command = new UpdateRelationshipExtraCommand(
    schemaId,
    relationshipId,
    extra,
  );

  executeCommandWithValidation(command, () => {
    erdStore.updateRelationshipExtra(schemaId, relationshipId, extra);
  });
}

export async function addColumnToRelationship(
  schemaId: string,
  relationshipId: string,
  fkColumnId: string,
  pkColumnId: string,
  seqNo: number,
) {
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);

  const relationshipColumnId = ulid();

  const newRelationshipColumn: RelationshipColumn = {
    id: relationshipColumnId,
    relationshipId,
    fkColumnId,
    pkColumnId,
    seqNo,
    isAffected: false,
  };

  const command = new AddColumnToRelationshipCommand(
    schemaId,
    relationshipId,
    newRelationshipColumn,
  );

  executeCommandWithValidation(command, () => {
    erdStore.addColumnToRelationship(
      schemaId,
      relationshipId,
      newRelationshipColumn,
    );
  });
}

export async function removeColumnFromRelationship(
  schemaId: string,
  relationshipId: string,
  relationshipColumnId: string,
) {
  const erdStore = getErdStore();
  const { relationship } = validateAndGetRelationship(schemaId, relationshipId);

  const relationshipColumn = relationship.columns.find(
    (c) => c.id === relationshipColumnId,
  );

  if (!relationshipColumn) {
    throw new Error(
      ERROR_MESSAGES.RELATIONSHIP_COLUMN_NOT_FOUND(relationshipColumnId),
    );
  }

  const command = new RemoveColumnFromRelationshipCommand(
    schemaId,
    relationshipId,
    relationshipColumnId,
  );

  executeCommandWithValidation(command, () => {
    erdStore.removeColumnFromRelationship(
      schemaId,
      relationshipId,
      relationshipColumnId,
    );
  });
}

export async function deleteRelationship(
  schemaId: string,
  relationshipId: string,
) {
  const erdStore = getErdStore();
  validateAndGetRelationship(schemaId, relationshipId);

  const command = new DeleteRelationshipCommand(schemaId, relationshipId);

  executeCommandWithValidation(command, () => {
    erdStore.deleteRelationship(schemaId, relationshipId);
  });
}
