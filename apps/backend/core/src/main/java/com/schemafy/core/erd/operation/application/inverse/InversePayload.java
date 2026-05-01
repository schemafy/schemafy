package com.schemafy.core.erd.operation.application.inverse;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChangeTableNameInverse.class, name = "CHANGE_TABLE_NAME"),
    @JsonSubTypes.Type(value = ChangeColumnNameInverse.class, name = "CHANGE_COLUMN_NAME"),
    @JsonSubTypes.Type(value = ChangeColumnTypeInverse.class, name = "CHANGE_COLUMN_TYPE"),
    @JsonSubTypes.Type(value = ChangeRelationshipNameInverse.class, name = "CHANGE_RELATIONSHIP_NAME"),
    @JsonSubTypes.Type(value = ChangeRelationshipCardinalityInverse.class, name = "CHANGE_RELATIONSHIP_CARDINALITY"),
    @JsonSubTypes.Type(value = ChangeConstraintNameInverse.class, name = "CHANGE_CONSTRAINT_NAME"),
    @JsonSubTypes.Type(value = ChangeIndexNameInverse.class, name = "CHANGE_INDEX_NAME"),
    @JsonSubTypes.Type(value = ChangeIndexTypeInverse.class, name = "CHANGE_INDEX_TYPE")
})
public sealed interface InversePayload permits
    ChangeTableNameInverse,
    ChangeColumnNameInverse,
    ChangeColumnTypeInverse,
    ChangeRelationshipNameInverse,
    ChangeRelationshipCardinalityInverse,
    ChangeConstraintNameInverse,
    ChangeIndexNameInverse,
    ChangeIndexTypeInverse {

}
