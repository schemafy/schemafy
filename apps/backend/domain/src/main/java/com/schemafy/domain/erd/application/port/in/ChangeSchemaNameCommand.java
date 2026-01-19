package com.schemafy.domain.erd.application.port.in;

public record ChangeSchemaNameCommand(
    String projectId,
    String schemaId,
    String newName) {

}
