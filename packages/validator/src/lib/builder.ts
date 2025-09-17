import { Column, Constraint, ConstraintColumn, Database, Index, IndexColumn, Schema, Table } from './types';

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
      projects: this.schemas.map((builder) => builder.build()),
    };
  }
}

class SchemaBuilder {
  private id: string = idGenerator();
  private projectId: string;
  private name: string = 'default_schema';
  private tables: TableBuilder[] = [];
  private dbVendorId: 'mysql' = 'mysql';
  private charset: string = 'utf8mb4';
  private collation: string = 'utf8mb4_general_ci';

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
      vendorOption: '',
      createdAt: new Date(),
      updatedAt: new Date(),
      deletedAt: null,
      tables: this.tables.map((builder) => builder.build()),
    };
  }
}

class TableBuilder {
  private id: string = idGenerator();
  private schemaId: string;
  private name: string = 'default_table';
  private columns: ColumnBuilder[] = [];
  private constraints: ConstraintBuilder[] = [];
  private indexes: IndexBuilder[] = [];

  constructor(schemaId: string) {
    this.schemaId = schemaId;
  }

  withId(id: string) {
    this.id = id;
    return this;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withColumn(modifier?: (builder: ColumnBuilder) => void) {
    const columnBuilder = new ColumnBuilder(this.id);
    if (modifier) {
      modifier(columnBuilder);
    }
    this.columns.push(columnBuilder);
    return this;
  }

  withConstraint(modifier?: (builder: ConstraintBuilder) => void) {
    const constraintBuilder = new ConstraintBuilder(this.id);
    if (modifier) {
      modifier(constraintBuilder);
    }
    this.constraints.push(constraintBuilder);
    return this;
  }

  withIndex(modifier?: (builder: IndexBuilder) => void) {
    const indexBuilder = new IndexBuilder(this.id);
    if (modifier) {
      modifier(indexBuilder);
    }
    this.indexes.push(indexBuilder);
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

    return {
      id: this.id,
      schemaId: this.schemaId,
      name: this.name,
      comment: '',
      tableOptions: '',
      createdAt: new Date(),
      updatedAt: new Date(),
      deletedAt: null,
      columns,
      indexes,
      constraints,
      relationships: [],
    };
  }
}

class ColumnBuilder {
  private id: string = idGenerator();
  private tableId: string;
  private name: string = 'id';
  private dataType: string = 'INT';
  private isAutoIncrement: boolean = false;

  constructor(tableId: string) {
    this.tableId = tableId;
  }

  withId(id: string) {
    this.id = id;
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

  build(): Column {
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      ordinalPosition: 1,
      dataType: this.dataType,
      lengthScale: '',
      isAutoIncrement: this.isAutoIncrement,
      charset: 'utf8mb4',
      collation: 'utf8mb4_general_ci',
      comment: '',
      createdAt: new Date(),
      updatedAt: new Date(),
      deletedAt: null,
    };
  }
}

type ConstraintKind = Constraint['kind'];

class ConstraintColumnBuilder {
  private id: string = idGenerator();
  private constraintId: string;
  private columnName: string;
  private columnId: string | null = null;
  private seqNo: number = 1;

  constructor(constraintId: string, columnName: string) {
    this.constraintId = constraintId;
    this.columnName = columnName;
  }

  build(): ConstraintColumn {
    if (!this.columnId) {
      throw new Error(`Column ID for ${this.columnName} not resolved`);
    }
    return {
      id: this.id,
      constraintId: this.constraintId,
      columnId: this.columnId,
      seqNo: this.seqNo,
    };
  }

  resolveColumnId(columnMap: Map<string, string>) {
    const columnId = columnMap.get(this.columnName);
    if (!columnId) {
      throw new Error(`Column ${this.columnName} not found in table`);
    }
    this.columnId = columnId;
  }
}

class ConstraintBuilder {
  private id: string = idGenerator();
  private tableId: string;
  private name: string = 'default_constraint';
  private kind: ConstraintKind = 'PRIMARY_KEY';
  private columns: ConstraintColumnBuilder[] = [];

  constructor(tableId: string) {
    this.tableId = tableId;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withKind(kind: ConstraintKind) {
    this.kind = kind;
    return this;
  }

  withColumn(columnName: string) {
    this.columns.push(new ConstraintColumnBuilder(this.id, columnName));
    return this;
  }

  build(): Constraint {
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      kind: this.kind,
      columns: this.columns.map((c) => c.build()),
    };
  }

  resolveColumnIds(columnMap: Map<string, string>) {
    this.columns.forEach((c) => c.resolveColumnId(columnMap));
  }
}

type IndexType = Index['type'];
type IndexSortDir = IndexColumn['sortDir'];

class IndexColumnBuilder {
  private id: string = idGenerator();
  private indexId: string;
  private columnName: string;
  private columnId: string | null = null;
  private seqNo: number = 1;
  private sortDir: IndexSortDir = 'ASC';

  constructor(indexId: string, columnName: string) {
    this.indexId = indexId;
    this.columnName = columnName;
  }

  build(): IndexColumn {
    if (!this.columnId) {
      throw new Error(`Column ID for ${this.columnName} not resolved`);
    }
    return {
      id: this.id,
      indexId: this.indexId,
      columnId: this.columnId,
      seqNo: this.seqNo,
      sortDir: this.sortDir,
    };
  }

  resolveColumnId(columnMap: Map<string, string>) {
    const columnId = columnMap.get(this.columnName);
    if (!columnId) {
      throw new Error(`Column ${this.columnName} not found in table`);
    }
    this.columnId = columnId;
  }
}

class IndexBuilder {
  private id: string = idGenerator();
  private tableId: string;
  private name: string = 'default_index';
  private type: IndexType = 'BTREE';
  private columns: IndexColumnBuilder[] = [];

  constructor(tableId: string) {
    this.tableId = tableId;
  }

  withName(name: string) {
    this.name = name;
    return this;
  }

  withType(type: IndexType) {
    this.type = type;
    return this;
  }

  withColumn(columnName: string) {
    this.columns.push(new IndexColumnBuilder(this.id, columnName));
    return this;
  }

  build(): Index {
    return {
      id: this.id,
      tableId: this.tableId,
      name: this.name,
      type: this.type,
      columns: this.columns.map((c) => c.build()),
      comment: null,
    };
  }

  resolveColumnIds(columnMap: Map<string, string>) {
    this.columns.forEach((c) => c.resolveColumnId(columnMap));
  }
}

export const createTestDatabase = () => new DatabaseBuilder();
