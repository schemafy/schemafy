package com.schemafy.core.erd.service;

import java.util.Map;

public record IdMappings(
    Map<String, String> schemas,
    Map<String, String> tables,
    Map<String, String> columns,
    Map<String, String> indexes,
    Map<String, String> indexColumns,
    Map<String, String> constraints,
    Map<String, String> constraintColumns,
    Map<String, String> relationships,
    Map<String, String> relationshipColumns) {
}
