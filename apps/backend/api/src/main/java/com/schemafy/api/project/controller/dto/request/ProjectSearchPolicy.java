package com.schemafy.api.project.controller.dto.request;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.core.common.exception.DomainException;

public final class ProjectSearchPolicy {

  public static final int MAX_SEARCH_LENGTH = 100;
  private static final String INVALID_SEARCH_MESSAGE = "search must contain 1 to 100 characters without control characters";

  private ProjectSearchPolicy() {}

  public static String normalize(String search) {
    String normalizedSearch = search == null ? null : search.strip();
    if (search == null
        || search.codePoints().anyMatch(Character::isISOControl)
        || normalizedSearch.isEmpty()
        || normalizedSearch.codePointCount(0, normalizedSearch.length()) > MAX_SEARCH_LENGTH) {
      throw new DomainException(CommonErrorCode.INVALID_PARAMETER, INVALID_SEARCH_MESSAGE);
    }
    return normalizedSearch;
  }

}
