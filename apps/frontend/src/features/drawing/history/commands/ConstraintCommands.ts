import type { QueryClient } from '@tanstack/react-query';
import type { ConstraintSnapshotResponse, CreateConstraintRequest, } from '../../api';
import {
  addConstraintColumn,
  changeConstraintName,
  createConstraint,
  deleteConstraint,
  removeConstraintColumn,
} from '../../api';
import type { ErdCommand } from '../ErdCommand';
import { updateAffectedTablesInCache } from '../erdCacheHelpers';

interface ConstraintCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateConstraintCommand implements ErdCommand {
  private currentConstraintId: string;

  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      originalRequest: CreateConstraintRequest;
    },
  ) {
    this.currentConstraintId = params.constraintId;
  }

  async undo(): Promise<void> {
    const result = await deleteConstraint(this.currentConstraintId);
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const result = await createConstraint(this.params.originalRequest);
    this.currentConstraintId = result.data.id;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class DeleteConstraintCommand implements ErdCommand {
  private restoredConstraintId: string | null = null;

  constructor(
    private params: ConstraintCommandBase & {
      snapshot: ConstraintSnapshotResponse;
    },
  ) {
  }

  async undo(): Promise<void> {
    const {snapshot, schemaId, queryClient} = this.params;
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

    await updateAffectedTablesInCache(queryClient, schemaId, result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredConstraintId) return;
    const result = await deleteConstraint(this.restoredConstraintId);
    this.restoredConstraintId = null;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeConstraintNameCommand implements ErdCommand {
  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      previousName: string;
      newName: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const result = await changeConstraintName(this.params.constraintId, {
      newName: this.params.previousName,
    });
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const result = await changeConstraintName(this.params.constraintId, {
      newName: this.params.newName,
    });
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class AddConstraintColumnCommand implements ErdCommand {
  private currentConstraintColumnId: string;

  constructor(
    private params: ConstraintCommandBase & {
      constraintColumnId: string;
      constraintId: string;
      columnId: string;
      seqNo: number;
    },
  ) {
    this.currentConstraintColumnId = params.constraintColumnId;
  }

  async undo(): Promise<void> {
    const result = await removeConstraintColumn(this.currentConstraintColumnId);
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const result = await addConstraintColumn(this.params.constraintId, {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
    });
    this.currentConstraintColumnId = result.data.id;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class RemoveConstraintColumnCommand implements ErdCommand {
  private restoredConstraintColumnId: string | null = null;

  constructor(
    private params: ConstraintCommandBase & {
      constraintId: string;
      columnId: string;
      seqNo: number;
    },
  ) {
  }

  async undo(): Promise<void> {
    const result = await addConstraintColumn(this.params.constraintId, {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
    });
    this.restoredConstraintColumnId = result.data.id;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    if (!this.restoredConstraintColumnId) return;
    const result = await removeConstraintColumn(this.restoredConstraintColumnId);
    this.restoredConstraintColumnId = null;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}
