package com.schemafy.domain.erd.table.fixture;

import com.schemafy.domain.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.GetTableQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.domain.erd.table.domain.Table;

public class TableFixture {

  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5TAB";
  public static final String DEFAULT_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  public static final String DEFAULT_NAME = "test_table";
  public static final String DEFAULT_CHARSET = "utf8mb4";
  public static final String DEFAULT_COLLATION = "utf8mb4_general_ci";

  public static Table defaultTable() {
    return new Table(
        DEFAULT_ID,
        DEFAULT_SCHEMA_ID,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Table tableWithId(String id) {
    return new Table(
        id,
        DEFAULT_SCHEMA_ID,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Table tableWithName(String name) {
    return new Table(
        DEFAULT_ID,
        DEFAULT_SCHEMA_ID,
        name,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Table tableWithSchemaId(String schemaId) {
    return new Table(
        DEFAULT_ID,
        schemaId,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateTableCommand createCommand() {
    return new CreateTableCommand(
        DEFAULT_SCHEMA_ID,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateTableCommand createCommandWithName(String name) {
    return new CreateTableCommand(
        DEFAULT_SCHEMA_ID,
        name,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateTableResult createResult() {
    return new CreateTableResult(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateTableResult createResultFrom(Table table) {
    return new CreateTableResult(
        table.id(),
        table.name(),
        table.charset(),
        table.collation());
  }

  public static ChangeTableNameCommand changeNameCommand(String newName) {
    return new ChangeTableNameCommand(
        DEFAULT_SCHEMA_ID,
        DEFAULT_ID,
        newName);
  }

  public static ChangeTableMetaCommand changeMetaCommand(String charset, String collation) {
    return new ChangeTableMetaCommand(
        DEFAULT_ID,
        charset,
        collation);
  }

  public static ChangeTableExtraCommand changeExtraCommand(String extra) {
    return new ChangeTableExtraCommand(
        DEFAULT_ID,
        extra);
  }

  public static DeleteTableCommand deleteCommand() {
    return new DeleteTableCommand(DEFAULT_ID);
  }

  public static DeleteTableCommand deleteCommand(String tableId) {
    return new DeleteTableCommand(tableId);
  }

  public static GetTableQuery getTableQuery() { return new GetTableQuery(DEFAULT_ID); }

  public static GetTableQuery getTableQuery(String tableId) {
    return new GetTableQuery(tableId);
  }

  public static GetTablesBySchemaIdQuery getTablesBySchemaIdQuery() {
    return new GetTablesBySchemaIdQuery(DEFAULT_SCHEMA_ID);
  }

  public static GetTablesBySchemaIdQuery getTablesBySchemaIdQuery(String schemaId) {
    return new GetTablesBySchemaIdQuery(schemaId);
  }

  private TableFixture() {}

}
