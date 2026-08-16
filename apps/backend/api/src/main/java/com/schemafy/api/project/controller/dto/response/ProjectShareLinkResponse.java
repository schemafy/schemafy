package com.schemafy.api.project.controller.dto.response;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.core.project.domain.ShareLink;

public record ProjectShareLinkResponse(String id, String url, Boolean isActive) {

  public static ProjectShareLinkResponse inactive() {
    return new ProjectShareLinkResponse(null, null, false);
  }

  public static ProjectShareLinkResponse of(
      ShareLink shareLink, String baseUrl, String version) {
    String normalizedBaseUrl = baseUrl.endsWith("/")
        ? baseUrl.substring(0, baseUrl.length() - 1)
        : baseUrl;
    String publicApiPath = ApiPath.PUBLIC_API.replace("{version}", version);
    return new ProjectShareLinkResponse(
        shareLink.getId(),
        normalizedBaseUrl + publicApiPath + "/share/" + shareLink.getId(),
        shareLink.getIsActive());
  }

}
