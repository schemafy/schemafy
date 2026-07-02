package com.schemafy.api.user.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.user.application.port.in.SignUpUserResult;

public record SignUpChallengeResponse(String email, Instant expiresAt) {

  public static SignUpChallengeResponse from(SignUpUserResult result) {
    return new SignUpChallengeResponse(result.email(), result.expiresAt());
  }

}
