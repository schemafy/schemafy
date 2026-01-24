package com.schemafy.domain.erd.schema.application.port.in;

public record ChangeSchemaNameCommand(
    String projectId,
    String schemaId,
    String newName) {

}
