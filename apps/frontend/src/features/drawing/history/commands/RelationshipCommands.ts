import type { QueryClient } from '@tanstack/react-query';
import type {
  AddRelationshipColumnRequest,
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipExtraRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipNameRequest,
  CreateRelationshipRequest,
  RelationshipSnapshotResponse,
} from '../../api';
import {
  addRelationshipColumn,
  changeRelationshipCardinality,
  changeRelationshipExtra,
  changeRelationshipKind,
  changeRelationshipName,
  createRelationship,
  deleteRelationship,
} from '../../api';
import type { ErdCommand } from '../ErdCommand';
import { updateAffectedTablesInCache } from '../erdCacheHelpers';

interface RelationshipCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateRelationshipCommand implements ErdCommand {
  private currentRelationshipId: string;

  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      createRequest: CreateRelationshipRequest;
      extra: string;
    },
  ) {
    this.currentRelationshipId = params.relationshipId;
  }

  async undo(): Promise<void> {
    const result = await deleteRelationship(this.currentRelationshipId);
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const createRelationshipData: CreateRelationshipRequest = {
      ...this.params.createRequest,
      extra: this.params.extra,
    };

    const result = await createRelationship(createRelationshipData);
    this.currentRelationshipId = result.data.id;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class DeleteRelationshipCommand implements ErdCommand {
  private restoredRelationshipId: string | null = null;

  constructor(
    private params: RelationshipCommandBase & {
      snapshot: RelationshipSnapshotResponse;
    },
  ) {
  }

  async undo(): Promise<void> {
    const {relationship, columns} = this.params.snapshot;

    const createRelationshipData: CreateRelationshipRequest = {
      fkTableId: relationship.fkTableId,
      pkTableId: relationship.pkTableId,
      kind: relationship.kind,
      cardinality: relationship.cardinality,
      extra: relationship.extra ?? undefined,
    };

    const result = await createRelationship(createRelationshipData);
    this.restoredRelationshipId = result.data.id;

    for (const col of columns) {
      const addRelationshipColumnData: AddRelationshipColumnRequest = {
        fkColumnId: col.fkColumnId,
        pkColumnId: col.pkColumnId,
        seqNo: col.seqNo,
      };

      await addRelationshipColumn(result.data.id, addRelationshipColumnData);
    }

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    if (!this.restoredRelationshipId) return;

    const result = await deleteRelationship(this.restoredRelationshipId);
    this.restoredRelationshipId = null;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeRelationshipNameCommand implements ErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousName: string;
      newName: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeRelationshipNameData: ChangeRelationshipNameRequest = {
      newName: this.params.previousName,
    };

    const result = await changeRelationshipName(this.params.relationshipId, changeRelationshipNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeRelationshipNameData: ChangeRelationshipNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeRelationshipName(this.params.relationshipId, changeRelationshipNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeRelationshipKindCommand implements ErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousKind: string;
      newKind: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeRelationshipKindData: ChangeRelationshipKindRequest = {
      kind: this.params.previousKind,
    };

    const result = await changeRelationshipKind(this.params.relationshipId, changeRelationshipKindData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeRelationshipKindData: ChangeRelationshipKindRequest = {
      kind: this.params.newKind,
    };

    const result = await changeRelationshipKind(this.params.relationshipId, changeRelationshipKindData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeRelationshipCardinalityCommand implements ErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousCardinality: string;
      newCardinality: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeRelationshipCardinalityData: ChangeRelationshipCardinalityRequest = {
      cardinality: this.params.previousCardinality
    };

    const result = await changeRelationshipCardinality(
      this.params.relationshipId,
      changeRelationshipCardinalityData,
    );

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeRelationshipCardinalityData: ChangeRelationshipCardinalityRequest = {
      cardinality: this.params.newCardinality
    };

    const result = await changeRelationshipCardinality(
      this.params.relationshipId,
      changeRelationshipCardinalityData,
    );
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeRelationshipExtraCommand implements ErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousExtra: string;
      newExtra: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeRelationshipExtraData: ChangeRelationshipExtraRequest = {
      extra: this.params.previousExtra,
    };

    const result = await changeRelationshipExtra(this.params.relationshipId, changeRelationshipExtraData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeRelationshipExtraData: ChangeRelationshipExtraRequest = {
      extra: this.params.newExtra,
    };

    const result = await changeRelationshipExtra(this.params.relationshipId, changeRelationshipExtraData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  merge(other: ErdCommand): ErdCommand | null {
    if (!(other instanceof ChangeRelationshipExtraCommand)) return null;

    if (other.params.relationshipId !== this.params.relationshipId) return null;
    
    return new ChangeRelationshipExtraCommand({
      ...this.params,
      newExtra: other.params.newExtra,
    });
  }
}
