package com.schemafy.api.user.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.user.application.port.in.VerifySignUpEmailResult;

public record VerifySignUpEmailResponse(
    String email,
    String signupVerificationToken,
    Instant expiresAt) {

  public static VerifySignUpEmailResponse from(VerifySignUpEmailResult result) {
    return new VerifySignUpEmailResponse(
        result.email(),
        result.signupVerificationToken(),
        result.expiresAt());
  }

}
