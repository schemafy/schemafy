import type { QueryClient } from '@tanstack/react-query';
import type { ChangeColumnNameRequest, ChangeColumnTypeRequest, ColumnResponse, CreateColumnRequest } from '../../api';
import { changeColumnName, changeColumnPosition, changeColumnType, createColumn, deleteColumn } from '../../api';
import { BaseErdCommand } from '../erdCacheHelpers';

type ColumnCommandBase = {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateColumnCommand extends BaseErdCommand {
  private currentColumnId: string;

  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      tableId: string;
      originalRequest: CreateColumnRequest;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentColumnId = params.columnId;
  }

  async undo(): Promise<void> {
    const result = await deleteColumn(this.currentColumnId);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const result = await createColumn(this.params.originalRequest);
    this.currentColumnId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }
}

export class DeleteColumnCommand extends BaseErdCommand {
  private restoredColumnId: string | null = null;

  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      tableId: string;
      columnData: ColumnResponse;
      seqNo: number;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const {columnData, tableId, seqNo} = this.params;

    const createColumnData: CreateColumnRequest = {
      tableId,
      name: columnData.name,
      dataType: columnData.dataType,
      length: columnData.lengthScale.length,
      precision: columnData.lengthScale.precision,
      scale: columnData.lengthScale.scale,
      autoIncrement: columnData.autoIncrement,
      charset: columnData.charset,
      collation: columnData.collation,
      comment: columnData.comment || undefined,
    };

    const result = await createColumn(createColumnData);
    this.restoredColumnId = result.data.id;

    if (seqNo !== result.data.seqNo) {
      await changeColumnPosition(result.data.id, {seqNo});
    }

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const idToDelete =
      this.restoredColumnId ?? this.params.columnId;
    const result = await deleteColumn(idToDelete);
    this.restoredColumnId = null;

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeColumnNameCommand extends BaseErdCommand {
  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      previousName: string;
      newName: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const newNameData: ChangeColumnNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeColumnName(this.params.columnId, newNameData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const newNameData: ChangeColumnNameRequest = {
      newName: this.params.newName
    };

    const result = await changeColumnName(this.params.columnId, newNameData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class ChangeColumnTypeCommand extends BaseErdCommand {
  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      previousType: string;
      newType: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const newTypeData: ChangeColumnTypeRequest = {
      dataType: this.params.previousType
    };

    const result = await changeColumnType(this.params.columnId, newTypeData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const newTypeData: ChangeColumnTypeRequest = {
      dataType: this.params.newType
    };

    const result = await changeColumnType(this.params.columnId, newTypeData);

    await this.updateCache(result.affectedTableIds);
  }
}
