package com.schemafy.domain.erd.column.fixture;

import com.schemafy.domain.common.PatchField;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;

public class ColumnFixture {

  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5COL";
  public static final String DEFAULT_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TAB";
  public static final String DEFAULT_NAME = "test_column";
  public static final String DEFAULT_DATA_TYPE = "VARCHAR";
  public static final int DEFAULT_LENGTH = 255;
  public static final int DEFAULT_SEQ_NO = 0;

  public static Column defaultColumn() {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column columnWithId(String id) {
    return new Column(
        id,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column columnWithName(String name) {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column columnWithIdAndName(String id, String name) {
    return new Column(
        id,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column columnWithDataType(String dataType, ColumnLengthScale lengthScale) {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        dataType,
        lengthScale,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column intColumn() {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        "INT",
        null,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column intColumnWithAutoIncrement() {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        "INT",
        null,
        DEFAULT_SEQ_NO,
        true,
        null,
        null,
        null);
  }

  public static Column intColumnWithAutoIncrementAndName(String id, String name) {
    return new Column(
        id,
        DEFAULT_TABLE_ID,
        name,
        "INT",
        null,
        DEFAULT_SEQ_NO,
        true,
        null,
        null,
        null);
  }

  public static Column decimalColumn() {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        "DECIMAL",
        new ColumnLengthScale(null, 10, 2),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static Column varcharColumnWithCharset(String charset, String collation) {
    return new Column(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        charset,
        collation,
        null);
  }

  public static Column varcharColumnWithIdAndCharset(String id, String charset, String collation) {
    return new Column(
        id,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        charset,
        collation,
        null);
  }

  public static CreateColumnCommand createCommand() {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        DEFAULT_LENGTH,
        null,
        null,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createIntCommand() {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        "INT",
        null,
        null,
        null,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createDecimalCommand() {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        "DECIMAL",
        null,
        10,
        2,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createCommandWithName(String name) {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_DATA_TYPE,
        DEFAULT_LENGTH,
        null,
        null,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createCommandWithDataType(
      String dataType, Integer length, Integer precision, Integer scale) {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        dataType,
        length,
        precision,
        scale,
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createCommandWithAutoIncrement(String dataType) {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        dataType,
        null,
        null,
        null,
        DEFAULT_SEQ_NO,
        true,
        null,
        null,
        null);
  }

  public static CreateColumnCommand createCommandWithCharset(String charset, String collation) {
    return new CreateColumnCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        DEFAULT_LENGTH,
        null,
        null,
        DEFAULT_SEQ_NO,
        false,
        charset,
        collation,
        null);
  }

  public static ChangeColumnNameCommand changeNameCommand(String newName) {
    return new ChangeColumnNameCommand(DEFAULT_ID, newName);
  }

  public static ChangeColumnTypeCommand changeTypeCommand(
      String dataType, Integer length, Integer precision, Integer scale) {
    return new ChangeColumnTypeCommand(DEFAULT_ID, dataType, length, precision, scale);
  }

  public static ChangeColumnMetaCommand changeMetaCommand(
      PatchField<Boolean> autoIncrement,
      PatchField<String> charset,
      PatchField<String> collation,
      PatchField<String> comment) {
    return new ChangeColumnMetaCommand(DEFAULT_ID, autoIncrement, charset, collation, comment);
  }

  public static ChangeColumnPositionCommand changePositionCommand(int seqNo) {
    return new ChangeColumnPositionCommand(DEFAULT_ID, seqNo);
  }

  public static DeleteColumnCommand deleteCommand() {
    return new DeleteColumnCommand(DEFAULT_ID);
  }

  public static DeleteColumnCommand deleteCommand(String columnId) {
    return new DeleteColumnCommand(columnId);
  }

  public static GetColumnQuery getColumnQuery() { return new GetColumnQuery(DEFAULT_ID); }

  public static GetColumnQuery getColumnQuery(String columnId) {
    return new GetColumnQuery(columnId);
  }

  public static GetColumnsByTableIdQuery getColumnsByTableIdQuery() {
    return new GetColumnsByTableIdQuery(DEFAULT_TABLE_ID);
  }

  public static GetColumnsByTableIdQuery getColumnsByTableIdQuery(String tableId) {
    return new GetColumnsByTableIdQuery(tableId);
  }

  public static CreateColumnResult createResult() {
    return new CreateColumnResult(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_DATA_TYPE,
        new ColumnLengthScale(DEFAULT_LENGTH, null, null),
        DEFAULT_SEQ_NO,
        false,
        null,
        null,
        null);
  }

  public static CreateColumnResult createResultFrom(Column column) {
    return new CreateColumnResult(
        column.id(),
        column.name(),
        column.dataType(),
        column.lengthScale(),
        column.seqNo(),
        column.autoIncrement(),
        column.charset(),
        column.collation(),
        column.comment());
  }

  private ColumnFixture() {}

}
