import type { QueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import type {
  AddConstraintColumnRequest,
  AddIndexColumnRequest,
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
  changeTableExtra,
  changeTableName,
  createColumn,
  createConstraint,
  createIndex,
  createRelationship,
  createTable,
  deleteTable,
  getRelationshipColumns,
} from '../../api';
import { erdKeys } from '../../hooks/query-keys';
import type { ErdCommand } from '../ErdCommand';
import { BaseErdCommand, removeTableFromCache } from '../erdCacheHelpers';
import { restoreCascadeRelationships } from '../cascadeHelpers';

interface TableCommandBase {
  schemaId: string;
  queryClient: QueryClient;
}

export class CreateTableCommand extends BaseErdCommand {
  private currentTableId: string;

  constructor(
    private params: TableCommandBase & {
      tableId: string;
      originalRequest: CreateTableRequest;
      originalExtra: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
    this.currentTableId = params.tableId;
  }

  async undo(): Promise<void> {
    const result = await deleteTable(this.currentTableId);

    removeTableFromCache(
      this.queryClient,
      this.schemaId,
      this.currentTableId,
    );

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const createTableData: CreateTableRequest = {
      ...this.params.originalRequest,
      extra: this.params.originalExtra,
    };

    const result = await createTable(createTableData);
    this.currentTableId = result.data.id;

    await this.updateCache(result.affectedTableIds);
  }
}

export class DeleteTableCommand extends BaseErdCommand {
  private restoredTableId: string | null = null;

  constructor(
    private params: TableCommandBase & {
      tableId: string;
      snapshot: TableSnapshotResponse;
      cascadeSnapshots: Record<string, TableSnapshotResponse>;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const {snapshot} = this.params;
    const {table, columns, constraints, indexes, relationships} = snapshot;

    const fkColumnIds = new Set<string>();
    const identifyingFkColumnIds = new Set<string>();
    for (const relSnapshot of relationships) {
      if (relSnapshot.relationship.fkTableId === snapshot.table.id) {
        for (const rc of relSnapshot.columns) {
          fkColumnIds.add(rc.fkColumnId);
          if (relSnapshot.relationship.kind === 'IDENTIFYING') {
            identifyingFkColumnIds.add(rc.fkColumnId);
          }
        }
      }
    }

    const createTableData: CreateTableRequest = {
      schemaId: this.schemaId,
      name: table.name,
      charset: table.charset,
      collation: table.collation,
      extra: table.extra ?? undefined,
    };

    const tableResult = await createTable(createTableData);
    const newTableId = tableResult.data.id;
    this.restoredTableId = newTableId;

    const allAffectedTableIds = new Set<string>(tableResult.affectedTableIds);

    const columnIdMap = new Map<string, string>();
    for (const col of columns) {
      if (fkColumnIds.has(col.id)) continue;

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

    const constraintIdMap = new Map<string, string>();
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

      constraintIdMap.set(constraint.id, constraintResult.data.id);

      for (const cc of constraintColumns) {
        if (fkColumnIds.has(cc.columnId)) continue;
        const newColumnId = columnIdMap.get(cc.columnId) ?? cc.columnId;

        const addConstraintColumnData: AddConstraintColumnRequest = {
          columnId: newColumnId,
          seqNo: cc.seqNo,
        };
        await addConstraintColumn(constraintResult.data.id, addConstraintColumnData);
      }
    }

    const indexIdMap = new Map<string, string>();
    for (const indexSnapshot of indexes) {
      const {index, columns: indexColumns} = indexSnapshot;

      const createIndexData: CreateIndexRequest = {
        tableId: newTableId,
        name: index.name,
        type: index.type,
      };

      const indexResult = await createIndex(createIndexData);
      indexIdMap.set(index.id, indexResult.data.id);

      for (const ic of indexColumns) {
        if (fkColumnIds.has(ic.columnId)) continue;

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
      this.queryClient.getQueryData<Record<string, TableSnapshotResponse>>(
        erdKeys.schemaSnapshots(this.schemaId),
      );

    for (const relSnapshot of relationships) {
      const {relationship, columns: relColumns} = relSnapshot;
      const isFkTable = relationship.fkTableId === snapshot.table.id;
      const otherTableId = isFkTable ? relationship.pkTableId : relationship.fkTableId;

      if (!currentSnapshots || !currentSnapshots[otherTableId]) {
        toast.info('반대쪽 테이블이 없어 일부 관계를 복원하지 못했습니다.');
        continue;
      }

      const fkTableId = isFkTable ? newTableId : relationship.fkTableId;
      const pkTableId = isFkTable ? relationship.pkTableId : newTableId;

      const extra = relationship.extra
        ? relationship.extra.replaceAll(snapshot.table.id, newTableId)
        : undefined;

      const createRelationshipData: CreateRelationshipRequest = {
        fkTableId,
        pkTableId,
        kind: relationship.kind,
        cardinality: relationship.cardinality,
        extra,
      };

      const relResult = await createRelationship(createRelationshipData);
      for (const id of relResult.affectedTableIds) {
        allAffectedTableIds.add(id);
      }

      if (isFkTable) {
        const autoRelCols = await getRelationshipColumns(relResult.data.id);
        for (const arc of autoRelCols) {
          const originRelCol = relColumns.find((rc) => rc.pkColumnId === arc.pkColumnId);
          if (originRelCol) {
            columnIdMap.set(originRelCol.fkColumnId, arc.fkColumnId);
          }
        }
      }

      if (!isFkTable && relationship.kind === 'IDENTIFYING') {
        await restoreCascadeRelationships(
          fkTableId,
          this.params.cascadeSnapshots,
          currentSnapshots,
          allAffectedTableIds,
          createRelationship
        );
      }
    }

    for (const constraintSnapshot of constraints) {
      const {constraint, columns: constraintColumns} = constraintSnapshot;
      const newConstraintId = constraintIdMap.get(constraint.id);
      if (!newConstraintId) continue;

      for (const cc of constraintColumns) {
        if (!fkColumnIds.has(cc.columnId)) continue;
        if (identifyingFkColumnIds.has(cc.columnId) && constraint.kind === 'PRIMARY_KEY') continue;
        const newColumnId = columnIdMap.get(cc.columnId);
        if (!newColumnId) continue;
        await addConstraintColumn(newConstraintId, {columnId: newColumnId, seqNo: cc.seqNo});
      }
    }

    for (const indexSnapshot of indexes) {
      const {index, columns: indexColumns} = indexSnapshot;
      const newIndexId = indexIdMap.get(index.id);
      if (!newIndexId) continue;

      for (const ic of indexColumns) {
        if (!fkColumnIds.has(ic.columnId)) continue;
        const newColumnId = columnIdMap.get(ic.columnId);
        if (!newColumnId) continue;
        await addIndexColumn(newIndexId, {columnId: newColumnId, seqNo: ic.seqNo, sortDirection: ic.sortDirection});
      }
    }

    await this.updateCache([...allAffectedTableIds]);
  }

  async redo(): Promise<void> {
    if (!this.restoredTableId) return;

    const result = await deleteTable(this.restoredTableId);

    removeTableFromCache(
      this.queryClient,
      this.schemaId,
      this.restoredTableId,
    );

    await this.updateCache(result.affectedTableIds);
    this.restoredTableId = null;
  }
}

export class ChangeTableNameCommand extends BaseErdCommand {
  constructor(
    private params: TableCommandBase & {
      tableId: string;
      previousName: string;
      newName: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeTableNameData: ChangeTableNameRequest = {
      newName: this.params.previousName,
    };

    const result = await changeTableName(this.params.tableId, changeTableNameData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeTableNameData: ChangeTableNameRequest = {
      newName: this.params.newName,
    };

    const result = await changeTableName(this.params.tableId, changeTableNameData);

    await this.updateCache(result.affectedTableIds);
  }
}

export class MoveTableCommand extends BaseErdCommand {
  constructor(
    private params: TableCommandBase & {
      tableId: string;
      previousExtra: string;
      newExtra: string;
    },
  ) {
    super(params.schemaId, params.queryClient);
  }

  async undo(): Promise<void> {
    const changeTableExtraData: ChangeTableExtraRequest = {
      extra: this.params.previousExtra,
    };

    const result = await changeTableExtra(this.params.tableId, changeTableExtraData);

    await this.updateCache(result.affectedTableIds);
  }

  async redo(): Promise<void> {
    const changeTableExtraData: ChangeTableExtraRequest = {
      extra: this.params.newExtra,
    };

    const result = await changeTableExtra(this.params.tableId, changeTableExtraData);

    await this.updateCache(result.affectedTableIds);
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
