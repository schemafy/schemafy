package com.schemafy.core.collaboration.dto;

public record CursorPosition(
    String userId,
    String userName,
    double x,
    double y) {

  public CursorPosition withUserName(String userName) {
    return new CursorPosition(userId, userName, x, y);
  }

  public CursorPosition withUserInfo(String userId, String userName) {
    return new CursorPosition(userId, userName, x, y);
  }

}
