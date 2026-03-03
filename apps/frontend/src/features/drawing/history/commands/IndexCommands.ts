import type { QueryClient } from '@tanstack/react-query';
import type {
  AddIndexColumnRequest,
  ChangeIndexColumnSortDirectionRequest,
  ChangeIndexNameRequest,
  ChangeIndexTypeRequest,
  CreateIndexRequest,
  IndexSnapshotResponse,
} from '../../api';
import {
  addIndexColumn,
  changeIndexColumnSortDirection,
  changeIndexName,
  changeIndexType,
  createIndex,
  deleteIndex,
  removeIndexColumn,
} from '../../api';
import type { ErdCommand } from '../ErdCommand';
import { updateAffectedTablesInCache } from '../erdCacheHelpers';

interface IndexCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateIndexCommand implements ErdCommand {
  private currentIndexId: string;

  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      originalRequest: CreateIndexRequest;
    },
  ) {
    this.currentIndexId = params.indexId;
  }

  async undo(): Promise<void> {
    const result = await deleteIndex(this.currentIndexId);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const result = await createIndex(this.params.originalRequest);

    this.currentIndexId = result.data.id;
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class DeleteIndexCommand implements ErdCommand {
  private restoredIndexId: string | null = null;

  constructor(
    private params: IndexCommandBase & {
      snapshot: IndexSnapshotResponse;
    },
  ) {
  }

  async undo(): Promise<void> {
    const {snapshot, schemaId, queryClient} = this.params;
    const {index, columns} = snapshot;

    const newIndexData: CreateIndexRequest = {
      tableId: index.tableId,
      name: index.name,
      type: index.type,
    };

    const result = await createIndex(newIndexData);
    this.restoredIndexId = result.data.id;

    for (const col of columns) {
      await addIndexColumn(result.data.id, {
        columnId: col.columnId,
        seqNo: col.seqNo,
        sortDirection: col.sortDirection,
      });
    }

    await updateAffectedTablesInCache(queryClient, schemaId, result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredIndexId) return;

    const result = await deleteIndex(this.restoredIndexId);
    this.restoredIndexId = null;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeIndexNameCommand implements ErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      previousName: string;
      newName: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeIndexNameData: ChangeIndexNameRequest = {
      newName: this.params.previousName
    };

    const result = await changeIndexName(this.params.indexId, changeIndexNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeIndexNameData: ChangeIndexNameRequest = {
      newName: this.params.newName
    };

    const result = await changeIndexName(this.params.indexId, changeIndexNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeIndexTypeCommand implements ErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      previousType: string;
      newType: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeIndexTypeData: ChangeIndexTypeRequest = {
      type: this.params.newType
    };

    const result = await changeIndexType(this.params.indexId, changeIndexTypeData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeIndexTypeData: ChangeIndexTypeRequest = {
      type: this.params.newType
    };

    const result = await changeIndexType(this.params.indexId, changeIndexTypeData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class AddIndexColumnCommand implements ErdCommand {
  private currentIndexColumnId: string;

  constructor(
    private params: IndexCommandBase & {
      indexColumnId: string;
      indexId: string;
      columnId: string;
      seqNo: number;
      sortDirection: string;
    },
  ) {
    this.currentIndexColumnId = params.indexColumnId;
  }

  async undo(): Promise<void> {
    const result = await removeIndexColumn(this.currentIndexColumnId);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const newIndexColumnData: AddIndexColumnRequest = {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
      sortDirection: this.params.sortDirection,
    };

    const result = await addIndexColumn(this.params.indexId, newIndexColumnData);
    this.currentIndexColumnId = result.data.id;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class RemoveIndexColumnCommand implements ErdCommand {
  private restoredIndexColumnId: string | null = null;

  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      columnId: string;
      seqNo: number;
      sortDirection: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const addIndexColumnData: AddIndexColumnRequest = {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
      sortDirection: this.params.sortDirection,
    };

    const result = await addIndexColumn(this.params.indexId, addIndexColumnData);
    this.restoredIndexColumnId = result.data.id;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    if (!this.restoredIndexColumnId) return;

    const result = await removeIndexColumn(this.restoredIndexColumnId);
    this.restoredIndexColumnId = null;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeIndexColumnSortDirectionCommand implements ErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexColumnId: string;
      previousSortDirection: string;
      newSortDirection: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeIndexColumnSortDirectionData: ChangeIndexColumnSortDirectionRequest = {
      sortDirection: this.params.previousSortDirection
    };

    const result = await changeIndexColumnSortDirection(
      this.params.indexColumnId,
      changeIndexColumnSortDirectionData,
    );

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeIndexColumnSortDirectionData: ChangeIndexColumnSortDirectionRequest = {
      sortDirection: this.params.newSortDirection
    };

    const result = await changeIndexColumnSortDirection(
      this.params.indexColumnId,
      changeIndexColumnSortDirectionData,
    );
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}
