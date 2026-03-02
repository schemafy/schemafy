package com.schemafy.core.user.repository.entity;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.repository.vo.AuthProvider;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transitional legacy entity for read compatibility in core module.
 * <p>
 * User auth-provider ownership is moving to domain user adapters.
 * Keep this entity for compatibility only until project/workspace tracks are migrated.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("user_auth_providers")
@Deprecated(forRemoval = false)
public class UserAuthProvider extends BaseEntity {

  private String userId;

  private String provider;

  private String providerUserId;

  public static UserAuthProvider create(String userId,
      AuthProvider provider, String providerUserId) {
    UserAuthProvider entity = new UserAuthProvider(
        userId,
        provider.name(),
        providerUserId);
    entity.setId(UlidGenerator.generate());
    return entity;
  }

}
