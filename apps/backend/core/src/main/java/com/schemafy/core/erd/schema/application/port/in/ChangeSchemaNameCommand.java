package com.schemafy.core.erd.schema.application.port.in;

public record ChangeSchemaNameCommand(
    String schemaId,
    String newName) {

}
