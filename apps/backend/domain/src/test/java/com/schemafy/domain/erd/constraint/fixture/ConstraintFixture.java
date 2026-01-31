package com.schemafy.domain.erd.constraint.fixture;

import java.util.List;

import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

public class ConstraintFixture {

  // Default Constraint constants
  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5CNT";
  public static final String DEFAULT_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TBL";
  public static final String DEFAULT_NAME = "pk_test_constraint";
  public static final ConstraintKind DEFAULT_KIND = ConstraintKind.PRIMARY_KEY;
  public static final String DEFAULT_CHECK_EXPR = null;
  public static final String DEFAULT_DEFAULT_EXPR = null;

  // Default ConstraintColumn constants
  public static final String DEFAULT_CONSTRAINT_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5CCL";
  public static final String DEFAULT_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5COL";
  public static final int DEFAULT_SEQ_NO = 0;

  // Other constants
  public static final String DEFAULT_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  // ========== Constraint Domain Object Factory Methods ==========

  public static Constraint defaultConstraint() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint constraintWithId(String id) {
    return new Constraint(
        id,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint constraintWithName(String name) {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint constraintWithIdAndName(String id, String name) {
    return new Constraint(
        id,
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint constraintWithTableId(String tableId) {
    return new Constraint(
        DEFAULT_ID,
        tableId,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint constraintWithKind(ConstraintKind kind) {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        kind,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static Constraint primaryKeyConstraint() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "pk_" + DEFAULT_NAME,
        ConstraintKind.PRIMARY_KEY,
        null,
        null);
  }

  public static Constraint primaryKeyConstraintWithId(String id) {
    return new Constraint(
        id,
        DEFAULT_TABLE_ID,
        "pk_" + DEFAULT_NAME,
        ConstraintKind.PRIMARY_KEY,
        null,
        null);
  }

  public static Constraint primaryKeyConstraintWithTableId(String tableId) {
    return new Constraint(
        DEFAULT_ID,
        tableId,
        "pk_" + DEFAULT_NAME,
        ConstraintKind.PRIMARY_KEY,
        null,
        null);
  }

  public static Constraint uniqueConstraint() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "uq_" + DEFAULT_NAME,
        ConstraintKind.UNIQUE,
        null,
        null);
  }

  public static Constraint uniqueConstraintWithId(String id) {
    return new Constraint(
        id,
        DEFAULT_TABLE_ID,
        "uq_" + DEFAULT_NAME,
        ConstraintKind.UNIQUE,
        null,
        null);
  }

  public static Constraint checkConstraint() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "ck_" + DEFAULT_NAME,
        ConstraintKind.CHECK,
        "column1 > 0",
        null);
  }

  public static Constraint checkConstraintWithExpr(String checkExpr) {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "ck_" + DEFAULT_NAME,
        ConstraintKind.CHECK,
        checkExpr,
        null);
  }

  public static Constraint defaultConstraintValue() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "df_" + DEFAULT_NAME,
        ConstraintKind.DEFAULT,
        null,
        "0");
  }

  public static Constraint defaultConstraintWithExpr(String defaultExpr) {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "df_" + DEFAULT_NAME,
        ConstraintKind.DEFAULT,
        null,
        defaultExpr);
  }

  public static Constraint notNullConstraint() {
    return new Constraint(
        DEFAULT_ID,
        DEFAULT_TABLE_ID,
        "nn_" + DEFAULT_NAME,
        ConstraintKind.NOT_NULL,
        null,
        null);
  }

  public static Constraint constraint(
      String id, String tableId, String name, ConstraintKind kind,
      String checkExpr, String defaultExpr) {
    return new Constraint(id, tableId, name, kind, checkExpr, defaultExpr);
  }

  // ========== ConstraintColumn Domain Object Factory Methods ==========

  public static ConstraintColumn defaultConstraintColumn() {
    return new ConstraintColumn(
        DEFAULT_CONSTRAINT_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static ConstraintColumn constraintColumnWithId(String id) {
    return new ConstraintColumn(
        id,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static ConstraintColumn constraintColumnWithSeqNo(int seqNo) {
    return new ConstraintColumn(
        DEFAULT_CONSTRAINT_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        seqNo);
  }

  public static ConstraintColumn constraintColumnWithConstraintId(String constraintId) {
    return new ConstraintColumn(
        DEFAULT_CONSTRAINT_COLUMN_ID,
        constraintId,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static ConstraintColumn constraintColumnWithColumnId(String columnId) {
    return new ConstraintColumn(
        DEFAULT_CONSTRAINT_COLUMN_ID,
        DEFAULT_ID,
        columnId,
        DEFAULT_SEQ_NO);
  }

  public static ConstraintColumn constraintColumn(
      String id, String constraintId, String columnId, int seqNo) {
    return new ConstraintColumn(id, constraintId, columnId, seqNo);
  }

  // ========== CreateConstraintCommand Factory Methods ==========

  public static CreateConstraintCommand createCommand() {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createCommandWithName(String name) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        name,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createCommandWithTableId(String tableId) {
    return new CreateConstraintCommand(
        tableId,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createCommandWithKind(ConstraintKind kind) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        kind,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createPrimaryKeyCommand() {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "pk_test",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createPrimaryKeyCommandWithColumns(
      List<CreateConstraintColumnCommand> columns) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "pk_test",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        columns);
  }

  public static CreateConstraintCommand createUniqueCommand() {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "uq_test",
        ConstraintKind.UNIQUE,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createUniqueCommandWithColumns(
      List<CreateConstraintColumnCommand> columns) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "uq_test",
        ConstraintKind.UNIQUE,
        null,
        null,
        columns);
  }

  public static CreateConstraintCommand createCheckCommand(String checkExpr) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "ck_test",
        ConstraintKind.CHECK,
        checkExpr,
        null,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createDefaultCommand(String defaultExpr) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "df_test",
        ConstraintKind.DEFAULT,
        null,
        defaultExpr,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createNotNullCommand() {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        "nn_test",
        ConstraintKind.NOT_NULL,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO)));
  }

  public static CreateConstraintCommand createCommandWithColumns(
      List<CreateConstraintColumnCommand> columns) {
    return new CreateConstraintCommand(
        DEFAULT_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR,
        columns);
  }

  // ========== CreateConstraintColumnCommand Factory Methods ==========

  public static CreateConstraintColumnCommand createColumnCommand() {
    return new CreateConstraintColumnCommand(DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO);
  }

  public static CreateConstraintColumnCommand createColumnCommand(String columnId, int seqNo) {
    return new CreateConstraintColumnCommand(columnId, seqNo);
  }

  // ========== Other Command Factory Methods ==========

  public static ChangeConstraintNameCommand changeNameCommand(String newName) {
    return new ChangeConstraintNameCommand(DEFAULT_ID, newName);
  }

  public static ChangeConstraintNameCommand changeNameCommand(
      String constraintId, String newName) {
    return new ChangeConstraintNameCommand(constraintId, newName);
  }

  public static AddConstraintColumnCommand addColumnCommand() {
    return new AddConstraintColumnCommand(DEFAULT_ID, DEFAULT_COLUMN_ID, DEFAULT_SEQ_NO);
  }

  public static AddConstraintColumnCommand addColumnCommand(
      String constraintId, String columnId, int seqNo) {
    return new AddConstraintColumnCommand(constraintId, columnId, seqNo);
  }

  public static RemoveConstraintColumnCommand removeColumnCommand() {
    return new RemoveConstraintColumnCommand(DEFAULT_ID, DEFAULT_CONSTRAINT_COLUMN_ID);
  }

  public static RemoveConstraintColumnCommand removeColumnCommand(
      String constraintId, String constraintColumnId) {
    return new RemoveConstraintColumnCommand(constraintId, constraintColumnId);
  }

  public static ChangeConstraintColumnPositionCommand changeColumnPositionCommand(int seqNo) {
    return new ChangeConstraintColumnPositionCommand(DEFAULT_CONSTRAINT_COLUMN_ID, seqNo);
  }

  public static ChangeConstraintColumnPositionCommand changeColumnPositionCommand(
      String constraintColumnId, int seqNo) {
    return new ChangeConstraintColumnPositionCommand(constraintColumnId, seqNo);
  }

  public static DeleteConstraintCommand deleteCommand() {
    return new DeleteConstraintCommand(DEFAULT_ID);
  }

  public static DeleteConstraintCommand deleteCommand(String constraintId) {
    return new DeleteConstraintCommand(constraintId);
  }

  // ========== Query Factory Methods ==========

  public static GetConstraintQuery getConstraintQuery() { return new GetConstraintQuery(DEFAULT_ID); }

  public static GetConstraintQuery getConstraintQuery(String constraintId) {
    return new GetConstraintQuery(constraintId);
  }

  public static GetConstraintsByTableIdQuery getConstraintsByTableIdQuery() {
    return new GetConstraintsByTableIdQuery(DEFAULT_TABLE_ID);
  }

  public static GetConstraintsByTableIdQuery getConstraintsByTableIdQuery(String tableId) {
    return new GetConstraintsByTableIdQuery(tableId);
  }

  public static GetConstraintColumnQuery getConstraintColumnQuery() {
    return new GetConstraintColumnQuery(DEFAULT_CONSTRAINT_COLUMN_ID);
  }

  public static GetConstraintColumnQuery getConstraintColumnQuery(String constraintColumnId) {
    return new GetConstraintColumnQuery(constraintColumnId);
  }

  public static GetConstraintColumnsByConstraintIdQuery getConstraintColumnsByConstraintIdQuery() {
    return new GetConstraintColumnsByConstraintIdQuery(DEFAULT_ID);
  }

  public static GetConstraintColumnsByConstraintIdQuery getConstraintColumnsByConstraintIdQuery(
      String constraintId) {
    return new GetConstraintColumnsByConstraintIdQuery(constraintId);
  }

  // ========== Result Factory Methods ==========

  public static CreateConstraintResult createResult() {
    return new CreateConstraintResult(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CHECK_EXPR,
        DEFAULT_DEFAULT_EXPR);
  }

  public static CreateConstraintResult createResultFrom(Constraint constraint) {
    return new CreateConstraintResult(
        constraint.id(),
        constraint.name(),
        constraint.kind(),
        constraint.checkExpr(),
        constraint.defaultExpr());
  }

  public static AddConstraintColumnResult addColumnResult() {
    return new AddConstraintColumnResult(
        DEFAULT_CONSTRAINT_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_COLUMN_ID,
        DEFAULT_SEQ_NO,
        List.of());
  }

  public static AddConstraintColumnResult addColumnResultFrom(ConstraintColumn constraintColumn) {
    return new AddConstraintColumnResult(
        constraintColumn.id(),
        constraintColumn.constraintId(),
        constraintColumn.columnId(),
        constraintColumn.seqNo(),
        List.of());
  }

  private ConstraintFixture() {}

}
