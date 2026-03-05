import type { QueryClient } from '@tanstack/react-query';
import type {
  ChangeRelationshipCardinalityRequest,
  ChangeRelationshipExtraRequest,
  ChangeRelationshipKindRequest,
  ChangeRelationshipNameRequest,
  CreateRelationshipRequest,
  RelationshipSnapshotResponse,
} from '../../api';
import {
  changeRelationshipCardinality,
  changeRelationshipExtra,
  changeRelationshipKind,
  changeRelationshipName,
  createRelationship,
  deleteRelationship,
} from '../../api';
import type { ErdCommand } from '../ErdCommand';
import { BaseErdCommand } from '../erdCacheHelpers';

interface RelationshipCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateRelationshipCommand extends BaseErdCommand {
  private currentRelationshipId: string;

  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      createRequest: CreateRelationshipRequest;
      extra: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentRelationshipId = params.relationshipId;
  }

  async undo(): Promise<void> {
    const result = await deleteRelationship(this.currentRelationshipId);
    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const createRelationshipData: CreateRelationshipRequest = {
      ...this.params.createRequest,
      extra: this.params.extra,
    };

    const result = await createRelationship(createRelationshipData);
    this.currentRelationshipId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }
}

export class DeleteRelationshipCommand extends BaseErdCommand {
  private restoredRelationshipId: string | null = null;

  constructor(
    private params: RelationshipCommandBase & {
      snapshot: RelationshipSnapshotResponse;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const {relationship} = this.params.snapshot;

    const createRelationshipData: CreateRelationshipRequest = {
      fkTableId: relationship.fkTableId,
      pkTableId: relationship.pkTableId,
      kind: relationship.kind,
      cardinality: relationship.cardinality,
      extra: relationship.extra ?? undefined,
    };

    const result = await createRelationship(createRelationshipData);
    this.restoredRelationshipId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredRelationshipId) return;

    const result = await deleteRelationship(this.restoredRelationshipId);
    this.restoredRelationshipId = null;

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeRelationshipNameCommand extends BaseErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousName: string;
      newName: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeRelationshipNameData: ChangeRelationshipNameRequest = {
      newName: this.params.previousName,
    };

    const result = await changeRelationshipName(this.params.relationshipId, changeRelationshipNameData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeRelationshipNameData: ChangeRelationshipNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeRelationshipName(this.params.relationshipId, changeRelationshipNameData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeRelationshipKindCommand extends BaseErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousKind: string;
      newKind: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeRelationshipKindData: ChangeRelationshipKindRequest = {
      kind: this.params.previousKind,
    };

    const result = await changeRelationshipKind(this.params.relationshipId, changeRelationshipKindData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeRelationshipKindData: ChangeRelationshipKindRequest = {
      kind: this.params.newKind,
    };

    const result = await changeRelationshipKind(this.params.relationshipId, changeRelationshipKindData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeRelationshipCardinalityCommand extends BaseErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousCardinality: string;
      newCardinality: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeRelationshipCardinalityData: ChangeRelationshipCardinalityRequest = {
      cardinality: this.params.previousCardinality
    };

    const result = await changeRelationshipCardinality(
      this.params.relationshipId,
      changeRelationshipCardinalityData,
    );

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeRelationshipCardinalityData: ChangeRelationshipCardinalityRequest = {
      cardinality: this.params.newCardinality
    };

    const result = await changeRelationshipCardinality(
      this.params.relationshipId,
      changeRelationshipCardinalityData,
    );
    await this.updateCache(result.affectedTableIds);
  }
}

export class ReconnectRelationshipCommand extends BaseErdCommand {
  private restoredOldId: string | null = null;
  private currentNewId: string;

  constructor(
    private params: RelationshipCommandBase & {
      oldSnapshot: RelationshipSnapshotResponse;
      newRelationshipId: string;
      newCreateRequest: CreateRelationshipRequest;
      newExtra: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentNewId = params.newRelationshipId;
  }

  async undo(): Promise<void> {
    const deleteResult = await deleteRelationship(this.currentNewId);

    const { relationship } = this.params.oldSnapshot;
    const createResult = await createRelationship({
      fkTableId: relationship.fkTableId,
      pkTableId: relationship.pkTableId,
      kind: relationship.kind,
      cardinality: relationship.cardinality,
      extra: relationship.extra ?? undefined,
    });
    this.restoredOldId = createResult.data.id;

    await this.updateCache([
      ...deleteResult.affectedTableIds,
      ...createResult.affectedTableIds,
    ]);
  }

  async redo(): Promise<void> {
    if (!this.restoredOldId) return;

    const deleteResult = await deleteRelationship(this.restoredOldId);
    this.restoredOldId = null;

    const createResult = await createRelationship({
      ...this.params.newCreateRequest,
      extra: this.params.newExtra,
    });
    this.currentNewId = createResult.data.id;

    await this.updateCache([
      ...deleteResult.affectedTableIds,
      ...createResult.affectedTableIds,
    ]);
  }
}

export class ChangeRelationshipExtraCommand extends BaseErdCommand {
  constructor(
    private params: RelationshipCommandBase & {
      relationshipId: string;
      previousExtra: string;
      newExtra: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeRelationshipExtraData: ChangeRelationshipExtraRequest = {
      extra: this.params.previousExtra,
    };

    const result = await changeRelationshipExtra(this.params.relationshipId, changeRelationshipExtraData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeRelationshipExtraData: ChangeRelationshipExtraRequest = {
      extra: this.params.newExtra,
    };

    const result = await changeRelationshipExtra(this.params.relationshipId, changeRelationshipExtraData);

    await this.updateCache(result.affectedTableIds);
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
