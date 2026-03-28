package com.schemafy.core.erd.operation.domain;

public enum ErdOperationType {

  CREATE_SCHEMA(ErdTouchedEntityType.SCHEMA, true),
  CHANGE_SCHEMA_NAME(ErdTouchedEntityType.SCHEMA, false),
  DELETE_SCHEMA(ErdTouchedEntityType.SCHEMA, false),
  CREATE_TABLE(ErdTouchedEntityType.TABLE, true),
  CHANGE_TABLE_NAME(ErdTouchedEntityType.TABLE, false),
  CHANGE_TABLE_META(ErdTouchedEntityType.TABLE, false),
  CHANGE_TABLE_EXTRA(ErdTouchedEntityType.TABLE, false),
  DELETE_TABLE(ErdTouchedEntityType.TABLE, false),
  CREATE_COLUMN(ErdTouchedEntityType.COLUMN, true),
  CHANGE_COLUMN_NAME(ErdTouchedEntityType.COLUMN, false),
  CHANGE_COLUMN_TYPE(ErdTouchedEntityType.COLUMN, false),
  CHANGE_COLUMN_META(ErdTouchedEntityType.COLUMN, false),
  CHANGE_COLUMN_POSITION(ErdTouchedEntityType.COLUMN, false),
  DELETE_COLUMN(ErdTouchedEntityType.COLUMN, false),
  CREATE_CONSTRAINT(ErdTouchedEntityType.CONSTRAINT, true),
  CHANGE_CONSTRAINT_NAME(ErdTouchedEntityType.CONSTRAINT, false),
  CHANGE_CONSTRAINT_CHECK_EXPR(ErdTouchedEntityType.CONSTRAINT, false),
  CHANGE_CONSTRAINT_DEFAULT_EXPR(ErdTouchedEntityType.CONSTRAINT, false),
  DELETE_CONSTRAINT(ErdTouchedEntityType.CONSTRAINT, false),
  ADD_CONSTRAINT_COLUMN(ErdTouchedEntityType.CONSTRAINT_COLUMN, true),
  REMOVE_CONSTRAINT_COLUMN(ErdTouchedEntityType.CONSTRAINT_COLUMN, false),
  CHANGE_CONSTRAINT_COLUMN_POSITION(ErdTouchedEntityType.CONSTRAINT_COLUMN, false),
  CREATE_INDEX(ErdTouchedEntityType.INDEX, true),
  CHANGE_INDEX_NAME(ErdTouchedEntityType.INDEX, false),
  CHANGE_INDEX_TYPE(ErdTouchedEntityType.INDEX, false),
  DELETE_INDEX(ErdTouchedEntityType.INDEX, false),
  ADD_INDEX_COLUMN(ErdTouchedEntityType.INDEX_COLUMN, true),
  REMOVE_INDEX_COLUMN(ErdTouchedEntityType.INDEX_COLUMN, false),
  CHANGE_INDEX_COLUMN_POSITION(ErdTouchedEntityType.INDEX_COLUMN, false),
  CHANGE_INDEX_COLUMN_SORT_DIRECTION(ErdTouchedEntityType.INDEX_COLUMN, false),
  CREATE_RELATIONSHIP(ErdTouchedEntityType.RELATIONSHIP, true),
  CHANGE_RELATIONSHIP_NAME(ErdTouchedEntityType.RELATIONSHIP, false),
  CHANGE_RELATIONSHIP_KIND(ErdTouchedEntityType.RELATIONSHIP, false),
  CHANGE_RELATIONSHIP_CARDINALITY(ErdTouchedEntityType.RELATIONSHIP, false),
  CHANGE_RELATIONSHIP_EXTRA(ErdTouchedEntityType.RELATIONSHIP, false),
  DELETE_RELATIONSHIP(ErdTouchedEntityType.RELATIONSHIP, false),
  ADD_RELATIONSHIP_COLUMN(ErdTouchedEntityType.RELATIONSHIP_COLUMN, true),
  REMOVE_RELATIONSHIP_COLUMN(ErdTouchedEntityType.RELATIONSHIP_COLUMN, false),
  CHANGE_RELATIONSHIP_COLUMN_POSITION(ErdTouchedEntityType.RELATIONSHIP_COLUMN, false);

  private final ErdTouchedEntityType touchedEntityType;
  private final boolean createsEntity;

  ErdOperationType(ErdTouchedEntityType touchedEntityType, boolean createsEntity) {
    this.touchedEntityType = touchedEntityType;
    this.createsEntity = createsEntity;
  }

  public ErdTouchedEntityType touchedEntityType() {
    return touchedEntityType;
  }

  public boolean createsEntity() {
    return createsEntity;
  }

}
