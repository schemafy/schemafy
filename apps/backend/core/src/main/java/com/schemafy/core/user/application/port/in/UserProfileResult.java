package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.User;

public record UserProfileResult(String id, String email, String name) {

  public static final String WITHDRAWN_USER_LABEL = "탈퇴한 사용자";

  public static UserProfileResult forProfileLookup(User user, boolean detailsVisible) {
    if (detailsVisible) {
      return new UserProfileResult(user.id(), user.email(), user.name());
    }
    return new UserProfileResult(user.id(), "", WITHDRAWN_USER_LABEL);
  }

}
