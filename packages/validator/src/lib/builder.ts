// 싱글톤 적용 필요

import {
  Column,
  Constraint,
  ConstraintColumn,
  Database,
  Index,
  IndexColumn,
  Relationship,
  RelationshipColumn,
  Schema,
  Table,
} from "./types";

const idGenerator = () => Math.random().toString(36).substring(2, 12);

class DatabaseBuilder {
  private id: string = idGenerator();
  private schemas: SchemaBuilder[] = [];

  withId(id: string) {
    this.id = id;
    return this;
  }

  withSchema(modifier?: (builder: SchemaBuilder) => void) {
    const schemaBuilder = new SchemaBuilder(this.id);
    if (modifier) {
      modifier(schemaBuilder);
    }
    this.schemas.push(schemaBuilder);
    return this;
  }

  build(): Database {
    return {
      id: this.id,
      schemas: this.schemas.map((builder) => builder.build()),
      isAffected: false,
    };
  }
}

class SchemaBuilder {
  private id: string = idGenerator();
  private projectId: string;
  private name: string = "default_schema";
  private tables: TableBuilder[] = [];
  private dbVendorId: Schema["dbVendorId"] = "MYSQL";
  private charset: string = "utf8mb4";
  private collation: string = "utf8mb4_general_ci";

  constructor(projectId: string) {
    this.projectId = projectId;
  }

  withId(id: string) {
    this.id = id;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withTable(modifier?: (builder: TableBuilder) => void) {
    const tableBuilder = new TableBuilder(this.id);
    if (modifier) {
      modifier(tableBuilder);
    }
    this.tables.push(tableBuilder);
    return this;
  }

  build(): Schema {
    return {
      id: this.id,
      projectId: this.projectId,
      name: this.name,
      dbVendorId: this.dbVendorId,
      charset: this.charset,
      collation: this.collation,
      vendorOption: "",
      tables: this.tables.map((builder) => builder.build()),
      isAffected: false,
    };
  }
}

class TableBuilder {
  private id: string = idGenerator();
  private schemaId: string;
  private name: string = "default_table";
  private columns: ColumnBuilder[] = [];
  private constraints: ConstraintBuilder[] = [];
  private indexes: IndexBuilder[] = [];
  private relationships: RelationshipBuilder[] = [];

  constructor(schemaId: string) {
    this.schemaId = schemaId;
  }

  withId(id: string) {
    this.id = id;
    this.columns.forEach((c) => c.withTableId(id));
    this.constraints.forEach((c) => c.withTableId(id));
    this.indexes.forEach((i) => i.withTableId(id));
    this.relationships.forEach((r) => r.withFkTableId(id));
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withColumn(modifier?: (builder: ColumnBuilder) => void) {
    const columnBuilder = new ColumnBuilder()
      .withTableId(this.id)
      .withSeqNo(this.columns.length);
    if (modifier) {
      modifier(columnBuilder);
    }
    this.columns.push(columnBuilder);
    return this;
  }

  withConstraint(modifier?: (builder: ConstraintBuilder) => void) {
    const constraintBuilder = new ConstraintBuilder().withTableId(this.id);
    if (modifier) {
      modifier(constraintBuilder);
    }
    this.constraints.push(constraintBuilder);
    return this;
  }

  withIndex(modifier?: (builder: IndexBuilder) => void) {
    const indexBuilder = new IndexBuilder().withTableId(this.id);
    if (modifier) {
      modifier(indexBuilder);
    }
    this.indexes.push(indexBuilder);
    return this;
  }

  withRelationship(modifier?: (builder: RelationshipBuilder) => void) {
    const relationshipBuilder = new RelationshipBuilder().withFkTableId(
      this.id,
    );
    if (modifier) {
      modifier(relationshipBuilder);
    }
    this.relationships.push(relationshipBuilder);
    return this;
  }

  build(): Table {
    const columns = this.columns.map((builder) => builder.build());
    const columnMap = new Map(columns.map((col) => [col.name, col.id]));

    const constraints = this.constraints.map((builder) => {
      builder.resolveColumnIds(columnMap);
      return builder.build();
    });

    const indexes = this.indexes.map((builder) => {
      builder.resolveColumnIds(columnMap);
      return builder.build();
    });

    const relationships = this.relationships.map((builder) => builder.build());

    return {
      id: this.id,
      schemaId: this.schemaId,
      name: this.name,
      comment: "",
      tableOptions: "",
      columns,
      indexes,
      constraints,
      relationships,
      isAffected: false,
    };
  }
}

class ColumnBuilder {
  private id: string = idGenerator();
  private tableId?: string;
  private name: string = "id";
  private dataType: string = "INT";
  private isAutoIncrement: boolean = false;
  private lengthScale: string = "";
  private charset: string = "utf8mb4";
  private collation: string = "utf8mb4_general_ci";
  private comment: string | null = null;
  private seqNo: number = 0;

  constructor() {}

  withId(id: string) {
    this.id = id;
    return this;
  }

