package com.schemafy.core.user.domain;

import java.time.Duration;

public final class AuthPolicy {

  public static final Duration EMAIL_VERIFICATION_TTL = Duration.ofMinutes(1);
  public static final int EMAIL_VERIFICATION_MAX_ATTEMPTS = 3;

  public static final Duration SIGNUP_VERIFICATION_TTL = Duration.ofMinutes(10);
  public static final int SIGNUP_VERIFICATION_MAX_ATTEMPTS = 1;

  private AuthPolicy() {}

}
