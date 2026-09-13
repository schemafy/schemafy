package com.schemafy.api.project.controller.dto.request;

import org.springframework.util.StringUtils;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.core.common.exception.DomainException;

public record ProjectSearchRequest(
    ProjectSearchCategory category,
    String workspaceId,
    String search,
    int page,
    int size) {

  public ProjectSearchRequest {
    switch (category) {
    case WORKSPACE -> {
      if (!StringUtils.hasText(workspaceId)) {
        throw new DomainException(CommonErrorCode.INVALID_PARAMETER, "workspaceId is required");
      }
    }
    case SHARED -> {
      if (workspaceId != null) {
        throw new DomainException(
            CommonErrorCode.INVALID_PARAMETER, "workspaceId is not allowed for SHARED");
      }
    }
    }
    search = ProjectSearchPolicy.normalize(search);
  }

}
