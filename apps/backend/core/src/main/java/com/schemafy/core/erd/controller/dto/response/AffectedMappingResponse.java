package com.schemafy.core.erd.controller.dto.response;

import java.util.Map;
import java.util.Collections;

import validation.Validation.Schema;

/**
 * Mapping result between frontend IDs (FE-ID) and backend IDs (BE-ID) with one-level hierarchical grouping.
 *
 * <p>General rules
 * <ul>
 *   <li>Flat maps (schemas, tables): {@code FE-ID -> BE-ID}</li>
 *   <li>Nested maps (others): {@code Parent-BE-ID -> ( FE-ID -> BE-ID )}</li>
 *   <li>"Parent-BE-ID" is the immediate upper-level entity ID for the group (e.g., tableId for columns).</li>
 * </ul>
 *
 * <p>Grouping basis
 * <ul>
 *   <li>columns: grouped by tableId</li>
 *   <li>indexes: grouped by tableId</li>
 *   <li>indexColumns: grouped by indexId</li>
 *   <li>constraints: grouped by tableId</li>
 *   <li>constraintColumns: grouped by constraintId</li>
 *   <li>relationships: grouped by tableId (owner table)</li>
 *   <li>relationshipColumns: grouped by relationshipId</li>
 * </ul>
 *
 * <p>Example
 * <pre>
 * {
 *   schemas:  { fe-schema-1: be-schema-1 },
 *   tables:   { fe-table-1:  be-table-1  },
 *   columns:  { be-table-1:  { fe-col-1:  be-col-1 } },
 *   indexes:  { be-table-1:  { fe-idx-1:  be-idx-1 } },
 *   indexColumns: { be-idx-1: { fe-idxcol-1: be-idxcol-1 } },
 *   constraints: { be-table-1: { fe-const-1: be-const-1 } },
 *   constraintColumns: { be-const-1: { fe-cc-1: be-cc-1 } },
 *   relationships: { be-table-1: { fe-rel-1: be-rel-1 } },
 *   relationshipColumns: { be-rel-1: { fe-rc-1: be-rc-1 } }
 * }
 * </pre>
 */
public record AffectedMappingResponse(
    Map<String, String> schemas,
    Map<String, String> tables,
    Map<String, Map<String, String>> columns,
    Map<String, Map<String, String>> indexes,
    Map<String, Map<String, String>> indexColumns,
    Map<String, Map<String, String>> constraints,
    Map<String, Map<String, String>> constraintColumns,
    Map<String, Map<String, String>> relationships,
    Map<String, Map<String, String>> relationshipColumns
) {
    public static AffectedMappingResponse of(
            Schema protoSchema,
            Schema savedSchema) {
        return new AffectedMappingResponse(
                Collections.singletonMap(protoSchema.getId(), savedSchema.getId()),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }
}
