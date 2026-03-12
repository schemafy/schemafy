package com.schemafy.api.user.controller.dto.response;

import com.schemafy.core.user.domain.User;

public record UserInfoResponse(String id, String email, String name) {

  public static UserInfoResponse from(User user) {
    return new UserInfoResponse(user.id(), user.email(), user.name());
  }

}
