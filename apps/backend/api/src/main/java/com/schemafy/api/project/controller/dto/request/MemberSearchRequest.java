package com.schemafy.api.project.controller.dto.request;

import org.springframework.util.StringUtils;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.core.common.exception.DomainException;

public record MemberSearchRequest(
    MemberSearchCategory category,
    String workspaceId,
    String projectId,
    String search,
    int page,
    int size) {

  public MemberSearchRequest {
    switch (category) {
    case WORKSPACE -> {
      if (!StringUtils.hasText(workspaceId)) {
        throw new DomainException(CommonErrorCode.INVALID_PARAMETER, "workspaceId is required");
      }
      if (projectId != null) {
        throw new DomainException(
            CommonErrorCode.INVALID_PARAMETER, "projectId is not allowed for WORKSPACE");
      }
    }
    case PROJECT -> {
      if (!StringUtils.hasText(projectId)) {
        throw new DomainException(CommonErrorCode.INVALID_PARAMETER, "projectId is required");
      }
      if (workspaceId != null) {
        throw new DomainException(
            CommonErrorCode.INVALID_PARAMETER, "workspaceId is not allowed for PROJECT");
      }
    }
    }
    search = ProjectSearchPolicy.normalize(search);
  }

}
