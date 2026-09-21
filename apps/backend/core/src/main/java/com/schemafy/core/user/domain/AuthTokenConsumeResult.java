package com.schemafy.core.user.domain;

public enum AuthTokenConsumeResult {
  CONSUMED,
  MISSING,
  MISMATCH,
  ATTEMPTS_EXCEEDED
}
