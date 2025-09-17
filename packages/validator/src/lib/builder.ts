import { Column, Database, Schema, Table } from './types';

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

  build(): Table {
    return {
      id: this.id,
      schemaId: this.schemaId,
      name: this.name,
      comment: '',
      tableOptions: '',
      createdAt: new Date(),
      updatedAt: new Date(),
      deletedAt: null,
      columns: this.columns.map((builder) => builder.build()),
      indexes: [],
      constraints: [],
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

export const createTestDatabase = () => new DatabaseBuilder();
