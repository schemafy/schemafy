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
import { BaseErdCommand } from '../erdCacheHelpers';

interface IndexCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateIndexCommand extends BaseErdCommand {
  private currentIndexId: string;

  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      originalRequest: CreateIndexRequest;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentIndexId = params.indexId;
  }

  async undo(): Promise<void> {
    const result = await deleteIndex(this.currentIndexId);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const result = await createIndex(this.params.originalRequest);

    this.currentIndexId = result.data.id;
    await this.updateCache(result.affectedTableIds);
  }
}

export class DeleteIndexCommand extends BaseErdCommand {
  private restoredIndexId: string | null = null;

  constructor(
    private params: IndexCommandBase & {
      snapshot: IndexSnapshotResponse;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const {snapshot} = this.params;
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

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredIndexId) return;

    const result = await deleteIndex(this.restoredIndexId);
    this.restoredIndexId = null;

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeIndexNameCommand extends BaseErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      previousName: string;
      newName: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeIndexNameData: ChangeIndexNameRequest = {
      newName: this.params.previousName
    };

    const result = await changeIndexName(this.params.indexId, changeIndexNameData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeIndexNameData: ChangeIndexNameRequest = {
      newName: this.params.newName
    };

    const result = await changeIndexName(this.params.indexId, changeIndexNameData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeIndexTypeCommand extends BaseErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      previousType: string;
      newType: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeIndexTypeData: ChangeIndexTypeRequest = {
      type: this.params.newType
    };

    const result = await changeIndexType(this.params.indexId, changeIndexTypeData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeIndexTypeData: ChangeIndexTypeRequest = {
      type: this.params.newType
    };

    const result = await changeIndexType(this.params.indexId, changeIndexTypeData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class AddIndexColumnCommand extends BaseErdCommand {
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
    super(params.schemaId, params.queryClient);
    this.currentIndexColumnId = params.indexColumnId;
  }

  async undo(): Promise<void> {
    const result = await removeIndexColumn(this.currentIndexColumnId);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const newIndexColumnData: AddIndexColumnRequest = {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
      sortDirection: this.params.sortDirection,
    };

    const result = await addIndexColumn(this.params.indexId, newIndexColumnData);
    this.currentIndexColumnId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }
}

export class RemoveIndexColumnCommand extends BaseErdCommand {
  private restoredIndexColumnId: string | null = null;

  constructor(
    private params: IndexCommandBase & {
      indexId: string;
      columnId: string;
      seqNo: number;
      sortDirection: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const addIndexColumnData: AddIndexColumnRequest = {
      columnId: this.params.columnId,
      seqNo: this.params.seqNo,
      sortDirection: this.params.sortDirection,
    };

    const result = await addIndexColumn(this.params.indexId, addIndexColumnData);
    this.restoredIndexColumnId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    if (!this.restoredIndexColumnId) return;

    const result = await removeIndexColumn(this.restoredIndexColumnId);
    this.restoredIndexColumnId = null;

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeIndexColumnSortDirectionCommand extends BaseErdCommand {
  constructor(
    private params: IndexCommandBase & {
      indexColumnId: string;
      previousSortDirection: string;
      newSortDirection: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeIndexColumnSortDirectionData: ChangeIndexColumnSortDirectionRequest = {
      sortDirection: this.params.previousSortDirection
    };

    const result = await changeIndexColumnSortDirection(
      this.params.indexColumnId,
      changeIndexColumnSortDirectionData,
    );

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeIndexColumnSortDirectionData: ChangeIndexColumnSortDirectionRequest = {
      sortDirection: this.params.newSortDirection
    };

    const result = await changeIndexColumnSortDirection(
      this.params.indexColumnId,
      changeIndexColumnSortDirectionData,
    );
    await this.updateCache(result.affectedTableIds);
  }
}
