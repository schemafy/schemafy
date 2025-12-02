package com.schemafy.core.collaboration.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketAuthInfo {

    private final String userId;
    private final String userName;

    public static WebSocketAuthInfo of(String userId, String userName) {
        return new WebSocketAuthInfo(userId, userName);
    }

}
