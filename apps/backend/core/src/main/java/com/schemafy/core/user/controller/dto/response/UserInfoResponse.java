package com.schemafy.core.user.controller.dto.response;

import com.schemafy.core.user.repository.entity.User;

public record UserInfoResponse(String id, String email, String name) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(user.getId(), user.getEmail(),
                user.getName());
    }

}
