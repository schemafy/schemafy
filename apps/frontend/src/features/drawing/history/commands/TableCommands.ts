import type { QueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import type {
  AddConstraintColumnRequest,
  AddIndexColumnRequest,
  AddRelationshipColumnRequest,
  ChangeTableExtraRequest,
  ChangeTableNameRequest,
  ColumnResponse,
  CreateColumnRequest,
  CreateConstraintRequest,
  CreateIndexRequest,
  CreateRelationshipRequest,
  CreateTableRequest,
  TableSnapshotResponse,
} from '../../api';
import {
  addConstraintColumn,
  addIndexColumn,
  addRelationshipColumn,
  changeTableExtra,
  changeTableName,
  createColumn,
  createConstraint,
  createIndex,
  createRelationship,
  createTable,
  deleteTable,
} from '../../api';
import { erdKeys } from '../../hooks/query-keys';
import type { ErdCommand } from '../ErdCommand';
import { removeTableFromCache, updateAffectedTablesInCache, } from '../erdCacheHelpers';

interface TableCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateTableCommand implements ErdCommand {
  private currentTableId: string;

  constructor(
    private params: TableCommandBase & {
      tableId: string;
      originalRequest: CreateTableRequest;
      originalExtra: string;
    },
  ) {
    this.currentTableId = params.tableId;
  }

  async undo(): Promise<void> {
    const result = await deleteTable(this.currentTableId);

    removeTableFromCache(
      this.params.queryClient,
      this.params.schemaId,
      this.currentTableId,
    );

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const createTableData: CreateTableRequest = {
      ...this.params.originalRequest,
      extra: this.params.originalExtra,
    };

    const result = await createTable(createTableData);
    this.currentTableId = result.data.id;

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class DeleteTableCommand implements ErdCommand {
  private restoredTableId: string | null = null;

  constructor(
    private params: TableCommandBase & {
      tableId: string;
      snapshot: TableSnapshotResponse;
    },
  ) {
  }

  async undo(): Promise<void> {
    const {schemaId, queryClient, snapshot} = this.params
    const {table, columns, constraints, indexes, relationships} = snapshot;

    const createTableData: CreateTableRequest = {
      schemaId,
      name: table.name,
      charset: table.charset,
      collation: table.collation,
      extra: table.extra ?? undefined,
    };

    const tableResult = await createTable(createTableData);
    const newTableId = tableResult.data.id;
    this.restoredTableId = newTableId;

    const columnIdMap = new Map<string, string>();
    for (const col of columns) {
      const createColumnData: CreateColumnRequest = {
        tableId: newTableId,
        name: col.name,
        dataType: col.dataType,
        length: col.lengthScale.length,
        precision: col.lengthScale.precision,
        scale: col.lengthScale.scale,
        autoIncrement: col.autoIncrement,
        charset: col.charset,
        collation: col.collation,
        comment: col.comment || undefined,
      };

      const colResult = await createColumn(createColumnData);
      columnIdMap.set(col.id, colResult.data.id);
    }

    for (const constraintSnapshot of constraints) {
      const {constraint, columns: constraintColumns} = constraintSnapshot;

      const createConstraintData: CreateConstraintRequest = {
        tableId: newTableId,
        name: constraint.name,
        kind: constraint.kind,
        checkExpr: constraint.checkExpr,
        defaultExpr: constraint.defaultExpr,
      };

      const constraintResult = await createConstraint(createConstraintData);

      for (const cc of constraintColumns) {
        const newColumnId = columnIdMap.get(cc.columnId) ?? cc.columnId;

        const addConstraintColumnData: AddConstraintColumnRequest = {
          columnId: newColumnId,
          seqNo: cc.seqNo,
        }

        await addConstraintColumn(constraintResult.data.id, addConstraintColumnData);
      }
    }

    for (const indexSnapshot of indexes) {
      const {index, columns: indexColumns} = indexSnapshot;
      const createIndexData: CreateIndexRequest = {
        tableId: newTableId,
        name: index.name,
        type: index.type,
      };

      const indexResult = await createIndex(createIndexData);

      for (const ic of indexColumns) {
        const newColumnId = columnIdMap.get(ic.columnId) ?? ic.columnId;

        const addIndexColumnData: AddIndexColumnRequest = {
          columnId: newColumnId,
          seqNo: ic.seqNo,
          sortDirection: ic.sortDirection,
        };

        await addIndexColumn(indexResult.data.id, addIndexColumnData);
      }
    }

    const currentSnapshots =
      queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(schemaId),
      );

    const affectedIds = new Set<string>([newTableId]);

    for (const relSnapshot of relationships) {
      const {relationship, columns: relColumns} = relSnapshot;
      const isFkTable = relationship.fkTableId === snapshot.table.id;
      const otherTableId = isFkTable
        ? relationship.pkTableId
        : relationship.fkTableId;

      if (!currentSnapshots || !currentSnapshots[otherTableId]) {
        toast.info('반대쪽 테이블이 없어 일부 관계를 복원하지 못했습니다.');
        continue;
      }

      const fkTableId = isFkTable ? newTableId : relationship.fkTableId;
      const pkTableId = isFkTable ? relationship.pkTableId : newTableId;

      const createRelationshipData: CreateRelationshipRequest = {
        fkTableId,
        pkTableId,
        kind: relationship.kind,
        cardinality: relationship.cardinality,
        extra: relationship.extra ?? undefined,
      };

      const relResult = await createRelationship(createRelationshipData);

      for (const rc of relColumns) {
        const fkTableColumnId = columnIdMap.get(rc.fkColumnId) ?? rc.fkColumnId;
        const fkColumnId = isFkTable
          ? fkTableColumnId
          : rc.fkColumnId;

        const pkTableColumnId = columnIdMap.get(rc.pkColumnId) ?? rc.pkColumnId;
        const pkColumnId = isFkTable
          ? rc.pkColumnId
          : pkTableColumnId;

        const addRelationshipColumnData: AddRelationshipColumnRequest = {
          fkColumnId,
          pkColumnId,
          seqNo: rc.seqNo,
        };

        await addRelationshipColumn(relResult.data.id, addRelationshipColumnData);
      }

      affectedIds.add(otherTableId);
    }

    await updateAffectedTablesInCache(
      queryClient,
      schemaId,
      Array.from(affectedIds),
    );
  }

  async redo(): Promise<void> {
    if (!this.restoredTableId) return;

    const result = await deleteTable(this.restoredTableId);

    removeTableFromCache(
      this.params.queryClient,
      this.params.schemaId,
      this.restoredTableId,
    );

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
    this.restoredTableId = null;
  }
}

export class ChangeTableNameCommand implements ErdCommand {
  constructor(
    private params: TableCommandBase & {
      tableId: string;
      previousName: string;
      newName: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeTableNameData: ChangeTableNameRequest = {
      newName: this.params.previousName,
    };

    const result = await changeTableName(this.params.tableId, changeTableNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeTableNameData: ChangeTableNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeTableName(this.params.tableId, changeTableNameData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }
}

export class MoveTableCommand implements ErdCommand {
  constructor(
    private params: TableCommandBase & {
      tableId: string;
      previousExtra: string;
      newExtra: string;
    },
  ) {
  }

  async undo(): Promise<void> {
    const changeTableExtraData: ChangeTableExtraRequest = {
      extra: this.params.previousExtra,
    };

    const result = await changeTableExtra(this.params.tableId, changeTableExtraData);

    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  async redo(): Promise<void> {
    const changeTableExtraData: ChangeTableExtraRequest = {
      extra: this.params.newExtra,
    };

    const result = await changeTableExtra(this.params.tableId, changeTableExtraData);
    
    await updateAffectedTablesInCache(
      this.params.queryClient,
      this.params.schemaId,
      result.affectedTableIds,
    );
  }

  merge(other: ErdCommand): ErdCommand | null {
    if (!(other instanceof MoveTableCommand)) return null;
    if (other.params.tableId !== this.params.tableId) return null;

    return new MoveTableCommand({
      ...this.params,
      newExtra: other.params.newExtra,
    });
  }
}

export type { ColumnResponse };
