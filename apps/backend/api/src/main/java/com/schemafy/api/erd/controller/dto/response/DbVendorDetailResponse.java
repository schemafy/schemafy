package com.schemafy.api.erd.controller.dto.response;

import com.schemafy.core.erd.vendor.domain.VendorCapabilities;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

public record DbVendorDetailResponse(
    Integer id,
    String displayName,
    String name,
    String version,
    DatatypePolicy datatypeMappings,
    VendorCapabilities capabilities) {
}