  withTableId(tableId: string) {
    this.tableId = tableId;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withDataType(dataType: string) {
    this.dataType = dataType;
    return this;
  }

  withAutoIncrement(isAutoIncrement: boolean) {
    this.isAutoIncrement = isAutoIncrement;
    return this;
  }

  withLengthScale(lengthScale: string) {
    this.lengthScale = lengthScale;
    return this;
  }

  withSeqNo(seqNo: number) {
    this.seqNo = seqNo;
    return this;
  }

  build(): Column {
    if (!this.tableId) {
      this.tableId = `tbl_${idGenerator()}`;
    }
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      seqNo: this.seqNo,
      dataType: this.dataType,
      lengthScale: this.lengthScale,
      isAutoIncrement: this.isAutoIncrement,
      charset: this.charset,
      collation: this.collation,
      comment: this.comment,
      isAffected: false,
    };
  }
}

type ConstraintKind = Constraint["kind"];

class ConstraintColumnBuilder {
  private id: string = idGenerator();
  private constraintId: string;
  private columnName?: string;
  private columnId: string | null = null;
  private seqNo: number = 0;

  constructor(constraintId: string) {
    this.constraintId = constraintId;
  }

  build(): ConstraintColumn {
    if (!this.columnId) {
      this.columnId = `col_${idGenerator()}`;
    }
    return {
      id: this.id,
      constraintId: this.constraintId,
      columnId: this.columnId,
      seqNo: this.seqNo,
      isAffected: false,
    };
  }

  // 이름으로만 지정된 컬럼을 빌드 시점에 name→id 맵으로 지연 해석하여 columnId를 채웁니다.
  // 이미 columnId가 있거나 columnName이 없으면 아무 동작도 하지 않습니다.
  // columnName이 맵에서 발견되지 않으면 에러를 던집니다.
  resolveColumnId(columnMap: Map<string, string>) {
    if (this.columnId) return;
    if (!this.columnName) return;
    const columnId = columnMap.get(this.columnName);
    if (!columnId) {
      this.columnId = `col_${idGenerator()}`;
      return;
    }
    this.columnId = columnId;
  }

  withColumnName(name: string) {
    this.columnName = name;
    return this;
  }

  withColumnId(id: string) {
    this.columnId = id;
    return this;
  }

  withSeqNo(seqNo: number) {
    this.seqNo = seqNo;
    return this;
  }
}

class ConstraintBuilder {
  private id: string = idGenerator();
  private tableId?: string;
  private name: string = "default_constraint";
  private kind: ConstraintKind = "PRIMARY_KEY";
  private columns: ConstraintColumnBuilder[] = [];
  private defaultExpr?: string;
  private checkExpr?: string;

  constructor() {}

  withTableId(tableId: string) {
    this.tableId = tableId;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withKind(kind: ConstraintKind) {
    this.kind = kind;
    return this;
  }

  withColumn(modifier: (builder: ConstraintColumnBuilder) => void) {
    const columnBuilder = new ConstraintColumnBuilder(this.id);
    modifier(columnBuilder);
    this.columns.push(columnBuilder);
    return this;
  }

  withDefaultExpr(expr: string) {
    this.defaultExpr = expr;
    return this;
  }

  withCheckExpr(expr: string) {
    this.checkExpr = expr;
    return this;
  }

  build(): Constraint {
    if (!this.tableId) {
      this.tableId = `tbl_${idGenerator()}`;
    }
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      kind: this.kind,
      columns: this.columns.map((c) => c.build()),
      defaultExpr: this.defaultExpr,
      checkExpr: this.checkExpr,
      isAffected: false,
    };
  }

  resolveColumnIds(columnMap: Map<string, string>) {
    this.columns.forEach((c) => c.resolveColumnId(columnMap));
  }
}

type IndexType = Index["type"];
type IndexSortDir = IndexColumn["sortDir"];

class IndexColumnBuilder {
  private id: string = idGenerator();
  private indexId: string;
  private columnName?: string;
  private columnId: string | null = null;
  private seqNo: number = 0;
  private sortDir: IndexSortDir = "ASC";

  constructor(indexId: string) {
    this.indexId = indexId;
  }

  withSeqNo(seqNo: number) {
    this.seqNo = seqNo;
    return this;
  }

  withSortDir(sortDir: IndexSortDir) {
    this.sortDir = sortDir;
    return this;
  }

  build(): IndexColumn {
    if (!this.columnId) {
      this.columnId = `col_${idGenerator()}`;
    }
    return {
      id: this.id,
      indexId: this.indexId,
      columnId: this.columnId,
      seqNo: this.seqNo,
      sortDir: this.sortDir,
      isAffected: false,
    };
  }

  // 이름만 지정된 인덱스 컬럼을 빌드 시점에 name -> id 맵으로 지연 해석
  resolveColumnId(columnMap: Map<string, string>) {
    if (this.columnId) return;
    if (!this.columnName) return;
    const columnId = columnMap.get(this.columnName);
    if (!columnId) {
      this.columnId = `col_${idGenerator()}`;
      return;
    }
    this.columnId = columnId;
  }

