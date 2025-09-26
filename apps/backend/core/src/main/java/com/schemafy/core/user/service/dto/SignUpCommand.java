package com.schemafy.core.user.service.dto;

import com.schemafy.core.user.repository.vo.UserInfo;

public record SignUpCommand(String email, String name, String password) {
    public UserInfo toUserInfo() {
        return new UserInfo(email, name, password);
    }
}
