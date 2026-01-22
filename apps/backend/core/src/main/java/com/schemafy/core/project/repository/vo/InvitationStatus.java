package com.schemafy.core.project.repository.vo;

import lombok.Getter;

@Getter
public enum InvitationStatus {

  PENDING("pending"),
  ACCEPTED("accepted"),
  REJECTED("rejected");

  private final String value;

  InvitationStatus(String value) {
    this.value = value;
  }

  public static InvitationStatus fromValue(String value) {
    for (InvitationStatus status : InvitationStatus.values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown invitation status: " + value);
  }

  public boolean isPending() { return this == PENDING; }

  public boolean isAccepted() { return this == ACCEPTED; }

  public boolean isRejected() { return this == REJECTED; }

}
