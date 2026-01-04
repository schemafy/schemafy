package com.schemafy.core.erd.controller.dto.response;

import java.util.List;

public record AffectedColumnsResponse(List<ColumnResponse> columns) {
}
