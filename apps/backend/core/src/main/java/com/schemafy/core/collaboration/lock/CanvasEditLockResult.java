package com.schemafy.core.collaboration.lock;

public record CanvasEditLockResult(
    CanvasEditLockState state,
    String ownerSessionId) {

  public boolean isOwner() { return state == CanvasEditLockState.ACQUIRED
      || state == CanvasEditLockState.RENEWED; }

  public boolean isUnavailable() { return state == CanvasEditLockState.UNAVAILABLE; }

}
