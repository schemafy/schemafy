package com.schemafy.api.user.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.user.application.port.in.SignUpEmailVerificationResult;

public record SignUpEmailVerificationResponse(String email, Instant expiresAt) {

  public static SignUpEmailVerificationResponse from(SignUpEmailVerificationResult result) {
    return new SignUpEmailVerificationResponse(result.email(), result.expiresAt());
  }

}
