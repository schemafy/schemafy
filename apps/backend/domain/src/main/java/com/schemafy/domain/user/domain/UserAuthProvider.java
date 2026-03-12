package com.schemafy.domain.user.domain;

import java.time.Instant;

public record UserAuthProvider(String id, String userId, AuthProvider provider, String providerUserId,
    Instant createdAt, Instant updatedAt, Instant deletedAt) {

  public static UserAuthProvider create(
      String id,
      String userId,
      AuthProvider provider,
      String providerUserId) {
    return new UserAuthProvider(
        id,
        userId,
        provider,
        providerUserId,
        null,
        null,
        null);
  }

}
