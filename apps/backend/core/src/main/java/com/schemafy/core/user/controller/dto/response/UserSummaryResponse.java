package com.schemafy.core.user.controller.dto.response;

import com.schemafy.core.user.repository.entity.User;

public record UserSummaryResponse(String id, String name) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getName());
    }

}
