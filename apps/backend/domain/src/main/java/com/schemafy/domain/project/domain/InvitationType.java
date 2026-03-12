package com.schemafy.domain.project.domain;

public enum InvitationType {

  WORKSPACE,
  PROJECT;

  public static InvitationType fromString(String value) {
    for (InvitationType type : InvitationType.values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown invitation type: " + value);
  }

  public boolean isWorkspace() { return this == WORKSPACE; }

  public boolean isProject() { return this == PROJECT; }

}
