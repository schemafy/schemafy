import type { QueryClient } from '@tanstack/react-query';
import type { ConstraintSnapshotResponse, CreateConstraintRequest, } from '../../api';
import {
  addConstraintColumn,
  changeConstraintName,
  createConstraint,
  deleteConstraint,
  removeConstraintColumn,
} from '../../api';
import { BaseErdCommand } from '../erdCacheHelpers';

interface ConstraintCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateConstraintCommand extends BaseErdCommand {
  private currentConstraintId: string;

  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      originalRequest: CreateConstraintRequest;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentConstraintId = params.constraintId;
  }

  async undo(): Promise<void> {
    const result = await deleteConstraint(this.currentConstraintId);
    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const result = await createConstraint(this.params.originalRequest);
    this.currentConstraintId = result.data.id;
    await this.updateCache(result.affectedTableIds);
  }
}

export class DeleteConstraintCommand extends BaseErdCommand {
  private restoredConstraintId: string | null = null;

  constructor(
    private params: ConstraintCommandBase & {
      snapshot: ConstraintSnapshotResponse;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const {snapshot} = this.params;
    const {constraint, columns} = snapshot;

    const newConstraintData: CreateConstraintRequest = {
      tableId: constraint.tableId,
      name: constraint.name,
      kind: constraint.kind,
      checkExpr: constraint.checkExpr,
      defaultExpr: constraint.defaultExpr,
    };

    const result = await createConstraint(newConstraintData);
    this.restoredConstraintId = result.data.id;

    for (const col of columns) {
      await addConstraintColumn(result.data.id, {
        columnId: col.columnId,
        seqNo: col.seqNo,
      });
    }

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredConstraintId) return;
    const result = await deleteConstraint(this.restoredConstraintId);
    this.restoredConstraintId = null;
    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeConstraintNameCommand extends BaseErdCommand {
  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      previousName: string;
      newName: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const result = await changeConstraintName(this.params.constraintId, {
      newName: this.params.previousName,
    });
    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const result = await changeConstraintName(this.params.constraintId, {
      newName: this.params.newName,
    });
    await this.updateCache(result.affectedTableIds);
  }
}

export class AddConstraintColumnCommand extends BaseErdCommand {
  private currentConstraintColumnId: string;

  constructor(
    private params: ConstraintCommandBase & {
      constraintColumnId: string;
      constraintId: string;
      columnId: string;
      seqNo: number;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentConstraintColumnId = params.constraintColumnId;
  }

  async undo(): Promise<void> {
    const result = await removeConstraintColumn(this.currentConstraintColumnId);
    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const result = await addConstraintColumn(this.params.constraintId, {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
    });
    this.currentConstraintColumnId = result.data.id;
    await this.updateCache(result.affectedTableIds);
  }
}

export class RemoveConstraintColumnCommand extends BaseErdCommand {
  private restoredConstraintColumnId: string | null = null;

  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      columnId: string;
      seqNo: number;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const result = await addConstraintColumn(this.params.constraintId, {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
    });
    this.restoredConstraintColumnId = result.data.id;
    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredConstraintColumnId) return;
    const result = await removeConstraintColumn(this.restoredConstraintColumnId);
    this.restoredConstraintColumnId = null;
    await this.updateCache(result.affectedTableIds);
  }
}
