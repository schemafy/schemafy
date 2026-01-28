package com.schemafy.core.project.repository.vo;

import lombok.Getter;

@Getter
public enum InvitationType {

  WORKSPACE("WORKSPACE"),
  PROJECT("PROJECT");

  private final String value;

  InvitationType(String value) {
    this.value = value;
  }

  public static InvitationType fromValue(String value) {
    for (InvitationType type : InvitationType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown invitation type: " + value);
  }

  public boolean isWorkspace() { return this == WORKSPACE; }

  public boolean isProject() { return this == PROJECT; }

}
