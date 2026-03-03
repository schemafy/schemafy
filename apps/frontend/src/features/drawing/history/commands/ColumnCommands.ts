import type { QueryClient } from '@tanstack/react-query';
import type { ChangeColumnNameRequest, ChangeColumnTypeRequest, ColumnResponse, CreateColumnRequest } from '../../api';
import { changeColumnName, changeColumnPosition, changeColumnType, createColumn, deleteColumn } from '../../api';
import type { ErdCommand } from '../ErdCommand';
import { updateAffectedTablesInCache } from '../erdCacheHelpers';

type ColumnCommandBase = {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateColumnCommand implements ErdCommand {
  private currentColumnId: string;

  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      tableId: string;
      originalRequest: CreateColumnRequest;
    },
  ) {
    this.currentColumnId = params.columnId;
  }

  async undo(): Promise<void> {
    const result = await deleteColumn(this.currentColumnId);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const result = await createColumn(this.params.originalRequest);
    this.currentColumnId = result.data.id;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class DeleteColumnCommand implements ErdCommand {
  private restoredColumnId: string | null = null;

  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      tableId: string;
      columnData: ColumnResponse;
      seqNo: number;
    },
  ) {
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

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const idToDelete =
      this.restoredColumnId ?? this.params.columnId;
    const result = await deleteColumn(idToDelete);
    this.restoredColumnId = null;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeColumnNameCommand implements ErdCommand {
  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      previousName: string;
      newName: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const newNameData: ChangeColumnNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeColumnName(this.params.columnId, newNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const newNameData: ChangeColumnNameRequest = {
      newName: this.params.newName
    };

    const result = await changeColumnName(this.params.columnId, newNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class ChangeColumnTypeCommand implements ErdCommand {
  constructor(
    private params: ColumnCommandBase & {
      columnId: string;
      previousType: string;
      newType: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const newTypeData: ChangeColumnTypeRequest = {
      dataType: this.params.previousType
    };

    const result = await changeColumnType(this.params.columnId, newTypeData);
    
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const newTypeData: ChangeColumnTypeRequest = {
      dataType: this.params.newType
    };

    const result = await changeColumnType(this.params.columnId, newTypeData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}
