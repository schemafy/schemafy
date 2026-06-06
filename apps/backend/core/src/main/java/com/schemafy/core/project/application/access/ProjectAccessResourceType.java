package com.schemafy.core.project.application.access;

public enum ProjectAccessResourceType {

  NONE("none"),
  PROJECT("project"),
  SCHEMA("schema"),
  TABLE("table"),
  COLUMN("column"),
  CONSTRAINT("constraint"),
  CONSTRAINT_COLUMN("constraintColumn"),
  INDEX("index"),
  INDEX_COLUMN("indexColumn"),
  RELATIONSHIP("relationship"),
  RELATIONSHIP_COLUMN("relationshipColumn"),
  MEMO("memo"),
  MEMO_COMMENT("memoComment"),
  OPERATION("operation");

  private final String token;

  ProjectAccessResourceType(String token) {
    this.token = token;
  }

  static ProjectAccessResourceType fromToken(String token) {
    for (ProjectAccessResourceType type : values()) {
      if (type.token.equals(token)) {
        return type;
      }
    }
    throw new IllegalStateException("Unknown project access target type: " + token);
  }

}
