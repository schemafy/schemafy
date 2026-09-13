package com.schemafy.core.project.domain;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

public final class WorkspacePolicy {

  public static final int MAX_NAME_LENGTH = 255;
  public static final int MAX_DESCRIPTION_LENGTH = 1000;

  private WorkspacePolicy() {}

  public static void validateText(String name, String description) {
    validateName(name);
    validateDescription(description);
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new DomainException(WorkspaceErrorCode.SETTINGS_INVALID,
          "Workspace name is required");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new DomainException(WorkspaceErrorCode.SETTINGS_TOO_LARGE,
          "Workspace name must not exceed " + MAX_NAME_LENGTH + " characters");
    }
  }

  private static void validateDescription(String description) {
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
      throw new DomainException(WorkspaceErrorCode.SETTINGS_TOO_LARGE,
          "Workspace description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
    }
  }

}
