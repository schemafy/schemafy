export type MutationResponse<T = null> = {
  data: T;
  affectedTableIds: string[];
};

export type SchemaResponse = {
  id: string;
  projectId: string;
  dbVendorName: string;
  name: string;
  charset: string;
  collation: string;
};

export type CreateSchemaRequest = {
  projectId: string;
  dbVendorName: string;
  name: string;
  charset: string;
  collation: string;
};

export type ChangeSchemaNameRequest = {
  newName: string;
};

export type TableResponse = {
  id: string;
  schemaId: string;
  name: string;
  charset: string;
  collation: string;
  extra: string | null;
};

export type CreateTableRequest = {
  schemaId: string;
  name: string;
  charset: string;
  collation: string;
};

export type ChangeTableNameRequest = {
  newName: string;
};

export type ChangeTableMetaRequest = {
  charset?: string;
  collation?: string;
};

export type ChangeTableExtraRequest = {
  extra: string;
};

export type ColumnLengthScale = {
  length: number | null;
  precision: number | null;
  scale: number | null;
};

export type ColumnResponse = {
  id: string;
  tableId: string;
  name: string;
  dataType: string;
  lengthScale: ColumnLengthScale;
  seqNo: number;
  autoIncrement: boolean;
  charset: string;
  collation: string;
  comment: string;
};

export type CreateColumnRequest = {
  tableId: string;
  name: string;
  dataType: string;
  length?: number | null;
  precision?: number | null;
  scale?: number | null;
  autoIncrement: boolean;
  charset: string;
  collation: string;
  comment?: string;
};

export type ChangeColumnNameRequest = {
  newName: string;
};

export type ChangeColumnTypeRequest = {
  dataType: string;
  length?: number | null;
  precision?: number | null;
  scale?: number | null;
};

export type ChangeColumnMetaRequest = {
  autoIncrement?: boolean;
  charset?: string;
  collation?: string;
  comment?: string;
};

export type ChangeColumnPositionRequest = {
  seqNo: number;
};

export type IndexResponse = {
  id: string;
  tableId: string;
  name: string;
  type: string;
};

export type IndexColumnResponse = {
  id: string;
  indexId: string;
  columnId: string;
  seqNo: number;
  sortDirection: string;
};

export type CreateIndexColumnRequest = {
  columnId: string;
  seqNo: number;
  sortDirection: string;
};

export type CreateIndexRequest = {
  tableId: string;
  name: string;
  type: string;
  columns?: CreateIndexColumnRequest[];
};

export type ChangeIndexNameRequest = {
  newName: string;
};

export type ChangeIndexTypeRequest = {
  type: string;
};

export type AddIndexColumnRequest = {
  columnId: string;
  seqNo: number;
  sortDirection: string;
};

export type AddIndexColumnResponse = {
  id: string;
  indexId: string;
  columnId: string;
  seqNo: number;
  sortDirection: string;
};

export type ChangeIndexColumnPositionRequest = {
  seqNo: number;
};

export type ChangeIndexColumnSortDirectionRequest = {
  sortDirection: string;
};

export type IndexSnapshotResponse = {
  index: IndexResponse;
  columns: IndexColumnResponse[];
};

export type ConstraintResponse = {
  id: string;
  tableId: string;
  name: string;
  kind: string;
  checkExpr: string | null;
  defaultExpr: string | null;
};

export type ConstraintColumnResponse = {
  id: string;
  constraintId: string;
  columnId: string;
  seqNo: number;
};

export type CreateConstraintColumnRequest = {
  columnId: string;
  seqNo: number;
};

export type CreateConstraintRequest = {
  tableId: string;
  name: string;
  kind: string;
  checkExpr?: string | null;
  defaultExpr?: string | null;
  columns?: CreateConstraintColumnRequest[];
};

export type ChangeConstraintNameRequest = {
  newName: string;
};

export type ChangeConstraintCheckExprRequest = {
  checkExpr?: string | null;
};

export type ChangeConstraintDefaultExprRequest = {
  defaultExpr?: string | null;
};

export type AddConstraintColumnRequest = {
  columnId: string;
  seqNo: number;
};

export type CascadeCreatedColumnResponse = {
  fkColumnId: string;
  fkColumnName: string;
  fkTableId: string;
  relationshipColumnId: string;
  relationshipId: string;
  pkConstraintColumnId: string;
  pkConstraintId: string;
};

export type AddConstraintColumnResponse = {
  id: string;
  constraintId: string;
  columnId: string;
  seqNo: number;
  cascadeCreatedColumns: CascadeCreatedColumnResponse[];
};

export type ChangeConstraintColumnPositionRequest = {
  seqNo: number;
};

export type ConstraintSnapshotResponse = {
  constraint: ConstraintResponse;
  columns: ConstraintColumnResponse[];
};

export type RelationshipResponse = {
  id: string;
  fkTableId: string;
  pkTableId: string;
  name: string;
  kind: string;
  cardinality: string;
  extra: string | null;
};

export type RelationshipColumnResponse = {
  id: string;
  relationshipId: string;
  pkColumnId: string;
  fkColumnId: string;
  seqNo: number;
};

export type CreateRelationshipRequest = {
  fkTableId: string;
  pkTableId: string;
  kind: string;
  cardinality: string;
};

export type ChangeRelationshipNameRequest = {
  newName: string;
};

export type ChangeRelationshipKindRequest = {
  kind: string;
};

export type ChangeRelationshipCardinalityRequest = {
  cardinality: string;
};

export type ChangeRelationshipExtraRequest = {
  extra: string;
};

export type AddRelationshipColumnRequest = {
  pkColumnId: string;
  fkColumnId: string;
  seqNo: number;
};

export type AddRelationshipColumnResponse = {
  id: string;
  relationshipId: string;
  pkColumnId: string;
  fkColumnId: string;
  seqNo: number;
};

export type ChangeRelationshipColumnPositionRequest = {
  seqNo: number;
};

export type RelationshipSnapshotResponse = {
  relationship: RelationshipResponse;
  columns: RelationshipColumnResponse[];
};

export type TableSnapshotResponse = {
  table: TableResponse;
  columns: ColumnResponse[];
  constraints: ConstraintSnapshotResponse[];
  relationships: RelationshipSnapshotResponse[];
};
