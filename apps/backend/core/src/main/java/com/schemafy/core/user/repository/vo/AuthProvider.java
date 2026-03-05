package com.schemafy.core.user.repository.vo;

/** Transitional enum for legacy core user entity/repository compatibility.
 * <p>
 * Do not introduce new usages; migrate callers to domain user AuthProvider. */
@Deprecated(forRemoval = false)
public enum AuthProvider {

  GITHUB

}
