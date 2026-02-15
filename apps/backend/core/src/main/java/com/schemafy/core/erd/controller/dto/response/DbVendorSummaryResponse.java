package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

public record DbVendorSummaryResponse(
    String displayName,
    String name,
    String version) {

  public static DbVendorSummaryResponse from(DbVendorSummary summary) {
    return new DbVendorSummaryResponse(
        summary.displayName(),
        summary.name(),
        summary.version());
  }

}
