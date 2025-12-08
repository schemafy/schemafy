package com.schemafy.core.collaboration.dto;

public record CursorPosition(
        String userName,
        double x,
        double y) {

    public CursorPosition withUserName(String userName) {
        return new CursorPosition(userName, x, y);
    }
}
