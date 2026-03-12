package com.schemafy.core.project.domain;

public enum InvitationStatus {

  PENDING,
  ACCEPTED,
  REJECTED,
  CANCELLED;

  public static InvitationStatus fromString(String value) {
    for (InvitationStatus status : InvitationStatus.values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown invitation status: " + value);
  }

  public boolean isPending() { return this == PENDING; }

  public boolean isAccepted() { return this == ACCEPTED; }

  public boolean isRejected() { return this == REJECTED; }

  public boolean isCancelled() { return this == CANCELLED; }

}
