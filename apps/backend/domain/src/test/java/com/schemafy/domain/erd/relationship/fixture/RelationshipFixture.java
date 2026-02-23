package com.schemafy.domain.erd.relationship.fixture;

import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsBySchemaIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public class RelationshipFixture {

  public static final String DEFAULT_ID = "01ARZ3NDEKTSV4RRFFQ69G5REL";
  public static final String DEFAULT_PK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5PKT";
  public static final String DEFAULT_FK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5FKT";
  public static final String DEFAULT_NAME = "fk_test_relationship";
  public static final RelationshipKind DEFAULT_KIND = RelationshipKind.NON_IDENTIFYING;
  public static final Cardinality DEFAULT_CARDINALITY = Cardinality.ONE_TO_MANY;
  public static final String DEFAULT_EXTRA = null;

  public static final String DEFAULT_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5RCL";
  public static final String DEFAULT_PK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5PKC";
  public static final String DEFAULT_FK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5FKC";
  public static final int DEFAULT_SEQ_NO = 0;

  public static final String DEFAULT_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  public static Relationship defaultRelationship() {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithId(String id) {
    return new Relationship(
        id,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithName(String name) {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        name,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithIdAndName(String id, String name) {
    return new Relationship(
        id,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        name,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithTables(String pkTableId, String fkTableId) {
    return new Relationship(
        DEFAULT_ID,
        pkTableId,
        fkTableId,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithKind(RelationshipKind kind) {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        kind,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship identifyingRelationship() {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        RelationshipKind.IDENTIFYING,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship identifyingRelationshipWithTables(
      String id, String pkTableId, String fkTableId, String name) {
    return new Relationship(
        id,
        pkTableId,
        fkTableId,
        name,
        RelationshipKind.IDENTIFYING,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship nonIdentifyingRelationship() {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        RelationshipKind.NON_IDENTIFYING,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship nonIdentifyingRelationshipWithTables(
      String id, String pkTableId, String fkTableId, String name) {
    return new Relationship(
        id,
        pkTableId,
        fkTableId,
        name,
        RelationshipKind.NON_IDENTIFYING,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithCardinality(Cardinality cardinality) {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        cardinality,
        DEFAULT_EXTRA);
  }

  public static Relationship relationshipWithExtra(String extra) {
    return new Relationship(
        DEFAULT_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        extra);
  }

  public static RelationshipColumn defaultRelationshipColumn() {
    return new RelationshipColumn(
        DEFAULT_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_PK_COLUMN_ID,
        DEFAULT_FK_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static RelationshipColumn relationshipColumnWithId(String id) {
    return new RelationshipColumn(
        id,
        DEFAULT_ID,
        DEFAULT_PK_COLUMN_ID,
        DEFAULT_FK_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static RelationshipColumn relationshipColumnWithSeqNo(int seqNo) {
    return new RelationshipColumn(
        DEFAULT_COLUMN_ID,
        DEFAULT_ID,
        DEFAULT_PK_COLUMN_ID,
        DEFAULT_FK_COLUMN_ID,
        seqNo);
  }

  public static RelationshipColumn relationshipColumnWithRelationshipId(String relationshipId) {
    return new RelationshipColumn(
        DEFAULT_COLUMN_ID,
        relationshipId,
        DEFAULT_PK_COLUMN_ID,
        DEFAULT_FK_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static RelationshipColumn relationshipColumn(
      String id, String relationshipId, String pkColumnId, String fkColumnId, int seqNo) {
    return new RelationshipColumn(id, relationshipId, pkColumnId, fkColumnId, seqNo);
  }

  public static CreateRelationshipCommand createCommand() {
    return new CreateRelationshipCommand(
        DEFAULT_FK_TABLE_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY, null);
  }

  public static CreateRelationshipCommand createCommandWithTables(
      String fkTableId, String pkTableId) {
    return new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY, null);
  }

  public static CreateRelationshipCommand createIdentifyingCommand() {
    return new CreateRelationshipCommand(
        DEFAULT_FK_TABLE_ID,
        DEFAULT_PK_TABLE_ID,
        RelationshipKind.IDENTIFYING,
        DEFAULT_CARDINALITY, null);
  }

  public static CreateRelationshipCommand createIdentifyingCommandWithTables(
      String fkTableId, String pkTableId) {
    return new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.IDENTIFYING,
        DEFAULT_CARDINALITY, null);
  }

  public static CreateRelationshipCommand createCommandWithKind(RelationshipKind kind) {
    return new CreateRelationshipCommand(
        DEFAULT_FK_TABLE_ID,
        DEFAULT_PK_TABLE_ID,
        kind,
        DEFAULT_CARDINALITY, null);
  }

  public static ChangeRelationshipNameCommand changeNameCommand(String newName) {
    return new ChangeRelationshipNameCommand(DEFAULT_ID, newName);
  }

  public static ChangeRelationshipNameCommand changeNameCommand(
      String relationshipId, String newName) {
    return new ChangeRelationshipNameCommand(relationshipId, newName);
  }

  public static ChangeRelationshipKindCommand changeKindCommand(RelationshipKind kind) {
    return new ChangeRelationshipKindCommand(DEFAULT_ID, kind);
  }

  public static ChangeRelationshipKindCommand changeKindCommand(
      String relationshipId, RelationshipKind kind) {
    return new ChangeRelationshipKindCommand(relationshipId, kind);
  }

  public static ChangeRelationshipCardinalityCommand changeCardinalityCommand(
      Cardinality cardinality) {
    return new ChangeRelationshipCardinalityCommand(DEFAULT_ID, cardinality);
  }

  public static ChangeRelationshipExtraCommand changeExtraCommand(String extra) {
    return new ChangeRelationshipExtraCommand(DEFAULT_ID, extra);
  }

  public static AddRelationshipColumnCommand addColumnCommand() {
    return new AddRelationshipColumnCommand(
        DEFAULT_ID,
        DEFAULT_PK_COLUMN_ID,
        DEFAULT_FK_COLUMN_ID,
        DEFAULT_SEQ_NO);
  }

  public static AddRelationshipColumnCommand addColumnCommand(
      String relationshipId, String pkColumnId, String fkColumnId, int seqNo) {
    return new AddRelationshipColumnCommand(relationshipId, pkColumnId, fkColumnId, seqNo);
  }

  public static RemoveRelationshipColumnCommand removeColumnCommand() {
    return new RemoveRelationshipColumnCommand(DEFAULT_COLUMN_ID);
  }

  public static RemoveRelationshipColumnCommand removeColumnCommand(
      String relationshipColumnId) {
    return new RemoveRelationshipColumnCommand(relationshipColumnId);
  }

  public static DeleteRelationshipCommand deleteCommand() {
    return new DeleteRelationshipCommand(DEFAULT_ID);
  }

  public static DeleteRelationshipCommand deleteCommand(String relationshipId) {
    return new DeleteRelationshipCommand(relationshipId);
  }

  public static GetRelationshipQuery getRelationshipQuery() { return new GetRelationshipQuery(DEFAULT_ID); }

  public static GetRelationshipQuery getRelationshipQuery(String relationshipId) {
    return new GetRelationshipQuery(relationshipId);
  }

  public static GetRelationshipsByTableIdQuery getRelationshipsByTableIdQuery() {
    return new GetRelationshipsByTableIdQuery(DEFAULT_FK_TABLE_ID);
  }

  public static GetRelationshipsByTableIdQuery getRelationshipsByTableIdQuery(String tableId) {
    return new GetRelationshipsByTableIdQuery(tableId);
  }

  public static GetRelationshipsBySchemaIdQuery getRelationshipsBySchemaIdQuery() {
    return new GetRelationshipsBySchemaIdQuery(DEFAULT_SCHEMA_ID);
  }

  public static GetRelationshipsBySchemaIdQuery getRelationshipsBySchemaIdQuery(String schemaId) {
    return new GetRelationshipsBySchemaIdQuery(schemaId);
  }

  public static GetRelationshipColumnQuery getRelationshipColumnQuery() {
    return new GetRelationshipColumnQuery(DEFAULT_COLUMN_ID);
  }

  public static GetRelationshipColumnQuery getRelationshipColumnQuery(
      String relationshipColumnId) {
    return new GetRelationshipColumnQuery(relationshipColumnId);
  }

  public static GetRelationshipColumnsByRelationshipIdQuery getRelationshipColumnsByRelationshipIdQuery() {
    return new GetRelationshipColumnsByRelationshipIdQuery(DEFAULT_ID);
  }

  public static GetRelationshipColumnsByRelationshipIdQuery getRelationshipColumnsByRelationshipIdQuery(
      String relationshipId) {
    return new GetRelationshipColumnsByRelationshipIdQuery(relationshipId);
  }

  public static CreateRelationshipResult createResult() {
    return new CreateRelationshipResult(
        DEFAULT_ID,
        DEFAULT_FK_TABLE_ID,
        DEFAULT_PK_TABLE_ID,
        DEFAULT_NAME,
        DEFAULT_KIND,
        DEFAULT_CARDINALITY,
        DEFAULT_EXTRA);
  }

  public static CreateRelationshipResult createResultFrom(Relationship relationship) {
    return new CreateRelationshipResult(
        relationship.id(),
        relationship.fkTableId(),
        relationship.pkTableId(),
        relationship.name(),
        relationship.kind(),
        relationship.cardinality(),
        relationship.extra());
  }

  private RelationshipFixture() {}

}
