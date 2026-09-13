package com.schemafy.core.project.domain;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

public final class ProjectPolicy {

  public static final int MAX_NAME_LENGTH = 255;
  public static final int MAX_DESCRIPTION_LENGTH = 1000;

  private ProjectPolicy() {}

  public static void validateText(String name, String description) {
    validateName(name);
    validateDescription(description);
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new DomainException(ProjectErrorCode.SETTINGS_INVALID,
          "Project name is required");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new DomainException(ProjectErrorCode.SETTINGS_TOO_LARGE,
          "Project name must not exceed " + MAX_NAME_LENGTH + " characters");
    }
  }

  private static void validateDescription(String description) {
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
      throw new DomainException(ProjectErrorCode.SETTINGS_TOO_LARGE,
          "Project description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
    }
  }

}
