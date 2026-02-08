package com.schemafy.domain.erd.schema.fixture;

import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.domain.Schema;

public class SchemaFixture {

  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  public static final String DEFAULT_PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
  public static final String DEFAULT_NAME = "test_schema";
  public static final String DEFAULT_DB_VENDOR = "MySQL";
  public static final String DEFAULT_CHARSET = "utf8mb4";
  public static final String DEFAULT_COLLATION = "utf8mb4_general_ci";

  public static Schema defaultSchema() {
    return new Schema(
        DEFAULT_ID,
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Schema schemaWithId(String id) {
    return new Schema(
        id,
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Schema schemaWithName(String name) {
    return new Schema(
        DEFAULT_ID,
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        name,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static Schema schemaWithDbVendor(String dbVendor) {
    return new Schema(
        DEFAULT_ID,
        DEFAULT_PROJECT_ID,
        dbVendor,
        DEFAULT_NAME,
        null,
        null);
  }

  public static CreateSchemaCommand createCommand() {
    return new CreateSchemaCommand(
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateSchemaCommand createCommandWithName(String name) {
    return new CreateSchemaCommand(
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        name,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateSchemaResult createResult() {
    return new CreateSchemaResult(
        DEFAULT_ID,
        DEFAULT_PROJECT_ID,
        DEFAULT_DB_VENDOR,
        DEFAULT_NAME,
        DEFAULT_CHARSET,
        DEFAULT_COLLATION);
  }

  public static CreateSchemaResult createResultFrom(Schema schema) {
    return new CreateSchemaResult(
        schema.id(),
        schema.projectId(),
        schema.dbVendorName(),
        schema.name(),
        schema.charset(),
        schema.collation());
  }

  public static ChangeSchemaNameCommand changeNameCommand(String newName) {
    return new ChangeSchemaNameCommand(
        DEFAULT_ID,
        newName);
  }

  public static DeleteSchemaCommand deleteCommand() {
    return new DeleteSchemaCommand(DEFAULT_ID);
  }

  public static DeleteSchemaCommand deleteCommand(String schemaId) {
    return new DeleteSchemaCommand(schemaId);
  }

  private SchemaFixture() {}

}
