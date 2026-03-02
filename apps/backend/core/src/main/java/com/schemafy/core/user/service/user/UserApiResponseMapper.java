package com.schemafy.core.user.service.user;

import org.springframework.stereotype.Component;

import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.domain.user.domain.User;

@Component
public class UserApiResponseMapper {

  public UserInfoResponse toUserInfoResponse(User user) {
    return new UserInfoResponse(user.id(), user.email(), user.name());
  }

}

