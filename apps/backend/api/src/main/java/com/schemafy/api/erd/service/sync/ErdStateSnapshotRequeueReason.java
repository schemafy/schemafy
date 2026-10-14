package com.schemafy.api.erd.service.sync;

public enum ErdStateSnapshotRequeueReason {

  SUPERSEDED(false),
  FAILURE(true);

  private final boolean incrementFailureCount;

  ErdStateSnapshotRequeueReason(boolean incrementFailureCount) {
    this.incrementFailureCount = incrementFailureCount;
  }

  public boolean incrementFailureCount() {
    return incrementFailureCount;
  }

}