  withColumnName(name: string) {
    this.columnName = name;
    return this;
  }

  withColumnId(id: string) {
    this.columnId = id;
    return this;
  }
}

class IndexBuilder {
  private id: string = idGenerator();
  private tableId?: string;
  private name: string = "default_index";
  private type: IndexType = "BTREE";
  private columns: IndexColumnBuilder[] = [];

  constructor() {}

  withId(id: string) {
    this.id = id;
    return this;
  }

  withTableId(tableId: string) {
    this.tableId = tableId;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withType(type: IndexType) {
    this.type = type;
    return this;
  }

  withColumn(modifier: (builder: IndexColumnBuilder) => void) {
    const builder = new IndexColumnBuilder(this.id);
    modifier(builder);
    this.columns.push(builder);
    return this;
  }

  build(): Index {
    if (!this.tableId) {
      this.tableId = `tbl_${idGenerator()}`;
    }
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      type: this.type,
      columns: this.columns.map((c) => c.build()),
      comment: null,
      isAffected: false,
    };
  }

  resolveColumnIds(columnMap: Map<string, string>) {
    this.columns.forEach((c) => c.resolveColumnId(columnMap));
  }
}

type RelationshipKind = Relationship["kind"];
type RelationshipCardinality = Relationship["cardinality"];
type RelationshipOnDelete = Relationship["onDelete"];
type RelationshipOnUpdate = Relationship["onUpdate"];

class RelationshipColumnBuilder {
  private id: string = idGenerator();
  private relationshipId: string;
  private fkColumnId: string | null = null;
  private pkColumnId: string | null = null;
  private seqNo: number = 0;

  constructor(relationshipId: string) {
    this.relationshipId = relationshipId;
  }

  withSeqNo(seqNo: number) {
    this.seqNo = seqNo;
    return this;
  }

  withFkColumnId(fkColumnId: string) {
    this.fkColumnId = fkColumnId;
    return this;
  }

  withPkColumnId(pkColumnId: string) {
    this.pkColumnId = pkColumnId;
    return this;
  }

  build(): RelationshipColumn {
    if (!this.fkColumnId) {
      this.fkColumnId = `fk_${idGenerator()}`;
    }
    if (!this.pkColumnId) {
      this.pkColumnId = `pk_${idGenerator()}`;
    }
    return {
      id: this.id,
      relationshipId: this.relationshipId,
      fkColumnId: this.fkColumnId,
      pkColumnId: this.pkColumnId,
      seqNo: this.seqNo,
      isAffected: false,
    };
  }
}

class RelationshipBuilder {
  private id: string = idGenerator();
  private fkTableId?: string;
  private pkTableId?: string;
  private name: string = "default_relationship";
  private kind: RelationshipKind = "NON_IDENTIFYING";
  private cardinality: RelationshipCardinality = "1:N";
  private onDelete: RelationshipOnDelete = "NO_ACTION";
  private onUpdate: RelationshipOnUpdate = "NO_ACTION";
  private fkEnforced: false = false;
  private columns: RelationshipColumnBuilder[] = [];

  withId(id: string) {
    this.id = id;
    return this;
  }

  withFkTableId(tableId: string) {
    this.fkTableId = tableId;
    return this;
  }

  withPkTableId(tableId: string) {
    this.pkTableId = tableId;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withKind(kind: RelationshipKind) {
    this.kind = kind;
    return this;
  }

  withCardinality(cardinality: RelationshipCardinality) {
    this.cardinality = cardinality;
    return this;
  }

  withOnDelete(onDelete: RelationshipOnDelete) {
    this.onDelete = onDelete;
    return this;
  }

  withOnUpdate(onUpdate: RelationshipOnUpdate) {
    this.onUpdate = onUpdate;
    return this;
  }

  withFkEnforced(enforced: false) {
    this.fkEnforced = enforced;
    return this;
  }

  withColumn(modifier: (builder: RelationshipColumnBuilder) => void) {
    const columnBuilder = new RelationshipColumnBuilder(this.id);
    modifier(columnBuilder);
    this.columns.push(columnBuilder);
    return this;
  }

  build(): Relationship {
    if (!this.fkTableId) {
      this.fkTableId = `tbl_${idGenerator()}`;
    }
    if (!this.pkTableId) {
      this.pkTableId = `tbl_${idGenerator()}`;
    }
    return {
      id: this.id,
      fkTableId: this.fkTableId,
      pkTableId: this.pkTableId,
      name: this.name,
      kind: this.kind,
      cardinality: this.cardinality,
      onDelete: this.onDelete,
      onUpdate: this.onUpdate,
      fkEnforced: this.fkEnforced,
      columns: this.columns.map((c) => c.build()),
      isAffected: false,
    };
  }
}

export const createTestDatabase = () => new DatabaseBuilder();

export const createColumnBuilder = () => new ColumnBuilder();

export const createConstraintBuilder = () => new ConstraintBuilder();

export const createIndexBuilder = () => new IndexBuilder();

export const createRelationshipBuilder = () => new RelationshipBuilder();
